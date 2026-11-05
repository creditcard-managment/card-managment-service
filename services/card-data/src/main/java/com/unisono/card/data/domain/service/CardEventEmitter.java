package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.output.EventOutboxPort;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.*;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Traduce los cambios de la tarjeta a eventos de integración y los encola en el outbox.
 *
 * Se invoca dentro de la transacción del caso de uso: el evento se confirma junto con el cambio.
 * Los eventos nunca llevan PAN, CVV ni PIN.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardEventEmitter {

    /** Autor de los cambios sin un usuario informado (por ejemplo, el bloqueo automático por PIN). */
    static final String SYSTEM_ACTOR = "system";

    private final EventOutboxPort eventOutbox;

    public void cardCreated(Card card) {
        String cardId = card.getId().getValue();
        eventOutbox.enqueue(KafkaTopics.CARD_CREATED, cardId, new CardCreatedEvent(
                cardId,
                card.getClientId().getValue(),
                card.getHolder().getFullName(),
                card.getType().name(),
                card.getCategory().name(),
                SYSTEM_ACTOR
        ));
    }

    /**
     * Encola el evento que corresponde al estado actual de la tarjeta.
     *
     * @param previousStatus estado anterior: ACTIVE significa activación o desbloqueo según de dónde venga
     * @param performedBy    si es nulo o está vacío, el autor es {@value #SYSTEM_ACTOR}
     */
    public void statusChanged(Card card, CardStatus previousStatus, String reason, String performedBy) {
        String cardId = card.getId().getValue();
        String actor = performedBy == null || performedBy.isBlank() ? SYSTEM_ACTOR : performedBy;
        LocalDateTime now = LocalDateTime.now();

        switch (card.getStatus()) {
            case APPROVED -> eventOutbox.enqueue(KafkaTopics.CARD_APPROVED, cardId,
                    new CardApprovedEvent(cardId, reason, actor));
            case REJECTED -> eventOutbox.enqueue(KafkaTopics.CARD_REJECTED, cardId,
                    new CardRejectedEvent(cardId, reason, actor));
            case ISSUED -> eventOutbox.enqueue(KafkaTopics.CARD_ISSUED, cardId, new CardIssuedEvent(
                    cardId,
                    card.getCardNumber() != null ? card.getCardNumber().getLastFourDigits() : null,
                    card.getExpirationDate() != null ? card.getExpirationDate().getMonth() : null,
                    card.getExpirationDate() != null ? card.getExpirationDate().getYear() : null,
                    reason,
                    actor
            ));
            case ACTIVE -> {
                if (previousStatus.isBlocked()) {
                    eventOutbox.enqueue(KafkaTopics.CARD_UNBLOCKED, cardId,
                            new CardUnblockedEvent(cardId, now, reason, actor));
                } else {
                    eventOutbox.enqueue(KafkaTopics.CARD_ACTIVATED, cardId,
                            new CardActivatedEvent(cardId, now, reason, actor));
                }
            }
            case BLOCKED, BLOCKED_FRAUD -> eventOutbox.enqueue(KafkaTopics.CARD_BLOCKED, cardId, new CardBlockedEvent(
                    cardId,
                    card.getStatus() == CardStatus.BLOCKED_FRAUD ? "FRAUD" : "NORMAL",
                    card.getBlockedAt() != null ? card.getBlockedAt() : now,
                    reason,
                    actor
            ));
            case CANCELLED -> eventOutbox.enqueue(KafkaTopics.CARD_CANCELLED, cardId, new CardCancelledEvent(
                    cardId,
                    card.getCancelledAt() != null ? card.getCancelledAt() : now,
                    reason,
                    actor
            ));
            default -> log.debug("No event defined for status {} (card {})", card.getStatus(), cardId);
        }
    }
}
