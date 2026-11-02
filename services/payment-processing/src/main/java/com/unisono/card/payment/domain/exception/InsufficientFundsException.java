package com.unisono.card.payment.domain.exception;

import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;

/**
 * Exception thrown when a card has insufficient funds/credit for a transaction
 */
public class InsufficientFundsException extends PaymentException {

    private final TransactionId transactionId;
    private final CardId cardId;
    private final Money requestedAmount;
    private final Money availableAmount;

    public InsufficientFundsException(TransactionId transactionId, CardId cardId,
                                      Money requestedAmount, Money availableAmount) {
        super(String.format(
                "Insufficient funds for transaction %s on card %s. Requested: %s, Available: %s",
                transactionId, cardId, requestedAmount, availableAmount
        ));
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.requestedAmount = requestedAmount;
        this.availableAmount = availableAmount;
    }

    public InsufficientFundsException(CardId cardId, Money requestedAmount, Money availableAmount) {
        super(String.format(
                "Insufficient funds on card %s. Requested: %s, Available: %s",
                cardId, requestedAmount, availableAmount
        ));
        this.transactionId = null;
        this.cardId = cardId;
        this.requestedAmount = requestedAmount;
        this.availableAmount = availableAmount;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public CardId getCardId() {
        return cardId;
    }

    public Money getRequestedAmount() {
        return requestedAmount;
    }

    public Money getAvailableAmount() {
        return availableAmount;
    }

    public Money getShortfall() {
        return Money.of(
                requestedAmount.getAmount().subtract(availableAmount.getAmount()),
                requestedAmount.getCurrency()
        );
    }
}
