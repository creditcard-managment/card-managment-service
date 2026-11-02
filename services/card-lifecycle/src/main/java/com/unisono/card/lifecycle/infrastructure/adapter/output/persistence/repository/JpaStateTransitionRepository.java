package com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.entity.StateTransitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para StateTransitionEntity
 */
@Repository
public interface JpaStateTransitionRepository extends JpaRepository<StateTransitionEntity, Long> {

    /**
     * Busca todas las transiciones de una tarjeta ordenadas por fecha descendente
     */
    List<StateTransitionEntity> findByCardIdOrderByTransitionedAtDesc(String cardId);

    /**
     * Busca transiciones exitosas de una tarjeta
     */
    List<StateTransitionEntity> findByCardIdAndSuccessfulTrueOrderByTransitionedAtDesc(String cardId);

    /**
     * Busca transiciones fallidas de una tarjeta
     */
    List<StateTransitionEntity> findByCardIdAndSuccessfulFalseOrderByTransitionedAtDesc(String cardId);

    /**
     * Busca la última transición de una tarjeta
     */
    Optional<StateTransitionEntity> findFirstByCardIdOrderByTransitionedAtDesc(String cardId);

    /**
     * Cuenta transiciones de una tarjeta
     */
    long countByCardId(String cardId);

    /**
     * Verifica si existen transiciones para una tarjeta
     */
    boolean existsByCardId(String cardId);

    /**
     * Busca transiciones por evento
     */
    List<StateTransitionEntity> findByEvent(String event);

    /**
     * Busca transiciones por estado destino
     */
    List<StateTransitionEntity> findByToState(String toState);
}
