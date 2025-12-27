package com.unisono.card.data.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.data.domain.model.CardStatus;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository para CardEntity
 */
@Repository
public interface JpaCardRepository extends JpaRepository<CardEntity, String> {

    /**
     * Busca todas las tarjetas de un cliente (excluye eliminadas)
     */
    @Query("SELECT c FROM CardEntity c WHERE c.clientId = :clientId AND c.deleted = false")
    List<CardEntity> findByClientId(@Param("clientId") String clientId);

    /**
     * Busca una tarjeta por los últimos 4 dígitos y cliente
     */
    @Query("SELECT c FROM CardEntity c WHERE c.lastFourDigits = :lastFourDigits " +
            "AND c.clientId = :clientId AND c.deleted = false")
    Optional<CardEntity> findByLastFourDigitsAndClientId(
            @Param("lastFourDigits") String lastFourDigits,
            @Param("clientId") String clientId
    );

    /**
     * Busca una tarjeta por número encriptado
     */
    @Query("SELECT c FROM CardEntity c WHERE c.cardNumberEncrypted = :encryptedNumber " +
            "AND c.deleted = false")
    Optional<CardEntity> findByCardNumberEncrypted(@Param("encryptedNumber") String encryptedNumber);

    /**
     * Busca tarjetas por estado
     */
    @Query("SELECT c FROM CardEntity c WHERE c.status = :status AND c.deleted = false")
    List<CardEntity> findByStatus(@Param("status") CardStatus status);

    /**
     * Busca tarjetas que requieren renovación (vencen en los próximos 30 días)
     */
    @Query("SELECT c FROM CardEntity c WHERE c.status = 'ACTIVE' " +
            "AND c.deleted = false " +
            "AND (c.expirationYear * 12 + c.expirationMonth) <= " +
            "(YEAR(CURRENT_DATE) * 12 + MONTH(CURRENT_DATE) + 1)")
    List<CardEntity> findCardsRequiringRenewal();

    /**
     * Override findById para excluir eliminadas
     */
    @Query("SELECT c FROM CardEntity c WHERE c.id = :id AND c.deleted = false")
    Optional<CardEntity> findById(@Param("id") String id);
}
