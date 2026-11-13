package com.unisono.card.payment.infrastructure.observability;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentCreditOperationEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentCreditOperationRepository;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentOutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Métricas y alertas de la consistencia con card-limits (saga de crédito) y con Kafka (outbox).
 * <p>
 * Gauges (se recalculan cada {@code refresh-interval-ms} con consultas a la base, no en cada scrape):
 * <ul>
 *   <li>{@code payment.credit_saga.pending}: operaciones de crédito PENDING (pago no confirmado, sin compensar).</li>
 *   <li>{@code payment.credit_saga.stuck}: PENDING con {@code stuck-after-attempts} cancelaciones fallidas o más
 *       viejas que {@code stuck-after-minutes}. El crédito de card-limits no coincide con los pagos: hay que revisarlas.</li>
 *   <li>{@code payment.outbox.pending}, {@code payment.outbox.stalled} (pendientes más viejos que
 *       {@code outbox-stalled-after-minutes}) y {@code payment.outbox.failed} (error permanente, no se reintentan).</li>
 * </ul>
 * Contadores: {@code payment.credit_saga.compensations} ({@code result} = compensated | failed,
 * {@code source} = immediate | reconciler) y {@code payment.outbox.publish_failures} ({@code kind} = transient | permanent).
 * <p>
 * Expuestas en {@code /actuator/prometheus}; las reglas de alerta de ejemplo están en
 * {@code deploy/prometheus/payment-processing-alerts.yml}. El estado también se ve en
 * {@code /actuator/health} ({@link ConsistencyHealthIndicator}).
 */
@Slf4j
@Component
public class ConsistencyMonitor {

    public static final String SOURCE_IMMEDIATE = "immediate";
    public static final String SOURCE_RECONCILER = "reconciler";

    private final JpaPaymentCreditOperationRepository creditOperations;
    private final JpaPaymentOutboxEventRepository outbox;
    private final MeterRegistry registry;
    private final TransactionTemplate readOnly;

    private final AtomicLong creditPending = new AtomicLong();
    private final AtomicLong creditStuck = new AtomicLong();
    private final AtomicLong outboxPending = new AtomicLong();
    private final AtomicLong outboxStalled = new AtomicLong();
    private final AtomicLong outboxFailed = new AtomicLong();

    @Value("${payment.credit-saga.alerts.stuck-after-attempts:5}")
    private int stuckAfterAttempts = 5;

    @Value("${payment.credit-saga.alerts.stuck-after-minutes:15}")
    private long stuckAfterMinutes = 15;

    @Value("${payment.outbox.alerts.stalled-after-minutes:5}")
    private long outboxStalledAfterMinutes = 5;

    public ConsistencyMonitor(JpaPaymentCreditOperationRepository creditOperations,
                              JpaPaymentOutboxEventRepository outbox,
                              MeterRegistry registry,
                              @Qualifier("paymentTransactionManager") PlatformTransactionManager transactionManager) {
        this.creditOperations = creditOperations;
        this.outbox = outbox;
        this.registry = registry;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);

