package com.unisono.card.payment.domain.port.output;

import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Port for fraud detection and risk scoring
 */
public interface FraudCheckPort {

    /**
     * Calculate risk score for a transaction (0-100)
     */
    Integer calculateRiskScore(Transaction transaction);

    /**
     * Check if transaction is suspicious
     */
    boolean isSuspicious(Transaction transaction);

    /**
     * Check velocity rules (too many transactions in short time)
     */
    boolean violatesVelocityRules(CardId cardId);

    /**
     * Check geolocation anomalies
     */
    boolean hasGeolocationAnomaly(CardId cardId, String merchantCountry);

    /**
     * Check amount anomalies (unusual transaction amount)
     */
    boolean hasAmountAnomaly(Transaction transaction);

    /**
     * Report fraud attempt
     */
    void reportFraud(TransactionId transactionId, String reason);
}
