package com.unisono.card.limits.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.limits.domain.model.LimitType;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.entity.CreditLimitHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository para CreditLimitHistoryEntity
 */
@Repository
public interface JpaCreditLimitHistoryRepository extends JpaRepository<CreditLimitHistoryEntity, Long> {

    /**
     * Busca todo el historial de un límite (ordenado por fecha)
     */
    @Query("SELECT h FROM CreditLimitHistoryEntity h " +
            "WHERE h.creditLimitId = :creditLimitId " +
            "ORDER BY h.changedAt DESC")
    List<CreditLimitHistoryEntity> findByCreditLimitIdOrderByChangedAtDesc(
            @Param("creditLimitId") String creditLimitId
    );

    /**
     * Busca historial filtrado por tipo de límite
     */
    @Query("SELECT h FROM CreditLimitHistoryEntity h " +
            "WHERE h.creditLimitId = :creditLimitId " +
            "AND h.limitType = :limitType " +
            "ORDER BY h.changedAt DESC")
    List<CreditLimitHistoryEntity> findByCreditLimitIdAndLimitType(
            @Param("creditLimitId") String creditLimitId,
            @Param("limitType") LimitType limitType
    );

    /**
     * Busca historial en un rango de fechas
     */
    @Query("SELECT h FROM CreditLimitHistoryEntity h " +
            "WHERE h.creditLimitId = :creditLimitId " +
            "AND h.changedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY h.changedAt DESC")
    List<CreditLimitHistoryEntity> findByCreditLimitIdAndDateRange(
            @Param("creditLimitId") String creditLimitId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Obtiene el último cambio
     */
    @Query("SELECT h FROM CreditLimitHistoryEntity h " +
            "WHERE h.creditLimitId = :creditLimitId " +
            "ORDER BY h.changedAt DESC " +
            "LIMIT 1")
    Optional<CreditLimitHistoryEntity> findLastChange(@Param("creditLimitId") String creditLimitId);
}
