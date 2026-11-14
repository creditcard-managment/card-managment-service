package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Evento publicado cuando se bloquea una tarjeta
 */
@Getter
@ToString(callSuper = true)
public class CardBlockedEvent extends DomainEvent implements CardStatusChange {

    private final String cardId;

    /** Estado anterior de la tarjeta (CardStatus); nulo en mensajes anteriores a este campo. */
    private final String previousStatus;

    private final String blockType; // NORMAL, FRAUD

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime blockedAt;

    private final String reason;
    private final String blockedBy;

    @JsonCreator
    public CardBlockedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("previousStatus") String previousStatus,
            @JsonProperty("blockType") String blockType,
            @JsonProperty("blockedAt") LocalDateTime blockedAt,
            @JsonProperty("reason") String reason,
            @JsonProperty("blockedBy") String blockedBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.previousStatus = previousStatus;
        this.blockType = blockType;
        this.blockedAt = blockedAt;
        this.reason = reason;
        this.blockedBy = blockedBy;
    }

    public CardBlockedEvent(
            String cardId,
            String previousStatus,
            String blockType,
            LocalDateTime blockedAt,
            String reason,
            String blockedBy
    ) {
        super();
        this.cardId = cardId;
        this.previousStatus = previousStatus;
        this.blockType = blockType;
        this.blockedAt = blockedAt;
        this.reason = reason;
        this.blockedBy = blockedBy;
    }

    @Override
    public String newStatus() {
        return "FRAUD".equals(blockType) ? "BLOCKED_FRAUD" : "BLOCKED";
    }

    @Override
    public String actor() {
        return blockedBy;
    }

    @Override
    public LocalDateTime changedAt() {
        return blockedAt != null ? blockedAt : getOccurredOn();
    }
}
