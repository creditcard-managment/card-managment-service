package com.unisono.card.events.domain.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.unisono.card.events.domain.DomainEvent;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.ToString;

/**
 * La transacción se rechazó. Contrato de integración de payment-processing (topic en KafkaTopics).
 * Campos planos: se puede leer con esta clase o como JSON sin depender del dominio de pagos.
 */
@Getter
@ToString(callSuper = true)
public class PaymentDeclinedEvent extends DomainEvent {

    private final String transactionId;

    private final String cardId;

    /** Código ISO 8583 del rechazo: 51 fondos, 54 vencida, 55 PIN, 59 fraude, 62 restringida, N7 CVV, o el del autorizador */
    private final String responseCode;

    private final String declineReason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime declinedAt;

    @JsonCreator
    public PaymentDeclinedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("transactionId") String transactionId,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("responseCode") String responseCode,
            @JsonProperty("declineReason") String declineReason,
            @JsonProperty("declinedAt") LocalDateTime declinedAt
    ) {
        super(eventId, occurredOn);
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.responseCode = responseCode;
        this.declineReason = declineReason;
        this.declinedAt = declinedAt;
    }
}
