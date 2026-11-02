package com.unisono.card.payment.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.TransactionItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for TransactionItemEntity
 */
@Repository
public interface JpaTransactionItemRepository extends JpaRepository<TransactionItemEntity, UUID> {

    /**
     * Find all items for a specific transaction
     */
    List<TransactionItemEntity> findByTransaction_TransactionId(UUID transactionId);

    /**
     * Count items in a transaction
     */
    Long countByTransaction_TransactionId(UUID transactionId);

    /**
     * Delete all items for a transaction
     */
    void deleteByTransaction_TransactionId(UUID transactionId);

    /**
     * Find items with description containing keyword
     */
    @Query("SELECT i FROM TransactionItemEntity i " +
            "WHERE LOWER(i.itemDescription) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<TransactionItemEntity> findByItemDescriptionContaining(@Param("keyword") String keyword);
}
