package com.unisono.card.payment.domain.exception;

import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Exception thrown when fraud is detected in a transaction
 */
public class FraudDetectedException extends PaymentException {

    private final TransactionId transactionId;
    private final CardId cardId;
    private final Integer riskScore;
    private final String fraudReason;

    public FraudDetectedException(TransactionId transactionId, CardId cardId,
                                  Integer riskScore, String fraudReason) {
        super(String.format(
                "Fraud detected for transaction %s on card %s. Risk score: %d, Reason: %s",
                transactionId, cardId, riskScore, fraudReason
        ));
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.riskScore = riskScore;
        this.fraudReason = fraudReason;
    }

    public static FraudDetectedException highRiskScore(TransactionId transactionId, CardId cardId,
                                                       Integer riskScore) {
        return new FraudDetectedException(
                transactionId, cardId, riskScore,
                "Risk score exceeds threshold"
        );
    }

    public static FraudDetectedException velocityViolation(TransactionId transactionId, CardId cardId) {
        return new FraudDetectedException(
                transactionId, cardId, 100,
                "Velocity check failed - too many transactions"
        );
    }

    public static FraudDetectedException geolocationMismatch(TransactionId transactionId, CardId cardId) {
        return new FraudDetectedException(
                transactionId, cardId, 85,
                "Geolocation mismatch detected"
        );
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public CardId getCardId() {
        return cardId;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public String getFraudReason() {
        return fraudReason;
    }
}
