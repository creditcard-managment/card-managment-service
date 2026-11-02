package com.unisono.card.payment.domain.exception;

import com.unisono.card.payment.domain.model.TransactionId;

/**
 * Exception thrown when a transaction cannot be found
 */
public class TransactionNotFoundException extends PaymentException {

    private final TransactionId transactionId;

    public TransactionNotFoundException(TransactionId transactionId) {
        super(String.format("Transaction not found: %s", transactionId));
        this.transactionId = transactionId;
    }

    public TransactionNotFoundException(String transactionIdString) {
        super(String.format("Transaction not found: %s", transactionIdString));
        this.transactionId = null;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }
}
