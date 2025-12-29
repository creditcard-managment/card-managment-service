package com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.mapper;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.entity.StateTransitionEntity;
import com.unisono.card.shared.domain.valueobject.CardId;
import org.springframework.stereotype.Component;

/**
 * Mapper entre StateTransition (domain) y StateTransitionEntity (persistence)
 */
@Component
public class StateTransitionEntityMapper {

    /**
     * Convierte Entity → Domain Model
     */
    public StateTransition toDomain(StateTransitionEntity entity) {
        if (entity == null) {
            return null;
        }

        return StateTransition.builder()
                .id(entity.getId())
                .cardId(CardId.of(entity.getCardId()))
                .fromState(entity.getFromState())
                .toState(entity.getToState())
                .event(entity.getEvent())
                .reason(entity.getReason())
                .performedBy(entity.getPerformedBy())
                .transitionedAt(entity.getTransitionedAt())
                .successful(entity.getSuccessful())
                .errorMessage(entity.getErrorMessage())
                .build();
    }

    /**
     * Convierte Domain Model → Entity
     */
    public StateTransitionEntity toEntity(StateTransition domain) {
        if (domain == null) {
            return null;
        }

        return StateTransitionEntity.builder()
                .id(domain.getId())
                .cardId(domain.getCardId().getValue())
                .fromState(domain.getFromState())
                .toState(domain.getToState())
                .event(domain.getEvent())
                .reason(domain.getReason())
                .performedBy(domain.getPerformedBy())
                .transitionedAt(domain.getTransitionedAt())
                .successful(domain.getSuccessful())
                .errorMessage(domain.getErrorMessage())
                .build();
    }
}
