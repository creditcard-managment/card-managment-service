package com.unisono.card.payment.domain.port.input;

import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;

/**
 * Use case for reversing transactions
 */
public interface ReverseTransactionUseCase {

    /**
     * Reverse a transaction (cancel/rollback)
     */
    Transaction reverseTransaction(TransactionId transactionId, String reason);

    /**
     * Auto-reverse failed transaction
     */
    Transaction autoReverse(TransactionId transactionId);
}
