package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento publicado cuando una transacción consume crédito disponible
 */
@Getter
@ToString(callSuper = true)
public class CreditLimitConsumedEvent extends DomainEvent {

    private final String cardId;
    private final String transactionId;
    private final BigDecimal amount;
    private final BigDecimal availableAmount;
    private final String currency;

    @JsonCreator
    public CreditLimitConsumedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("transactionId") String transactionId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("availableAmount") BigDecimal availableAmount,
            @JsonProperty("currency") String currency
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.availableAmount = availableAmount;
        this.currency = currency;
    }

    public CreditLimitConsumedEvent(
            String cardId,
            String transactionId,
            BigDecimal amount,
            BigDecimal availableAmount,
            String currency
    ) {
        super();
        this.cardId = cardId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.availableAmount = availableAmount;
        this.currency = currency;
    }
}
