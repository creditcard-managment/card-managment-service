package com.unisono.card.payment.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.TransactionEntity;
import com.unisono.card.payment.domain.model.TransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for TransactionEntity
 */
@Repository
public interface JpaTransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    /**
     * Load a transaction locking its row until the end of the transaction (refunds of the same original
     * are serialized, so the refunded total is checked against committed data)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TransactionEntity t WHERE t.transactionId = :id")
    Optional<TransactionEntity> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Refunds of a transaction
     */
    List<TransactionEntity> findByOriginalTransactionIdOrderByTransactionDateAsc(UUID originalTransactionId);

    /**
     * Pre-authorizations (AUTHORIZED purchases) authorized before the threshold
     */
    @Query("SELECT t FROM TransactionEntity t " +
            "WHERE t.transactionStatus = 'AUTHORIZED' AND t.transactionType = 'PURCHASE' " +
            "AND COALESCE(t.authorizationDate, t.transactionDate) < :threshold " +
            "ORDER BY t.transactionDate ASC")
    List<TransactionEntity> findExpiredPreAuthorizations(@Param("threshold") LocalDateTime threshold);

    /**
     * Find all transactions for a specific card
     */
    List<TransactionEntity> findByCardIdOrderByTransactionDateDesc(String cardId);

    /**
     * Find transactions by card and status
     */
    List<TransactionEntity> findByCardIdAndTransactionStatusOrderByTransactionDateDesc(
            String cardId,
            TransactionStatus status
    );

    /**
     * Find transactions by card in date range
     */
    @Query("SELECT t FROM TransactionEntity t " +
            "WHERE t.cardId = :cardId " +
            "AND t.transactionDate BETWEEN :fromDate AND :toDate " +
            "ORDER BY t.transactionDate DESC")
    List<TransactionEntity> findByCardIdAndDateRange(
            @Param("cardId") String cardId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );


    /**
     * Find transactions pending settlement
     * (Approved transactions without settlement date)
     */
    @Query("SELECT t FROM TransactionEntity t " +
            "WHERE t.transactionStatus = 'APPROVED' " +
            "AND t.settlementDate IS NULL " +
            "ORDER BY t.transactionDate ASC")
    List<TransactionEntity> findPendingSettlement();



    /**
     * Check if transaction exists by ID
     */
    boolean existsByTransactionId(UUID transactionId);

}
