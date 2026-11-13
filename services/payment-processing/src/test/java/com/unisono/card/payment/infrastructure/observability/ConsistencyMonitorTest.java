package com.unisono.card.payment.infrastructure.observability;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentCreditOperationEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentCreditOperationRepository;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentOutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsistencyMonitorTest {

    @Mock
    private JpaPaymentCreditOperationRepository creditOperations;
    @Mock
    private JpaPaymentOutboxEventRepository outbox;
    @Mock
    private PlatformTransactionManager transactionManager;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private ConsistencyMonitor monitor;
    private ConsistencyHealthIndicator health;

    @BeforeEach
    void setUp() {
        monitor = new ConsistencyMonitor(creditOperations, outbox, registry, transactionManager);
        health = new ConsistencyHealthIndicator(monitor);
    }

    @Test
    void shouldPublishTheGaugesAfterARefresh() {
        counts(3, 1, 7, 2, 4);

        monitor.refresh();

        assertThat(gauge("payment.credit_saga.pending")).isEqualTo(3);
        assertThat(gauge("payment.credit_saga.stuck")).isEqualTo(1);
        assertThat(gauge("payment.outbox.pending")).isEqualTo(7);
        assertThat(gauge("payment.outbox.stalled")).isEqualTo(2);
        assertThat(gauge("payment.outbox.failed")).isEqualTo(4);
    }

    @Test
    void shouldReportUpWhenNothingIsStuck() {
        counts(2, 0, 5, 0, 0);
        monitor.refresh();

        Health result = health.health();

        assertThat(result.getStatus()).isEqualTo(Status.UP);
        assertThat(result.getDetails()).containsEntry("creditOperationsPending", 2L).containsEntry("outboxPending", 5L);
    }

    @Test
    void shouldReportDegradedWhenCreditOperationsAreStuck() {
        counts(2, 1, 0, 0, 0);
        monitor.refresh();

        Health result = health.health();

        assertThat(result.getStatus()).isEqualTo(ConsistencyHealthIndicator.DEGRADED);
        assertThat(result.getDetails()).containsEntry("creditOperationsStuck", 1L);
    }

    @Test
    void shouldReportDegradedWhenOutboxEventsAreStalledOrFailed() {
        counts(0, 0, 3, 3, 0);
        monitor.refresh();
        assertThat(health.health().getStatus()).isEqualTo(ConsistencyHealthIndicator.DEGRADED);

        counts(0, 0, 0, 0, 1);
        monitor.refresh();
        assertThat(health.health().getStatus()).isEqualTo(ConsistencyHealthIndicator.DEGRADED);
    }

    @Test
    void shouldKeepThePreviousValuesWhenTheDatabaseFails() {
        counts(1, 1, 0, 0, 0);
        monitor.refresh();
        when(creditOperations.countByStatus(PaymentCreditOperationEntity.Status.PENDING))
                .thenThrow(new IllegalStateException("db down"));

        monitor.refresh();

        assertThat(gauge("payment.credit_saga.stuck")).isEqualTo(1);
    }

    @Test
    void shouldCountCompensationsBySourceAndResult() {
        monitor.compensationSucceeded(ConsistencyMonitor.SOURCE_IMMEDIATE);
        monitor.compensationSucceeded(ConsistencyMonitor.SOURCE_RECONCILER);
        monitor.compensationFailed(ConsistencyMonitor.SOURCE_RECONCILER, "op-1", 1);
        monitor.compensationFailed(ConsistencyMonitor.SOURCE_RECONCILER, "op-1", 2);

        assertThat(counter("payment.credit_saga.compensations", "result", "compensated", "source", "immediate")).isEqualTo(1);
        assertThat(counter("payment.credit_saga.compensations", "result", "failed", "source", "reconciler")).isEqualTo(2);
    }

    @Test
    void shouldCountOutboxFailuresByKind() {
        monitor.outboxPublishFailed(true);
        monitor.outboxPublishFailed(true);
        monitor.outboxPublishFailed(false);

        assertThat(registry.get("payment.outbox.publish_failures").tag("kind", "transient").counter().count()).isEqualTo(2);
        assertThat(registry.get("payment.outbox.publish_failures").tag("kind", "permanent").counter().count()).isEqualTo(1);
    }

    private void counts(long pending, long stuck, long outboxPending, long outboxStalled, long outboxFailed) {
        org.mockito.Mockito.lenient().when(creditOperations.countByStatus(PaymentCreditOperationEntity.Status.PENDING)).thenReturn(pending);
        org.mockito.Mockito.lenient().when(creditOperations.countStuck(anyInt(), any())).thenReturn(stuck);
        org.mockito.Mockito.lenient().when(outbox.countByPublishedAtIsNullAndFailedAtIsNull()).thenReturn(outboxPending);
        org.mockito.Mockito.lenient().when(outbox.countByPublishedAtIsNullAndFailedAtIsNullAndCreatedAtBefore(any())).thenReturn(outboxStalled);
        org.mockito.Mockito.lenient().when(outbox.countByFailedAtIsNotNull()).thenReturn(outboxFailed);
    }

    private double gauge(String name) {
        return registry.get(name).gauge().value();
    }

    private double counter(String name, String k1, String v1, String k2, String v2) {
        return registry.get(name).tag(k1, v1).tag(k2, v2).counter().count();
    }
}
