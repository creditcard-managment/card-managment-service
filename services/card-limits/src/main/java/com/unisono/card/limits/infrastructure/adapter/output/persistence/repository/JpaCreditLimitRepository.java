package com.unisono.card.limits.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.limits.infrastructure.adapter.output.persistence.entity.CreditLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA Repository para CreditLimitEntity
 */
@Repository
public interface JpaCreditLimitRepository extends JpaRepository<CreditLimitEntity, String> {

    /**
     * Busca el límite de crédito de una tarjeta (excluye eliminados)
     */
    @Query("SELECT c FROM CreditLimitEntity c WHERE c.cardId = :cardId AND c.deleted = false")
    Optional<CreditLimitEntity> findByCardId(@Param("cardId") String cardId);

    /**
     * Verifica si existe un límite para una tarjeta
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM CreditLimitEntity c WHERE c.cardId = :cardId AND c.deleted = false")
    boolean existsByCardId(@Param("cardId") String cardId);

    /**
     * Override findById para excluir eliminados
     */
    @Query("SELECT c FROM CreditLimitEntity c WHERE c.id = :id AND c.deleted = false")
    Optional<CreditLimitEntity> findById(@Param("id") String id);
}
