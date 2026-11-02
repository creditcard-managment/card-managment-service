package com.unisono.card.lifecycle.infrastructure.adapter.output.creditlimit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditLimitJpaRepository extends JpaRepository<CreditLimitJpaEntity, String> {

    /**
     * Busca límite de crédito por ID de tarjeta
     */
    Optional<CreditLimitJpaEntity> findByCardId(String cardId);

    /**
     * Verifica si existe límite para una tarjeta
     */
    boolean existsByCardId(String cardId);
}

