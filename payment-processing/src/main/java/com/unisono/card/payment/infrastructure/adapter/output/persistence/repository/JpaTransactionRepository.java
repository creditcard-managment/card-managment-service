package com.unisono.card.payment.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.TransactionEntity;
import com.unisono.card.payment.domain.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * Find all transactions by status
     */
    List<TransactionEntity> findByTransactionStatusOrderByTransactionDateDesc(TransactionStatus status);

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
     * Find transactions created before a specific date
     */
    @Query("SELECT t FROM TransactionEntity t " +
            "WHERE t.createdAt < :date " +
            "ORDER BY t.createdAt DESC")
    List<TransactionEntity> findCreatedBefore(@Param("date") LocalDateTime date);

    /**
     * Find transactions by merchant
     */
    List<TransactionEntity> findByMerchantIdOrderByTransactionDateDesc(String merchantId);

    /**
     * Find transactions by merchant and status
     */
    List<TransactionEntity> findByMerchantIdAndTransactionStatus(
            String merchantId,
            TransactionStatus status
    );

    /**
     * Find transactions by authorization code
     */
    Optional<TransactionEntity> findByAuthorizationCode(String authorizationCode);

    /**
     * Count transactions by card and status
     */
    Long countByCardIdAndTransactionStatus(String cardId, TransactionStatus status);

    /**
     * Count transactions by card in date range
     */
    @Query("SELECT COUNT(t) FROM TransactionEntity t " +
            "WHERE t.cardId = :cardId " +
            "AND t.transactionDate BETWEEN :fromDate AND :toDate")
    Long countByCardIdAndDateRange(
            @Param("cardId") String cardId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    /**
     * Find high-risk transactions (risk score >= threshold)
     */
    @Query("SELECT t FROM TransactionEntity t " +
            "WHERE t.riskScore >= :threshold " +
            "ORDER BY t.riskScore DESC, t.transactionDate DESC")
    List<TransactionEntity> findHighRiskTransactions(@Param("threshold") Integer threshold);

    /**
     * Find transactions requiring reversal
     * (Pending or Authorized for more than specified hours)
     */
    @Query("SELECT t FROM TransactionEntity t " +
            "WHERE (t.transactionStatus = 'PENDING' OR t.transactionStatus = 'AUTHORIZED') " +
            "AND t.transactionDate < :threshold " +
            "ORDER BY t.transactionDate ASC")
    List<TransactionEntity> findRequiringReversal(@Param("threshold") LocalDateTime threshold);

    /**
     * Check if transaction exists by ID
     */
    boolean existsByTransactionId(UUID transactionId);

    /**
     * Find recent transactions by card (last N transactions)
     */
    @Query("SELECT t FROM TransactionEntity t " +
            "WHERE t.cardId = :cardId " +
            "ORDER BY t.transactionDate DESC " +
            "LIMIT :limit")
    List<TransactionEntity> findRecentByCardId(
            @Param("cardId") String cardId,
            @Param("limit") int limit
    );

    /**
     * Find transactions by card in specific status list
     */
    @Query("SELECT t FROM TransactionEntity t " +
            "WHERE t.cardId = :cardId " +
            "AND t.transactionStatus IN :statuses " +
            "ORDER BY t.transactionDate DESC")
    List<TransactionEntity> findByCardIdAndStatusIn(
            @Param("cardId") String cardId,
            @Param("statuses") List<TransactionStatus> statuses
    );

    /**
     * Sum of approved transaction amounts for a card
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t " +
            "WHERE t.cardId = :cardId " +
            "AND t.transactionStatus = 'APPROVED'")
    java.math.BigDecimal sumApprovedAmountByCardId(@Param("cardId") String cardId);

    /**
     * Sum of transaction amounts by card in date range
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t " +
            "WHERE t.cardId = :cardId " +
            "AND t.transactionDate BETWEEN :fromDate AND :toDate " +
            "AND t.transactionStatus IN ('APPROVED', 'SETTLED')")
    java.math.BigDecimal sumAmountByCardIdAndDateRange(
            @Param("cardId") String cardId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
