package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento publicado cuando aumenta el crédito consumido de una tarjeta (credit-limit.consumed)
 */
@Getter
@ToString(callSuper = true)
public class CreditLimitConsumedEvent extends DomainEvent {

    private final String cardId;
    private final String creditLimitId;
    /** Id de la operación de crédito (el que envía payment-processing); nulo si no vino */
    private final String operationId;
    /** PURCHASE, CASH_ADVANCE o REFUND_CANCELLED (se deshizo una devolución) */
    private final String operationType;
    /** Monto consumido por este movimiento */
    private final BigDecimal amount;
    /** Crédito consumido total después del movimiento */
    private final BigDecimal consumedAmount;
    /** Crédito disponible después del movimiento */
    private final BigDecimal availableAmount;
    private final String currency;

    @JsonCreator
    public CreditLimitConsumedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("creditLimitId") String creditLimitId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("operationType") String operationType,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("consumedAmount") BigDecimal consumedAmount,
            @JsonProperty("availableAmount") BigDecimal availableAmount,
            @JsonProperty("currency") String currency
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.creditLimitId = creditLimitId;
        this.operationId = operationId;
        this.operationType = operationType;
        this.amount = amount;
        this.consumedAmount = consumedAmount;
        this.availableAmount = availableAmount;
        this.currency = currency;
    }

    public CreditLimitConsumedEvent(
            String cardId,
            String creditLimitId,
            String operationId,
            String operationType,
            BigDecimal amount,
            BigDecimal consumedAmount,
            BigDecimal availableAmount,
            String currency
    ) {
        super();
        this.cardId = cardId;
        this.creditLimitId = creditLimitId;
        this.operationId = operationId;
        this.operationType = operationType;
        this.amount = amount;
        this.consumedAmount = consumedAmount;
        this.availableAmount = availableAmount;
        this.currency = currency;
    }
}
