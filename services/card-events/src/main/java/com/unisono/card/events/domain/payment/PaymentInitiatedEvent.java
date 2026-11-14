package com.unisono.card.events.domain.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.unisono.card.events.domain.DomainEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.ToString;

/**
 * Se creó una compra o un adelanto (también si después se rechaza). Contrato de integración de payment-processing (topic en KafkaTopics).
 * Campos planos: se puede leer con esta clase o como JSON sin depender del dominio de pagos.
 */
@Getter
@ToString(callSuper = true)
public class PaymentInitiatedEvent extends DomainEvent {

    private final String transactionId;

    private final String cardId;

    /** PURCHASE o CASH_ADVANCE */
    private final String transactionType;

    /** Monto que se le cobra a la tarjeta (en la moneda local si hubo conversión) */
    private final BigDecimal amount;

    /** Moneda de amount (ISO 4217) */
    private final String currency;

    /** Monto en la moneda original, solo si hubo conversión (si no, null) */
    private final BigDecimal originalAmount;

    /** Moneda original, solo si hubo conversión */
    private final String originalCurrency;

    /** Tasa de cambio aplicada, solo si hubo conversión */
    private final BigDecimal exchangeRate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime initiatedAt;

    @JsonCreator
    public PaymentInitiatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("transactionId") String transactionId,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("transactionType") String transactionType,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("currency") String currency,
            @JsonProperty("originalAmount") BigDecimal originalAmount,
            @JsonProperty("originalCurrency") String originalCurrency,
            @JsonProperty("exchangeRate") BigDecimal exchangeRate,
            @JsonProperty("initiatedAt") LocalDateTime initiatedAt
    ) {
        super(eventId, occurredOn);
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.currency = currency;
        this.originalAmount = originalAmount;
        this.originalCurrency = originalCurrency;
        this.exchangeRate = exchangeRate;
        this.initiatedAt = initiatedAt;
    }
}
