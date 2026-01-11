package com.unisono.card.payment.domain.exception;

import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;

/**
 * Exception thrown when attempting to process a transaction that is already in a final state
 */
public class TransactionAlreadyProcessedException extends PaymentException {

    private final TransactionId transactionId;
    private final TransactionStatus currentStatus;

    public TransactionAlreadyProcessedException(String message) {
        super(message);
        this.transactionId = null;
        this.currentStatus = null;
    }

    public TransactionAlreadyProcessedException(TransactionId transactionId, TransactionStatus currentStatus) {
        super(String.format("Transaction %s is already in final status: %s and cannot be modified",
                transactionId, currentStatus));
        this.transactionId = transactionId;
        this.currentStatus = currentStatus;
    }

    public TransactionAlreadyProcessedException(TransactionId transactionId, TransactionStatus currentStatus,
                                                String operation) {
        super(String.format("Cannot %s transaction %s - already in final status: %s",
                operation, transactionId, currentStatus));
        this.transactionId = transactionId;
        this.currentStatus = currentStatus;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public TransactionStatus getCurrentStatus() {
        return currentStatus;
    }
}
