package com.unisono.card.data.infrastructure.adapter.output.persistence;

import com.unisono.card.data.domain.service.CardExpirationService;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.OutboxEventEntity;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vencimiento contra un PostgreSQL real (Testcontainers): la consulta por fecha y estado, FOR UPDATE SKIP LOCKED
 * y el card.expired en el outbox en la misma transacción. El esquema lo genera Hibernate a partir de las
 * entidades (ver CardDataPostgresTest).
 */
class CardExpirationPersistenceTest extends CardDataPostgresTest {

    private static final YearMonth DECEMBER_2026 = YearMonth.of(2026, 12);

    @Autowired
    private CardExpirationService service;

    @Test
    void locksOnlyCardsThatExpiredBeforeTheCurrentMonthAndCanExpire() {
        String active = save(CardStatus.ACTIVE, 11, 2026);
        String issued = save(CardStatus.ISSUED, 1, 2025);
        String blocked = save(CardStatus.BLOCKED, 6, 2026);
        String fraud = save(CardStatus.BLOCKED_FRAUD, 12, 2025);
        save(CardStatus.ACTIVE, 12, 2026);             // vence a fin de mes: todavía válida
        save(CardStatus.ACTIVE, 1, 2027);
        save(CardStatus.CANCELLED, 1, 2025);           // estados finales o sin emitir: no vencen
        save(CardStatus.EXPIRED, 1, 2025);
        save(CardStatus.APPROVED, null, null);
        String deleted = save(CardStatus.ACTIVE, 1, 2025);
        cards.findById(deleted).ifPresent(e -> { e.markAsDeleted(); cards.save(e); });

        List<String> locked = tx.execute(s -> adapter.lockExpiredBatch(DECEMBER_2026, 100).stream()
                .map(card -> card.getId().getValue()).toList());

        assertThat(locked).containsExactlyInAnyOrder(active, issued, blocked, fraud);
    }

    @Test
    void expiresTheCardsAndEnqueuesCardExpiredInTheSameTransaction() {
        String active = save(CardStatus.ACTIVE, 11, 2026);
        String blocked = save(CardStatus.BLOCKED, 10, 2026);
        String valid = save(CardStatus.ACTIVE, 12, 2026);

        List<CardId> expired = service.expireBatch(DECEMBER_2026, 100);

        assertThat(expired).extracting(CardId::getValue).containsExactlyInAnyOrder(active, blocked);
        assertThat(status(active)).isEqualTo(CardStatus.EXPIRED);
        assertThat(status(blocked)).isEqualTo(CardStatus.EXPIRED);
        assertThat(status(valid)).isEqualTo(CardStatus.ACTIVE);
        assertThat(cards.findById(active).orElseThrow().getExpiredAt()).isNotNull();

        List<OutboxEventEntity> events = outbox.findAll();
        assertThat(events).extracting(OutboxEventEntity::getTopic).containsOnly(KafkaTopics.CARD_EXPIRED);
        assertThat(events).extracting(OutboxEventEntity::getAggregateId).containsExactlyInAnyOrder(active, blocked);

        // Una segunda pasada no encuentra nada
        assertThat(service.expireBatch(DECEMBER_2026, 100)).isEmpty();
    }

    @Test
    void cardsLockedByAnotherTransactionAreSkipped() throws Exception {
        String first = save(CardStatus.ACTIVE, 1, 2026);
        String second = save(CardStatus.ACTIVE, 2, 2026);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        // Otra instancia del job toma una tarjeta y mantiene la transacción abierta
        CompletableFuture<List<String>> other = CompletableFuture.supplyAsync(() -> tx.execute(s -> {
            List<String> ids = adapter.lockExpiredBatch(DECEMBER_2026, 1).stream()
                    .map(card -> card.getId().getValue()).toList();
            locked.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return ids;
        }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        List<String> mine = tx.execute(s -> adapter.lockExpiredBatch(DECEMBER_2026, 100).stream()
                .map(card -> card.getId().getValue()).toList());
        release.countDown();

        assertThat(other.get(10, TimeUnit.SECONDS)).hasSize(1);
        assertThat(mine).hasSize(1).doesNotContainAnyElementsOf(other.get());
        assertThat(List.of(mine.get(0), other.get().get(0))).containsExactlyInAnyOrder(first, second);
    }
}
