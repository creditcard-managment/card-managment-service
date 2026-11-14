package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Evento publicado cuando se aprueba una tarjeta
 */
@Getter
@ToString(callSuper = true)
public class CardApprovedEvent extends DomainEvent implements CardStatusChange {

    private final String cardId;

    /** Estado anterior de la tarjeta (CardStatus); nulo en mensajes anteriores a este campo. */
    private final String previousStatus;
    private final String reason;
    private final String approvedBy;

    @JsonCreator
    public CardApprovedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("previousStatus") String previousStatus,
            @JsonProperty("reason") String reason,
            @JsonProperty("approvedBy") String approvedBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.previousStatus = previousStatus;
        this.reason = reason;
        this.approvedBy = approvedBy;
    }

    public CardApprovedEvent(
            String cardId,
            String previousStatus,
            String reason,
            String approvedBy
    ) {
        super();
        this.cardId = cardId;
        this.previousStatus = previousStatus;
        this.reason = reason;
        this.approvedBy = approvedBy;
    }

    @Override
    public String newStatus() {
        return "APPROVED";
    }

    @Override
    public String actor() {
        return approvedBy;
    }

    @Override
    public LocalDateTime changedAt() {
        return getOccurredOn();
    }
}
