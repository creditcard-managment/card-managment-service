package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Evento publicado cuando se emite una tarjeta física
 */
@Getter
@ToString(callSuper = true)
public class CardIssuedEvent extends DomainEvent implements CardStatusChange {

    private final String cardId;

    /** Estado anterior de la tarjeta (CardStatus); nulo en mensajes anteriores a este campo. */
    private final String previousStatus;
    private final String lastFourDigits;
    private final Integer expirationMonth;
    private final Integer expirationYear;
    private final String reason;
    private final String issuedBy;

    @JsonCreator
    public CardIssuedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("previousStatus") String previousStatus,
            @JsonProperty("lastFourDigits") String lastFourDigits,
            @JsonProperty("expirationMonth") Integer expirationMonth,
            @JsonProperty("expirationYear") Integer expirationYear,
            @JsonProperty("reason") String reason,
            @JsonProperty("issuedBy") String issuedBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.previousStatus = previousStatus;
        this.lastFourDigits = lastFourDigits;
        this.expirationMonth = expirationMonth;
        this.expirationYear = expirationYear;
        this.reason = reason;
        this.issuedBy = issuedBy;
    }

    public CardIssuedEvent(
            String cardId,
            String previousStatus,
            String lastFourDigits,
            Integer expirationMonth,
            Integer expirationYear,
            String reason,
            String issuedBy
    ) {
        super();
        this.cardId = cardId;
        this.previousStatus = previousStatus;
        this.lastFourDigits = lastFourDigits;
        this.expirationMonth = expirationMonth;
        this.expirationYear = expirationYear;
        this.reason = reason;
        this.issuedBy = issuedBy;
    }

    @Override
    public String newStatus() {
        return "ISSUED";
    }

    @Override
    public String actor() {
        return issuedBy;
    }

    @Override
    public LocalDateTime changedAt() {
        return getOccurredOn();
    }
}
