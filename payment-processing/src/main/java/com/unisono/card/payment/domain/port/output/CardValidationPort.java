package com.unisono.card.payment.domain.port.output;

import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardNumber;
import com.unisono.card.shared.domain.valueobject.CVV;

/**
 * Port for validating card information
 */
public interface CardValidationPort {

    /**
     * Validate if card is active and can process transactions
     */
    boolean isCardActive(CardId cardId);

    /**
     * Validate card number (Luhn algorithm)
     */
    boolean validateCardNumber(CardNumber cardNumber);

    /**
     * Validate CVV
     */
    boolean validateCVV(CardId cardId, CVV cvv);

    /**
     * Check if card is expired
     */
    boolean isCardExpired(CardId cardId);

    /**
     * Check if card is blocked or suspended
     */
    boolean isCardBlocked(CardId cardId);

    /**
     * Get card status
     */
    String getCardStatus(CardId cardId);
}
