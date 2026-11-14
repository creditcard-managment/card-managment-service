package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento publicado cuando cambia uno de los límites de una tarjeta (credit-limit.updated).
 * Un cambio de los límites diario y mensual a la vez publica un evento por cada uno.
 */
@Getter
@ToString(callSuper = true)
public class CreditLimitUpdatedEvent extends DomainEvent {

    private final String cardId;
    private final String creditLimitId;
    /** PURCHASE, CASH_ADVANCE, DAILY o MONTHLY */
    private final String limitType;
    private final BigDecimal previousAmount;
    private final BigDecimal newAmount;
    private final String currency;
    private final String reason;
    private final String updatedBy;

    @JsonCreator
    public CreditLimitUpdatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("creditLimitId") String creditLimitId,
            @JsonProperty("limitType") String limitType,
            @JsonProperty("previousAmount") BigDecimal previousAmount,
            @JsonProperty("newAmount") BigDecimal newAmount,
            @JsonProperty("currency") String currency,
            @JsonProperty("reason") String reason,
            @JsonProperty("updatedBy") String updatedBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.creditLimitId = creditLimitId;
        this.limitType = limitType;
        this.previousAmount = previousAmount;
        this.newAmount = newAmount;
        this.currency = currency;
        this.reason = reason;
        this.updatedBy = updatedBy;
    }

    public CreditLimitUpdatedEvent(
            String cardId,
            String creditLimitId,
            String limitType,
            BigDecimal previousAmount,
            BigDecimal newAmount,
            String currency,
            String reason,
            String updatedBy
    ) {
        super();
        this.cardId = cardId;
        this.creditLimitId = creditLimitId;
        this.limitType = limitType;
        this.previousAmount = previousAmount;
        this.newAmount = newAmount;
        this.currency = currency;
        this.reason = reason;
        this.updatedBy = updatedBy;
    }
}
