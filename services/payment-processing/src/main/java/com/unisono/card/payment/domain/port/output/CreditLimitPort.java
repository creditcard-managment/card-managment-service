package com.unisono.card.payment.domain.port.output;

import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;

import java.math.BigDecimal;

/**
 * Port for checking and managing credit limits
 */
public interface CreditLimitPort {

    /**
     * Get available credit for a card
     */
    Money getAvailableCredit(CardId cardId);

    /**
     * Check if card has sufficient funds for amount
     */
    boolean hasSufficientFunds(CardId cardId, Money amount);

    /**
     * Reserve credit (hold funds)
     */
    void reserveCredit(CardId cardId, Money amount);

    /**
     * Release reserved credit
     */
    void releaseCredit(CardId cardId, Money amount);

    /**
     * Consume credit (finalize transaction)
     */
    void consumeCredit(CardId cardId, Money amount);

    /**
     * Restore credit (refund/reversal)
     */
    void restoreCredit(CardId cardId, Money amount);
}
