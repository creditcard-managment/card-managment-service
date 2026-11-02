package com.unisono.card.payment.domain.port.output;

import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.shared.domain.valueobject.CardId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for Transaction aggregate
 */
public interface TransactionRepository {

    /**
     * Save a transaction
     */
    Transaction save(Transaction transaction);

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
     * Find transactions by status
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * Find transactions requiring settlement
     */
    List<Transaction> findPendingSettlement();

    /**
     * Find transactions created before a specific date
     */
    List<Transaction> findCreatedBefore(LocalDateTime date);

    /**
     * Check if transaction exists
     */
    boolean existsById(TransactionId transactionId);

    /**
     * Delete transaction (soft delete)
     */
    void delete(TransactionId transactionId);
}
