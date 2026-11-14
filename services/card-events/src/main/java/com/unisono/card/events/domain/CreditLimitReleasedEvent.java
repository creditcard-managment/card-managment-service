package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento publicado cuando baja el crédito consumido de una tarjeta (credit-limit.released)
 */
@Getter
@ToString(callSuper = true)
public class CreditLimitReleasedEvent extends DomainEvent {

    private final String cardId;
    private final String creditLimitId;
    /** Id de la operación de crédito; nulo en el pago del resumen, que no tiene */
    private final String operationId;
    /** PAYMENT (pago del resumen), REFUND (devolución) o CONSUMPTION_CANCELLED (se deshizo un consumo) */
    private final String operationType;
    /** Monto liberado por este movimiento */
    private final BigDecimal amount;
    /** Crédito consumido total después del movimiento */
    private final BigDecimal consumedAmount;
    /** Crédito disponible después del movimiento */
    private final BigDecimal availableAmount;
    private final String currency;

    @JsonCreator
    public CreditLimitReleasedEvent(
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

    public CreditLimitReleasedEvent(
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
