package com.unisono.card.payment.infrastructure.adapter.output.persistence;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentCreditOperationEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentCreditOperationEntity.Status;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentOutboxEventEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentCreditOperationRepository;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentOutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consultas nativas del outbox y de la saga de crédito contra PostgreSQL: orden, filtros y FOR UPDATE SKIP LOCKED
 */
class OutboxAndCreditOperationQueriesTest extends PostgresPersistenceTest {

    @Autowired
    JpaPaymentOutboxEventRepository outbox;

    @Autowired
    JpaPaymentCreditOperationRepository operations;

    // ==================== Outbox ====================

    @Test
    void shouldLockPendingEventsInInsertionOrderSkippingPublishedAndFailed() {
        PaymentOutboxEventEntity first = event();
        PaymentOutboxEventEntity published = event();
        published.setPublishedAt(LocalDateTime.now());
        PaymentOutboxEventEntity failed = event();
        failed.setFailedAt(LocalDateTime.now());
        PaymentOutboxEventEntity second = event();
        tx.executeWithoutResult(s -> outbox.saveAll(List.of(first, published, failed, second)));

        List<String> batch = tx.execute(s -> outbox.lockNextPendingBatch(10).stream()
                .map(PaymentOutboxEventEntity::getEventId).toList());

        assertThat(batch).containsExactly(first.getEventId(), second.getEventId());
    }

