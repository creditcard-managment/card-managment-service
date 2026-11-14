package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento publicado cuando se asigna el límite de crédito de una tarjeta (credit-limit.assigned)
 */
@Getter
@ToString(callSuper = true)
public class CreditLimitAssignedEvent extends DomainEvent {

    private final String cardId;
    private final String creditLimitId;
    private final BigDecimal purchaseLimitAmount;
    private final BigDecimal cashAdvanceLimitAmount;
    private final BigDecimal dailyLimitAmount;
    private final BigDecimal monthlyLimitAmount;
    private final String currency;
    private final String assignedBy;

    @JsonCreator
    public CreditLimitAssignedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("creditLimitId") String creditLimitId,
            @JsonProperty("purchaseLimitAmount") BigDecimal purchaseLimitAmount,
            @JsonProperty("cashAdvanceLimitAmount") BigDecimal cashAdvanceLimitAmount,
            @JsonProperty("dailyLimitAmount") BigDecimal dailyLimitAmount,
            @JsonProperty("monthlyLimitAmount") BigDecimal monthlyLimitAmount,
            @JsonProperty("currency") String currency,
            @JsonProperty("assignedBy") String assignedBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.creditLimitId = creditLimitId;
        this.purchaseLimitAmount = purchaseLimitAmount;
        this.cashAdvanceLimitAmount = cashAdvanceLimitAmount;
        this.dailyLimitAmount = dailyLimitAmount;
        this.monthlyLimitAmount = monthlyLimitAmount;
        this.currency = currency;
        this.assignedBy = assignedBy;
    }

    public CreditLimitAssignedEvent(
            String cardId,
            String creditLimitId,
            BigDecimal purchaseLimitAmount,
            BigDecimal cashAdvanceLimitAmount,
            BigDecimal dailyLimitAmount,
            BigDecimal monthlyLimitAmount,
            String currency,
            String assignedBy
    ) {
        super();
        this.cardId = cardId;
        this.creditLimitId = creditLimitId;
        this.purchaseLimitAmount = purchaseLimitAmount;
        this.cashAdvanceLimitAmount = cashAdvanceLimitAmount;
        this.dailyLimitAmount = dailyLimitAmount;
        this.monthlyLimitAmount = monthlyLimitAmount;
        this.currency = currency;
        this.assignedBy = assignedBy;
    }
}
