package com.unisono.card.payment.infrastructure.adapter.output.creditlimit;

import com.unisono.card.payment.infrastructure.observability.ConsistencyMonitor;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentCreditOperationEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentCreditOperationRepository;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Compensa las operaciones de crédito que quedaron PENDING: su transacción de pago no confirmó y la
 * compensación inmediata ({@link CreditOperationCompensator}) falló o no llegó a correr (el proceso se cayó,
 * o no se supo si el commit llegó).
 * <p>
 * Solo toma operaciones más viejas que {@code stale-after-seconds} y que ninguna transacción tiene tomadas
 * (FOR UPDATE SKIP LOCKED): un pago en curso tiene la suya bloqueada. Cada operación se cancela en card-limits
 * (idempotente); si falla, se reintenta en la próxima pasada, sin límite.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payment.credit-saga.reconciler.enabled", havingValue = "true", matchIfMissing = true)
public class CreditOperationReconciler {

    private final JpaPaymentCreditOperationRepository repository;
    private final CreditLimitAdapter creditLimitAdapter;
    private final ConsistencyMonitor monitor;
    private final TransactionTemplate transactionTemplate;

    @Value("${payment.credit-saga.reconciler.stale-after-seconds:60}")
    private long staleAfterSeconds = 60;

    @Value("${payment.credit-saga.reconciler.batch-size:20}")
    private int batchSize = 20;

    public CreditOperationReconciler(JpaPaymentCreditOperationRepository repository,
                                     CreditLimitAdapter creditLimitAdapter,
                                     ConsistencyMonitor monitor,
                                     @Qualifier("paymentTransactionManager") PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.creditLimitAdapter = creditLimitAdapter;
        this.monitor = monitor;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(
            initialDelayString = "${payment.credit-saga.reconciler.initial-delay-ms:30000}",
            fixedDelayString = "${payment.credit-saga.reconciler.interval-ms:30000}"
    )
    public void reconcile() {
        try {
            Integer compensated = transactionTemplate.execute(status -> reconcileBatch());
            if (compensated != null && compensated > 0) {
                log.warn("↩️ Credit saga: {} pending credit operation(s) compensated", compensated);
            }
        } catch (Exception ex) {
            log.error("Credit saga reconciliation failed", ex);
        }
    }

    /**
     * Compensa el próximo lote dentro de la transacción actual
     *
     * @return cuántas operaciones se compensaron
     */
    int reconcileBatch() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(staleAfterSeconds);
        List<PaymentCreditOperationEntity> pending = repository.lockStalePending(before, batchSize);
        int compensated = 0;

        for (PaymentCreditOperationEntity operation : pending) {
            try {
                CreditLimitAdapter.Cancellation cancellation = creditLimitAdapter.cancelOperation(
                        operation.getOperationId(), CardId.of(operation.getCardId()));
                String outcome = cancellation.outcome();
                operation.markCompensated();
                monitor.compensationSucceeded(ConsistencyMonitor.SOURCE_RECONCILER);
                if (cancellation.leftOverLimit()) {
                    monitor.compensationLeftOverLimit(operation.getOperationId(), operation.getCardId(),
                            cancellation.overLimitAmount());
                }
                compensated++;
                log.warn("↩️ Credit operation {} ({}, {} {}) of transaction {} compensated: {}",
                        operation.getOperationId(), operation.getOperationType(), operation.getAmount(),
                        operation.getCurrency(), operation.getTransactionId(), outcome);
            } catch (Exception ex) {
                operation.markCompensationFailed(ex.getMessage());
                monitor.compensationFailed(ConsistencyMonitor.SOURCE_RECONCILER, operation.getOperationId(),
                        operation.getAttempts());
                log.error("Could not compensate credit operation {} (attempt {}), will retry: {}",
                        operation.getOperationId(), operation.getAttempts(), ex.getMessage());
            }
        }

        repository.saveAll(pending);
        return compensated;
    }
}
