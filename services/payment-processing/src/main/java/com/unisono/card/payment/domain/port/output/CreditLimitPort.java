package com.unisono.card.payment.domain.port.output;

import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionType;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;

/**
 * Port for checking and managing credit limits (card-limits)
 *
 * Credit is consumed when a debit is approved and released when it is reversed or refunded.
 * Settlement does not change the available credit.
 */
public interface CreditLimitPort {

    /**
     * Check if the card can spend the amount (available credit, daily and monthly limits)
     */
    CreditCheck checkCredit(CardId cardId, Money amount, TransactionType type);

    /**
     * Consume credit for an approved purchase or cash advance.
     * <p>
     * The implementation guarantees that the consumption is undone in card-limits if the database transaction
     * that records {@code transactionId} does not commit (saga with compensation).
     */
    void consumeCredit(TransactionId transactionId, CardId cardId, Money amount, TransactionType type,
                       String description);

    /**
     * Undo the consumption of a reversed or refunded transaction: gives back the available credit and,
     * if the original transaction is from the current day or month, the daily and monthly quota.
     *
     * Like {@link #consumeCredit}, it is undone in card-limits if the database transaction does not commit.
     *
     * @param transactionId           transaction recorded together with the restoration (the reversed
     *                                transaction, or the refund)
     * @param originalTransactionDate date of the transaction that consumed the credit
     */
    void restoreCredit(TransactionId transactionId, CardId cardId, Money amount,
                       java.time.LocalDate originalTransactionDate, String description);

    record CreditCheck(boolean approved, String reason, Money available) {
    }
}
