package com.unisono.card.payment.domain.event;

import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when fraud is detected in a transaction
 */
@Getter
public class FraudDetected extends DomainEvent implements PaymentEvent {

    private final TransactionId transactionId;
    private final CardId cardId;
    private final Integer riskScore;
    private final String fraudReason;
    private final String detectionMethod;
    private final LocalDateTime detectedAt;

    public FraudDetected(TransactionId transactionId, CardId cardId, Integer riskScore,
                         String fraudReason, String detectionMethod, LocalDateTime detectedAt) {
        super();
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.riskScore = riskScore;
        this.fraudReason = fraudReason;
        this.detectionMethod = detectionMethod;
        this.detectedAt = detectedAt;
    }

    public FraudDetected(String eventId, LocalDateTime occurredOn,
                         TransactionId transactionId, CardId cardId, Integer riskScore,
                         String fraudReason, String detectionMethod, LocalDateTime detectedAt) {
        super(eventId, occurredOn);
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.riskScore = riskScore;
        this.fraudReason = fraudReason;
        this.detectionMethod = detectionMethod;
        this.detectedAt = detectedAt;
    }

    public boolean isCritical() {
        return riskScore != null && riskScore >= 90;
    }

    @Override
    public String toString() {
        return String.format("FraudDetected[transactionId=%s, riskScore=%d, reason=%s]",
                transactionId, riskScore, fraudReason);
    }
}
