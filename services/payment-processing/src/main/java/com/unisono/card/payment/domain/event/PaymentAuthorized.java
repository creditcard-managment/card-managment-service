package com.unisono.card.payment.domain.event;

import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a payment is successfully authorized
 */
@Getter
public class PaymentAuthorized extends DomainEvent {

    private final TransactionId transactionId;
    private final CardId cardId;
    private final String authorizationCode;
    private final Integer riskScore;
    private final LocalDateTime authorizedAt;

    public PaymentAuthorized(TransactionId transactionId, CardId cardId,
                             String authorizationCode, Integer riskScore,
                             LocalDateTime authorizedAt) {
        super();
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.authorizationCode = authorizationCode;
        this.riskScore = riskScore;
        this.authorizedAt = authorizedAt;
    }

    public PaymentAuthorized(String eventId, LocalDateTime occurredOn,
                             TransactionId transactionId, CardId cardId,
                             String authorizationCode, Integer riskScore,
                             LocalDateTime authorizedAt) {
        super(eventId, occurredOn);
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.authorizationCode = authorizationCode;
        this.riskScore = riskScore;
        this.authorizedAt = authorizedAt;
    }

    public boolean isLowRisk() {
        return riskScore != null && riskScore < 30;
    }

    public boolean isHighRisk() {
        return riskScore != null && riskScore >= 70;
    }

    @Override
    public String toString() {
        return String.format("PaymentAuthorized[transactionId=%s, authCode=%s, riskScore=%d]",
                transactionId, authorizationCode, riskScore);
    }
}
