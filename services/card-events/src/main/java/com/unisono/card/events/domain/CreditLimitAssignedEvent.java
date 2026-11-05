package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento publicado cuando se asigna un límite de crédito a una tarjeta
 */
@Getter
@ToString(callSuper = true)
public class CreditLimitAssignedEvent extends DomainEvent {

    private final String cardId;
    private final BigDecimal purchaseLimitAmount;
    private final BigDecimal cashAdvanceLimitAmount;
    private final String currency;
    private final String assignedBy;

    @JsonCreator
    public CreditLimitAssignedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("purchaseLimitAmount") BigDecimal purchaseLimitAmount,
            @JsonProperty("cashAdvanceLimitAmount") BigDecimal cashAdvanceLimitAmount,
            @JsonProperty("currency") String currency,
            @JsonProperty("assignedBy") String assignedBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.purchaseLimitAmount = purchaseLimitAmount;
        this.cashAdvanceLimitAmount = cashAdvanceLimitAmount;
        this.currency = currency;
        this.assignedBy = assignedBy;
    }

    public CreditLimitAssignedEvent(
            String cardId,
            BigDecimal purchaseLimitAmount,
            BigDecimal cashAdvanceLimitAmount,
            String currency,
            String assignedBy
    ) {
        super();
        this.cardId = cardId;
        this.purchaseLimitAmount = purchaseLimitAmount;
        this.cashAdvanceLimitAmount = cashAdvanceLimitAmount;
        this.currency = currency;
        this.assignedBy = assignedBy;
    }
}
