package com.unisono.card.events.domain.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.unisono.card.events.domain.DomainEvent;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.ToString;

/**
 * El autorizador aprobó la transacción. Contrato de integración de payment-processing (topic en KafkaTopics).
 * Campos planos: se puede leer con esta clase o como JSON sin depender del dominio de pagos.
 */
@Getter
@ToString(callSuper = true)
public class PaymentAuthorizedEvent extends DomainEvent {

    private final String transactionId;

    private final String cardId;

    private final String authorizationCode;

    /** Score de fraude 0-100 */
    private final Integer riskScore;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime authorizedAt;

    @JsonCreator
    public PaymentAuthorizedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("transactionId") String transactionId,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("authorizationCode") String authorizationCode,
            @JsonProperty("riskScore") Integer riskScore,
            @JsonProperty("authorizedAt") LocalDateTime authorizedAt
    ) {
        super(eventId, occurredOn);
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.authorizationCode = authorizationCode;
        this.riskScore = riskScore;
        this.authorizedAt = authorizedAt;
    }
}
