package com.unisono.card.payment.domain.exception;

import com.unisono.card.payment.domain.model.TransactionId;

/**
 * Exception thrown when a transaction operation is invalid
 */
public class InvalidTransactionException extends PaymentException {

    private TransactionId transactionId;

    public InvalidTransactionException(String message) {
        super(message);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTransactionException(TransactionId transactionId, String message) {
        super(String.format("Transaction %s: %s", transactionId, message));
        this.transactionId = transactionId;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }
}
