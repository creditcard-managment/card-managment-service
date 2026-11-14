package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Evento publicado cuando se activa una tarjeta
 */
@Getter
@ToString(callSuper = true)
public class CardActivatedEvent extends DomainEvent implements CardStatusChange {

    private final String cardId;

    /** Estado anterior de la tarjeta (CardStatus); nulo en mensajes anteriores a este campo. */
    private final String previousStatus;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime activatedAt;

    private final String reason;
    private final String activatedBy;

    @JsonCreator
    public CardActivatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("previousStatus") String previousStatus,
            @JsonProperty("activatedAt") LocalDateTime activatedAt,
            @JsonProperty("reason") String reason,
            @JsonProperty("activatedBy") String activatedBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.previousStatus = previousStatus;
        this.activatedAt = activatedAt;
        this.reason = reason;
        this.activatedBy = activatedBy;
    }

    public CardActivatedEvent(
            String cardId,
            String previousStatus,
            LocalDateTime activatedAt,
            String reason,
            String activatedBy
    ) {
        super();
        this.cardId = cardId;
        this.previousStatus = previousStatus;
        this.activatedAt = activatedAt;
        this.reason = reason;
        this.activatedBy = activatedBy;
    }

    @Override
    public String newStatus() {
        return "ACTIVE";
    }

    @Override
    public String actor() {
        return activatedBy;
    }

    @Override
    public LocalDateTime changedAt() {
        return activatedAt != null ? activatedAt : getOccurredOn();
    }
}
