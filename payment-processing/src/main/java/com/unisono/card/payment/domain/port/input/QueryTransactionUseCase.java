package com.unisono.card.payment.domain.port.input;

import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.shared.domain.valueobject.CardId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Use case for querying transaction information
 */
public interface QueryTransactionUseCase {

    /**
     * Find transaction by ID
     */
    Optional<Transaction> findById(TransactionId transactionId);

    /**
     * Find all transactions for a card
     */
    List<Transaction> findByCardId(CardId cardId);

    /**
     * Find transactions by card and status
     */
    List<Transaction> findByCardIdAndStatus(CardId cardId, TransactionStatus status);

    /**
     * Find transactions by card in date range
     */
    List<Transaction> findByCardIdAndDateRange(CardId cardId, LocalDateTime from, LocalDateTime to);

    /**
     * Find pending settlement transactions
     */
    List<Transaction> findPendingSettlement();

    /**
     * Find transactions requiring reversal (timeout, errors, etc.)
     */
    List<Transaction> findRequiringReversal();
}
