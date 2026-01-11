package com.unisono.card.payment.domain.port.input;

import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;

import java.util.List;

/**
 * Use case for settling transactions (batch processing)
 */
public interface SettleTransactionsUseCase {

    /**
     * Settle a single transaction
     */
    Transaction settleTransaction(TransactionId transactionId);

    /**
     * Settle multiple transactions in batch
     */
    List<Transaction> settleBatch(List<TransactionId> transactionIds);

    /**
     * Settle all pending transactions
     */
    List<Transaction> settleAllPending();
}