        gauge("payment.credit_saga.pending", creditPending, "Credit operations whose payment did not commit, not compensated yet");
        gauge("payment.credit_saga.stuck", creditStuck, "Pending credit operations that cannot be compensated (need review)");
        gauge("payment.outbox.pending", outboxPending, "Payment events waiting to be published");
        gauge("payment.outbox.stalled", outboxStalled, "Payment events pending for too long (broker unreachable)");
        gauge("payment.outbox.failed", outboxFailed, "Payment events that failed permanently (need review)");
    }

    /**
     * Recalcula los gauges y avisa en el log si hay algo trabado
     */
    @Scheduled(
            initialDelayString = "${payment.observability.refresh-initial-delay-ms:15000}",
            fixedDelayString = "${payment.observability.refresh-interval-ms:30000}"
    )
    public void refresh() {
        try {
            readOnly.executeWithoutResult(status -> {
                LocalDateTime now = LocalDateTime.now();
                creditPending.set(creditOperations.countByStatus(PaymentCreditOperationEntity.Status.PENDING));
                creditStuck.set(creditOperations.countStuck(stuckAfterAttempts, now.minusMinutes(stuckAfterMinutes)));
                outboxPending.set(outbox.countByPublishedAtIsNullAndFailedAtIsNull());
                outboxStalled.set(outbox.countByPublishedAtIsNullAndFailedAtIsNullAndCreatedAtBefore(
                        now.minusMinutes(outboxStalledAfterMinutes)));
                outboxFailed.set(outbox.countByFailedAtIsNotNull());
            });
        } catch (Exception ex) {
            log.error("Could not refresh payment consistency metrics", ex);
            return;
        }

        if (creditStuck.get() > 0) {
            log.error("🚨 Credit saga: {} credit operation(s) stuck in PENDING (card-limits does not accept the "
                    + "cancellation). Available credit in card-limits does not match the payments: review "
                    + "payment_credit_operations WHERE status = 'PENDING'", creditStuck.get());
        }
        if (outboxStalled.get() > 0) {
            log.error("🚨 Payment outbox: {} event(s) pending for more than {} min (Kafka unreachable?)",
                    outboxStalled.get(), outboxStalledAfterMinutes);
        }
    }

    // ==================== Contadores ====================

    public void compensationSucceeded(String source) {
        counter("payment.credit_saga.compensations", "compensated", source).increment();
    }

    /**
     * @param attempts intentos de compensación acumulados de la operación (incluido este)
     */
    public void compensationFailed(String source, String operationId, int attempts) {
        counter("payment.credit_saga.compensations", "failed", source).increment();
        if (attempts == stuckAfterAttempts) {
            // Una sola vez por operación: a partir de acá cuenta como trabada (payment.credit_saga.stuck)
            log.error("🚨 Credit operation {} could not be compensated after {} attempts. It keeps being retried, "
                    + "but the credit in card-limits is wrong until it succeeds: needs review", operationId, attempts);
        }
    }

    /**
     * Deshacer una restauración dejó la tarjeta excedida: el reverso o la devolución no se guardaron, así que el
     * crédito se volvió a consumir aunque ya se había gastado. Es correcto (el reverso nunca ocurrió), pero la
     * tarjeta queda por encima de su límite y conviene que alguien lo vea
     */
    public void compensationLeftOverLimit(String operationId, String cardId, java.math.BigDecimal overLimitAmount) {
        Counter.builder("payment.credit_saga.over_limit")
                .description("Compensations that left the card over its credit limit")
                .register(registry)
                .increment();
        log.warn("⚠️ Compensating credit operation {} left card {} over its limit by {}: the reversal or refund was not "
                + "recorded, so its credit was consumed again after being spent", operationId, cardId, overLimitAmount);
    }

    public void outboxPublishFailed(boolean transientError) {
        Counter.builder("payment.outbox.publish_failures")
                .description("Failed attempts to publish a payment event")
                .tag("kind", transientError ? "transient" : "permanent")
                .register(registry)
                .increment();
    }

    // ==================== Estado (health) ====================

    public long creditStuck() {
        return creditStuck.get();
    }

    public long creditPending() {
        return creditPending.get();
    }

    public long outboxStalled() {
        return outboxStalled.get();
    }

    public long outboxFailed() {
        return outboxFailed.get();
    }

    public long outboxPending() {
        return outboxPending.get();
    }

    private Counter counter(String name, String result, String source) {
        return Counter.builder(name)
                .description("Compensations of credit operations whose payment did not commit")
                .tag("result", result)
                .tag("source", source)
                .register(registry);
    }

    private void gauge(String name, AtomicLong value, String description) {
        Gauge.builder(name, value, AtomicLong::get).description(description).register(registry);
    }
}
