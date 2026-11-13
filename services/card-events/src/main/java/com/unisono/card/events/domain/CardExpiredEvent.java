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
public class CardExpiredEvent extends DomainEvent {

    private final String cardId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime expiredAt;

    private final String reason;
    private final String expiredBy;

    @JsonCreator
    public CardExpiredEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("expiredAt") LocalDateTime expiredAt,
            @JsonProperty("reason") String reason,
            @JsonProperty("expiredBy") String expiredBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.expiredAt = expiredAt;
        this.reason = reason;
        this.expiredBy = expiredBy;
    }

    public CardExpiredEvent(
            String cardId,
            LocalDateTime expiredAt,
            String reason,
            String expiredBy
    ) {
        super();
        this.cardId = cardId;
        this.expiredAt = expiredAt;
        this.reason = reason;
        this.expiredBy = expiredBy;
    }
}
