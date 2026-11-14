package com.unisono.card.data.infrastructure.adapter.output.persistence;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.BlockCardForFraudDeclineUseCase.FraudDeclineCommand;
import com.unisono.card.data.domain.port.input.BlockCardForFraudDeclineUseCase.Outcome;
import com.unisono.card.data.domain.service.FraudDeclineService;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.OutboxEventEntity;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bloqueo por fraude contra PostgreSQL: el eventId, el bloqueo y el card.blocked se guardan juntos, y un evento
 * repetido no vuelve a bloquear una tarjeta que se desbloqueó.
 */
class FraudDeclinePersistenceTest extends CardDataPostgresTest {

    @Autowired
    private FraudDeclineService service;

    @Test
    void fraudDeclineBlocksTheCardAndPublishesCardBlocked() {
        String id = save(CardStatus.ACTIVE, 12, 2030);
        String eventId = UUID.randomUUID().toString();

        Outcome outcome = service.execute(decline(eventId, id));

        assertThat(outcome).isEqualTo(Outcome.BLOCKED);
        assertThat(status(id)).isEqualTo(CardStatus.BLOCKED_FRAUD);
        assertThat(cards.findById(id).orElseThrow().getBlockReason()).contains("tx-1");
        assertThat(processedEvents.existsById(eventId)).isTrue();

        List<OutboxEventEntity> events = outbox.findAll();
        assertThat(events).extracting(OutboxEventEntity::getTopic).containsExactly(KafkaTopics.CARD_BLOCKED);
        assertThat(events.get(0).getPayload()).contains("\"blockType\":\"FRAUD\"").contains("\"blockedBy\":\"system\"")
                .contains("\"previousStatus\":\"ACTIVE\"");
    }

    @Test
    void replayedEventDoesNotBlockACardThatWasUnblockedMeanwhile() {
        String id = save(CardStatus.ACTIVE, 12, 2030);
        String eventId = UUID.randomUUID().toString();
        service.execute(decline(eventId, id));

        // Un operador revisa el caso y la desbloquea
        tx.executeWithoutResult(s -> {
            Card card = adapter.findById(CardId.of(id)).orElseThrow();
            card.unblock();
            adapter.save(card);
        });

        assertThat(service.execute(decline(eventId, id))).isEqualTo(Outcome.DUPLICATE);
        assertThat(status(id)).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void cancelledCardIsNotTouchedButTheEventIsRecorded() {
        String id = save(CardStatus.CANCELLED, 12, 2030);
        String eventId = UUID.randomUUID().toString();

        assertThat(service.execute(decline(eventId, id))).isEqualTo(Outcome.NOT_BLOCKABLE);
        assertThat(status(id)).isEqualTo(CardStatus.CANCELLED);
        assertThat(outbox.count()).isZero();
        assertThat(processedEvents.existsById(eventId)).isTrue();
    }

    private static FraudDeclineCommand decline(String eventId, String cardId) {
        return new FraudDeclineCommand(eventId, CardId.of(cardId), "tx-1", "Riesgo alto", LocalDateTime.now());
    }
}
