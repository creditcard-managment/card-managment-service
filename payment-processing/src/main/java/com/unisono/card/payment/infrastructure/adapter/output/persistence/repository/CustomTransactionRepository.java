package com.unisono.card.payment.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.TransactionEntity;
import com.unisono.card.payment.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Custom repository interface for complex transaction queries
 */
public interface CustomTransactionRepository {

    /**
     * Get transaction statistics for a card
     */
    Map<String, Object> getTransactionStatistics(UUID cardId, LocalDateTime from, LocalDateTime to);

    /**
     * Find suspicious transactions based on velocity and amount patterns
     */
    List<TransactionEntity> findSuspiciousTransactions(UUID cardId, int hours, BigDecimal amountThreshold);

    /**
     * Get daily transaction summary for a card
     */
    List<Map<String, Object>> getDailyTransactionSummary(UUID cardId, LocalDateTime from, LocalDateTime to);

    /**
     * Find duplicate transactions (same card, merchant, amount within timeframe)
     */
    List<TransactionEntity> findPotentialDuplicates(UUID cardId, LocalDateTime within);

    /**
     * Get merchant transaction volume
     */
    Map<String, Object> getMerchantTransactionVolume(UUID merchantId, LocalDateTime from, LocalDateTime to);

    /**
     * Find transactions by multiple criteria (dynamic query)
     */
    List<TransactionEntity> findByCriteria(
            UUID cardId,
            UUID merchantId,
            List<TransactionStatus> statuses,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount
    );
}
