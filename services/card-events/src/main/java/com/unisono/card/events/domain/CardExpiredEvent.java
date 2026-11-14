package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Evento publicado cuando una tarjeta pasa a EXPIRED (vencida). Es un estado final: no admite más operaciones.
 */
@Getter
@ToString(callSuper = true)
public class CardExpiredEvent extends DomainEvent implements CardStatusChange {

    private final String cardId;

    /** Estado anterior de la tarjeta (CardStatus); nulo en mensajes anteriores a este campo. */
    private final String previousStatus;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime expiredAt;

    private final String reason;
    private final String expiredBy;

    @JsonCreator
    public CardExpiredEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("previousStatus") String previousStatus,
            @JsonProperty("expiredAt") LocalDateTime expiredAt,
            @JsonProperty("reason") String reason,
            @JsonProperty("expiredBy") String expiredBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.previousStatus = previousStatus;
        this.expiredAt = expiredAt;
        this.reason = reason;
        this.expiredBy = expiredBy;
    }

    public CardExpiredEvent(
            String cardId,
            String previousStatus,
            LocalDateTime expiredAt,
            String reason,
            String expiredBy
    ) {
        super();
        this.cardId = cardId;
        this.previousStatus = previousStatus;
        this.expiredAt = expiredAt;
        this.reason = reason;
        this.expiredBy = expiredBy;
    }

    @Override
    public String newStatus() {
        return "EXPIRED";
    }

    @Override
    public String actor() {
        return expiredBy;
    }

    @Override
    public LocalDateTime changedAt() {
        return expiredAt != null ? expiredAt : getOccurredOn();
    }
}
