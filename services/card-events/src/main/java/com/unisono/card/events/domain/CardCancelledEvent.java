package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Evento publicado cuando se cancela una tarjeta
 */
@Getter
@ToString(callSuper = true)
public class CardCancelledEvent extends DomainEvent {

    private final String cardId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime cancelledAt;

    private final String reason;
    private final String cancelledBy;

    @JsonCreator
    public CardCancelledEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("cancelledAt") LocalDateTime cancelledAt,
            @JsonProperty("reason") String reason,
            @JsonProperty("cancelledBy") String cancelledBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.cancelledAt = cancelledAt;
        this.reason = reason;
        this.cancelledBy = cancelledBy;
    }

    public CardCancelledEvent(
            String cardId,
            LocalDateTime cancelledAt,
            String reason,
            String cancelledBy
    ) {
        super();
        this.cardId = cardId;
        this.cancelledAt = cancelledAt;
        this.reason = reason;
        this.cancelledBy = cancelledBy;
    }
}
