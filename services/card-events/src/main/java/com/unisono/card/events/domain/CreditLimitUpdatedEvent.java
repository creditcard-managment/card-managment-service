package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento publicado cuando se modifica el límite de crédito de una tarjeta
 */
@Getter
@ToString(callSuper = true)
public class CreditLimitUpdatedEvent extends DomainEvent {

    private final String cardId;
    private final BigDecimal previousPurchaseLimitAmount;
    private final BigDecimal newPurchaseLimitAmount;
    private final String currency;
    private final String reason;
    private final String updatedBy;

    @JsonCreator
    public CreditLimitUpdatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("previousPurchaseLimitAmount") BigDecimal previousPurchaseLimitAmount,
            @JsonProperty("newPurchaseLimitAmount") BigDecimal newPurchaseLimitAmount,
            @JsonProperty("currency") String currency,
            @JsonProperty("reason") String reason,
            @JsonProperty("updatedBy") String updatedBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.previousPurchaseLimitAmount = previousPurchaseLimitAmount;
        this.newPurchaseLimitAmount = newPurchaseLimitAmount;
        this.currency = currency;
        this.reason = reason;
        this.updatedBy = updatedBy;
    }

    public CreditLimitUpdatedEvent(
            String cardId,
            BigDecimal previousPurchaseLimitAmount,
            BigDecimal newPurchaseLimitAmount,
            String currency,
            String reason,
            String updatedBy
    ) {
        super();
        this.cardId = cardId;
        this.previousPurchaseLimitAmount = previousPurchaseLimitAmount;
        this.newPurchaseLimitAmount = newPurchaseLimitAmount;
        this.currency = currency;
        this.reason = reason;
        this.updatedBy = updatedBy;
    }
}
