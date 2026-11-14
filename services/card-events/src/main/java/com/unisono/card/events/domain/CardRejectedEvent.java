package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Evento publicado cuando se rechaza una solicitud de tarjeta
 */
@Getter
@ToString(callSuper = true)
public class CardRejectedEvent extends DomainEvent implements CardStatusChange {

    private final String cardId;

    /** Estado anterior de la tarjeta (CardStatus); nulo en mensajes anteriores a este campo. */
    private final String previousStatus;
    private final String reason;
    private final String rejectedBy;

    @JsonCreator
    public CardRejectedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("previousStatus") String previousStatus,
            @JsonProperty("reason") String reason,
            @JsonProperty("rejectedBy") String rejectedBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.previousStatus = previousStatus;
        this.reason = reason;
        this.rejectedBy = rejectedBy;
    }

    public CardRejectedEvent(
            String cardId,
            String previousStatus,
            String reason,
            String rejectedBy
    ) {
        super();
        this.cardId = cardId;
        this.previousStatus = previousStatus;
        this.reason = reason;
        this.rejectedBy = rejectedBy;
    }

    @Override
    public String newStatus() {
        return "REJECTED";
    }

    @Override
    public String actor() {
        return rejectedBy;
    }

    @Override
    public LocalDateTime changedAt() {
        return getOccurredOn();
    }
}
