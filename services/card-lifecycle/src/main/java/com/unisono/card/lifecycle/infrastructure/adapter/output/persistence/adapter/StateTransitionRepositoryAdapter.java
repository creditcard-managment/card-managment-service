package com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.adapter;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.port.output.StateTransitionRepository;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.entity.StateTransitionEntity;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.mapper.StateTransitionEntityMapper;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.repository.JpaStateTransitionRepository;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adapter que implementa StateTransitionRepository usando JPA
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true) // Por defecto solo lectura
public class StateTransitionRepositoryAdapter implements StateTransitionRepository {

    private final JpaStateTransitionRepository jpaRepository;
    private final StateTransitionEntityMapper mapper;

    @Override
    @Transactional // Write operation
    public StateTransition save(StateTransition transition) {
        log.debug("Guardando transición de estado para tarjeta: {}", transition.getCardId().getValue());

        StateTransitionEntity entity = mapper.toEntity(transition);
        StateTransitionEntity saved = jpaRepository.save(entity);

        log.debug("Transición guardada con ID: {}", saved.getId());
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<StateTransition> findById(Long id) {
        log.debug("Buscando transición por ID: {}", id);

        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<StateTransition> findByCardId(CardId cardId) {
        log.debug("Buscando transiciones para tarjeta: {}", cardId.getValue());

        return jpaRepository.findByCardIdOrderByTransitionedAtDesc(cardId.getValue())
                .stream()
                .map(mapper::toDomain)
                .toList(); // ✅ Java 16+ (en lugar de .collect(Collectors.toList()))
    }

    @Override
    public List<StateTransition> findSuccessfulByCardId(CardId cardId) {
        log.debug("Buscando transiciones exitosas para tarjeta: {}", cardId.getValue());

        return jpaRepository.findByCardIdAndSuccessfulTrueOrderByTransitionedAtDesc(cardId.getValue())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<StateTransition> findFailedByCardId(CardId cardId) {
        log.debug("Buscando transiciones fallidas para tarjeta: {}", cardId.getValue());

        return jpaRepository.findByCardIdAndSuccessfulFalseOrderByTransitionedAtDesc(cardId.getValue())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<StateTransition> findLastTransition(CardId cardId) {
        log.debug("Buscando última transición para tarjeta: {}", cardId.getValue());

        return jpaRepository.findFirstByCardIdOrderByTransitionedAtDesc(cardId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public long countByCardId(CardId cardId) {
        log.debug("Contando transiciones para tarjeta: {}", cardId.getValue());

        return jpaRepository.countByCardId(cardId.getValue());
    }

    @Override
    public boolean existsByCardId(CardId cardId) {
        log.debug("Verificando si existen transiciones para tarjeta: {}", cardId.getValue());

        return jpaRepository.existsByCardId(cardId.getValue());
    }
}