    @Test
    void shouldGiveConcurrentRelaysDisjointBatches() throws Exception {
        tx.executeWithoutResult(s -> outbox.saveAll(List.of(event(), event(), event(), event())));
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        CompletableFuture<List<String>> first = CompletableFuture.supplyAsync(() -> tx.execute(s -> {
            List<String> ids = ids(outbox.lockNextPendingBatch(2));
            locked.countDown();
            await(release);
            return ids;
        }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        // El segundo relay no espera al primero: se salta las filas tomadas y lee las siguientes
        List<String> second = tx.execute(s -> ids(outbox.lockNextPendingBatch(10)));
        release.countDown();

        assertThat(first.get(10, TimeUnit.SECONDS)).hasSize(2).doesNotContainAnyElementsOf(second);
        assertThat(second).hasSize(2);
    }

    @Test
    void shouldDeleteOnlyEventsPublishedBeforeTheDateAndCountFailures() {
        PaymentOutboxEventEntity old = event();
        old.setPublishedAt(LocalDateTime.now().minusDays(10));
        PaymentOutboxEventEntity recent = event();
        recent.setPublishedAt(LocalDateTime.now());
        PaymentOutboxEventEntity failed = event();
        failed.setFailedAt(LocalDateTime.now().minusDays(10));
        tx.executeWithoutResult(s -> outbox.saveAll(List.of(old, recent, failed, event())));

        Integer deleted = tx.execute(s -> outbox.deletePublishedBefore(LocalDateTime.now().minusDays(7)));

        Long remaining = tx.execute(s -> outbox.count());
        Long failedCount = tx.execute(s -> outbox.countByFailedAtIsNotNull());

        assertThat(deleted).isEqualTo(1);
        assertThat(remaining).isEqualTo(3);
        assertThat(failedCount).isEqualTo(1);
    }

    // ==================== Saga de crédito ====================

    @Test
    void shouldLockOnlyStalePendingOperations() {
        PaymentCreditOperationEntity stale = operation(Status.PENDING, 5);
        PaymentCreditOperationEntity fresh = operation(Status.PENDING, 0);
        PaymentCreditOperationEntity confirmed = operation(Status.CONFIRMED, 5);
        tx.executeWithoutResult(s -> operations.saveAll(List.of(stale, fresh, confirmed)));

        List<String> pending = tx.execute(s -> operations.lockStalePending(LocalDateTime.now().minusMinutes(1), 20)
                .stream().map(PaymentCreditOperationEntity::getOperationId).toList());

        assertThat(pending).containsExactly(stale.getOperationId());
    }

    @Test
    void shouldSkipOperationsHeldByAPaymentInProgress() throws Exception {
        PaymentCreditOperationEntity inProgress = operation(Status.PENDING, 5);
        PaymentCreditOperationEntity abandoned = operation(Status.PENDING, 5);
        tx.executeWithoutResult(s -> operations.saveAll(List.of(inProgress, abandoned)));
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        // La transacción del pago tiene tomada su operación mientras llama a card-limits
        CompletableFuture<Void> payment = CompletableFuture.runAsync(() -> tx.executeWithoutResult(s -> {
            operations.lockById(inProgress.getOperationId()).orElseThrow();
            locked.countDown();
            await(release);
        }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        List<String> reconciled = tx.execute(s -> operations.lockStalePending(LocalDateTime.now().minusMinutes(1), 20)
                .stream().map(PaymentCreditOperationEntity::getOperationId).toList());
        release.countDown();
        payment.get(10, TimeUnit.SECONDS);

        assertThat(reconciled).containsExactly(abandoned.getOperationId());
    }

    @Test
    void shouldResolveAnOperationOnlyWhileItIsPending() {
        PaymentCreditOperationEntity pending = operation(Status.PENDING, 0);
        PaymentCreditOperationEntity compensated = operation(Status.COMPENSATED, 0);
        tx.executeWithoutResult(s -> operations.saveAll(List.of(pending, compensated)));

        Integer resolved = tx.execute(s -> operations.resolvePending(pending.getOperationId(), Status.CONFIRMED, LocalDateTime.now()));
        Integer notPending = tx.execute(s -> operations.resolvePending(compensated.getOperationId(), Status.CONFIRMED, LocalDateTime.now()));

        assertThat(resolved).isEqualTo(1);
        assertThat(notPending).isZero();
        Status status = tx.execute(s -> operations.findById(pending.getOperationId()).orElseThrow().getStatus());
        Long stillCompensated = tx.execute(s -> operations.countByStatus(Status.COMPENSATED));
        assertThat(status).isEqualTo(Status.CONFIRMED);
        assertThat(stillCompensated).isEqualTo(1);
    }

    @Test
    void shouldCountStuckOperationsByAttemptsOrAge() {
        PaymentCreditOperationEntity manyAttempts = operation(Status.PENDING, 1);
        manyAttempts.setAttempts(5);
        PaymentCreditOperationEntity old = operation(Status.PENDING, 30);
        PaymentCreditOperationEntity recent = operation(Status.PENDING, 1);
        PaymentCreditOperationEntity oldButCompensated = operation(Status.COMPENSATED, 30);
        oldButCompensated.setAttempts(9);
        tx.executeWithoutResult(s -> operations.saveAll(List.of(manyAttempts, old, recent, oldButCompensated)));

        Long stuck = tx.execute(s -> operations.countStuck(5, LocalDateTime.now().minusMinutes(15)));

        assertThat(stuck).isEqualTo(2);
    }

    @Test
    void shouldCountPendingAndStalledEvents() {
        PaymentOutboxEventEntity stalled = event();
        stalled.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        PaymentOutboxEventEntity published = event();
        published.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        published.setPublishedAt(LocalDateTime.now());
        tx.executeWithoutResult(s -> outbox.saveAll(List.of(stalled, published, event())));

        Long pending = tx.execute(s -> outbox.countByPublishedAtIsNullAndFailedAtIsNull());
        Long stalledCount = tx.execute(s -> outbox.countByPublishedAtIsNullAndFailedAtIsNullAndCreatedAtBefore(
                LocalDateTime.now().minusMinutes(5)));

        assertThat(pending).isEqualTo(2);
        assertThat(stalledCount).isEqualTo(1);
    }

    private static PaymentOutboxEventEntity event() {
        String id = UUID.randomUUID().toString();
        return PaymentOutboxEventEntity.builder()
                .eventId(id)
                .aggregateId("550e8400-e29b-41d4-a716-446655440000")
                .topic("payment.processing.completed")
                .eventType("PaymentProcessed")
                .payload("{\"eventId\":\"" + id + "\"}")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static PaymentCreditOperationEntity operation(Status status, int minutesAgo) {
        LocalDateTime created = LocalDateTime.now().minusMinutes(minutesAgo);
        return PaymentCreditOperationEntity.builder()
                .operationId(UUID.randomUUID().toString())
                .transactionId(UUID.randomUUID())
                .cardId("550e8400-e29b-41d4-a716-446655440000")
                .operationType(PaymentCreditOperationEntity.Type.CONSUME_PURCHASE)
                .amount(new BigDecimal("1500.00"))
                .currency("ARS")
                .status(status)
                .createdAt(created)
                .updatedAt(created)
                .build();
    }

    private static List<String> ids(List<PaymentOutboxEventEntity> events) {
        return events.stream().map(PaymentOutboxEventEntity::getEventId).toList();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
