package com.unisono.card.payment.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.TransactionEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of custom transaction repository queries
 */
@Slf4j
@Repository
public class CustomTransactionRepositoryImpl implements CustomTransactionRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<String, Object> getTransactionStatistics(String cardId, LocalDateTime from, LocalDateTime to) {
        String query = """
            SELECT 
                COUNT(*) as totalCount,
                COALESCE(SUM(CASE WHEN transaction_status = 'APPROVED' THEN 1 ELSE 0 END), 0) as approvedCount,
                COALESCE(SUM(CASE WHEN transaction_status = 'DECLINED' THEN 1 ELSE 0 END), 0) as declinedCount,
                COALESCE(SUM(amount), 0) as totalAmount,
                COALESCE(AVG(amount), 0) as avgAmount,
                COALESCE(MAX(amount), 0) as maxAmount,
                COALESCE(MIN(amount), 0) as minAmount
            FROM transactions
            WHERE card_id = :cardId
            AND transaction_date BETWEEN :fromDate AND :toDate
            """;

        Object[] result = (Object[]) entityManager.createNativeQuery(query)
                .setParameter("cardId", cardId)
                .setParameter("fromDate", from)
                .setParameter("toDate", to)
                .getSingleResult();

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", ((Number) result[0]).longValue());
        statistics.put("approvedCount", ((Number) result[1]).longValue());
        statistics.put("declinedCount", ((Number) result[2]).longValue());
        statistics.put("totalAmount", (BigDecimal) result[3]);
        statistics.put("avgAmount", (BigDecimal) result[4]);
        statistics.put("maxAmount", (BigDecimal) result[5]);
        statistics.put("minAmount", (BigDecimal) result[6]);

        return statistics;
    }

    @Override
    public List<TransactionEntity> findSuspiciousTransactions(String cardId, int hours, BigDecimal amountThreshold) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(hours);

        String jpql = """
            SELECT t FROM TransactionEntity t
            WHERE t.cardId = :cardId
            AND t.transactionDate >= :threshold
            AND (t.amount >= :amountThreshold OR t.riskScore >= 70)
            ORDER BY t.transactionDate DESC
            """;

        return entityManager.createQuery(jpql, TransactionEntity.class)
                .setParameter("cardId", cardId)
                .setParameter("threshold", threshold)
                .setParameter("amountThreshold", amountThreshold)
                .getResultList();
    }

    @Override
    public List<Map<String, Object>> getDailyTransactionSummary(String cardId, LocalDateTime from, LocalDateTime to) {
        String query = """
            SELECT 
                DATE(transaction_date) as date,
                COUNT(*) as count,
                COALESCE(SUM(amount), 0) as totalAmount,
                COALESCE(AVG(amount), 0) as avgAmount
            FROM transactions
            WHERE card_id = :cardId
            AND transaction_date BETWEEN :fromDate AND :toDate
            GROUP BY DATE(transaction_date)
            ORDER BY date DESC
            """;

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(query)
                .setParameter("cardId", cardId)
                .setParameter("fromDate", from)
                .setParameter("toDate", to)
                .getResultList();

        List<Map<String, Object>> summary = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", row[0]);
            day.put("count", ((Number) row[1]).longValue());
            day.put("totalAmount", (BigDecimal) row[2]);
            day.put("avgAmount", (BigDecimal) row[3]);
            summary.add(day);
        }

        return summary;
    }

    @Override
    public List<TransactionEntity> findPotentialDuplicates(String cardId, LocalDateTime within) {
        String jpql = """
            SELECT t1 FROM TransactionEntity t1
            WHERE EXISTS (
                SELECT t2 FROM TransactionEntity t2
                WHERE t2.cardId = t1.cardId
                AND t2.merchantId = t1.merchantId
                AND t2.amount = t1.amount
                AND t2.transactionId != t1.transactionId
                AND t2.transactionDate BETWEEN t1.transactionDate AND :within
            )
            AND t1.cardId = :cardId
            ORDER BY t1.transactionDate DESC
            """;

        return entityManager.createQuery(jpql, TransactionEntity.class)
                .setParameter("cardId", cardId)
                .setParameter("within", within)
                .getResultList();
    }

    @Override
    public Map<String, Object> getMerchantTransactionVolume(String merchantId, LocalDateTime from, LocalDateTime to) {
        String query = """
            SELECT 
                COUNT(*) as totalTransactions,
                COALESCE(SUM(amount), 0) as totalVolume,
                COALESCE(AVG(amount), 0) as avgTransactionAmount,
                COUNT(DISTINCT card_id) as uniqueCards
            FROM transactions
            WHERE merchant_id = :merchantId
            AND transaction_date BETWEEN :fromDate AND :toDate
            AND transaction_status IN ('APPROVED', 'SETTLED')
            """;

        Object[] result = (Object[]) entityManager.createNativeQuery(query)
                .setParameter("merchantId", merchantId)
                .setParameter("fromDate", from)
                .setParameter("toDate", to)
                .getSingleResult();

        Map<String, Object> volume = new HashMap<>();
        volume.put("totalTransactions", ((Number) result[0]).longValue());
        volume.put("totalVolume", (BigDecimal) result[1]);
        volume.put("avgTransactionAmount", (BigDecimal) result[2]);
        volume.put("uniqueCards", ((Number) result[3]).longValue());

        return volume;
    }

    @Override
    public List<TransactionEntity> findByCriteria(
            String cardId,
            String merchantId,
            List<TransactionStatus> statuses,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TransactionEntity> cq = cb.createQuery(TransactionEntity.class);
        Root<TransactionEntity> transaction = cq.from(TransactionEntity.class);

        List<Predicate> predicates = new ArrayList<>();

        if (cardId != null) {
            predicates.add(cb.equal(transaction.get("cardId"), cardId));
        }

        if (merchantId != null) {
            predicates.add(cb.equal(transaction.get("merchantId"), merchantId));
        }

        if (statuses != null && !statuses.isEmpty()) {
            predicates.add(transaction.get("transactionStatus").in(statuses));
        }

        if (fromDate != null && toDate != null) {
            predicates.add(cb.between(transaction.get("transactionDate"), fromDate, toDate));
        }

        if (minAmount != null) {
            predicates.add(cb.greaterThanOrEqualTo(transaction.get("amount"), minAmount));
        }

        if (maxAmount != null) {
            predicates.add(cb.lessThanOrEqualTo(transaction.get("amount"), maxAmount));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(transaction.get("transactionDate")));

        TypedQuery<TransactionEntity> query = entityManager.createQuery(cq);
        return query.getResultList();
    }
}
