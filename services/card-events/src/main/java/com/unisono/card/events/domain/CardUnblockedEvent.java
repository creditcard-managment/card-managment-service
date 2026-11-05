package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Evento publicado cuando se desbloquea una tarjeta
 */
@Getter
@ToString(callSuper = true)
public class CardUnblockedEvent extends DomainEvent {

    private final String cardId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime unblockedAt;

    private final String reason;
    private final String unblockedBy;

    @JsonCreator
    public CardUnblockedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("unblockedAt") LocalDateTime unblockedAt,
            @JsonProperty("reason") String reason,
            @JsonProperty("unblockedBy") String unblockedBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.unblockedAt = unblockedAt;
        this.reason = reason;
        this.unblockedBy = unblockedBy;
    }

    public CardUnblockedEvent(
            String cardId,
            LocalDateTime unblockedAt,
            String reason,
            String unblockedBy
    ) {
        super();
        this.cardId = cardId;
        this.unblockedAt = unblockedAt;
        this.reason = reason;
        this.unblockedBy = unblockedBy;
    }
}
