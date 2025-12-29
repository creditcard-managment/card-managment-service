package com.unisono.card.lifecycle.infrastructure.adapter.input.rest.mapper;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.infrastructure.adapter.input.rest.dto.*;
import com.unisono.card.lifecycle.infrastructure.adapter.input.rest.dto.StateTransitionResponse;
import com.unisono.card.shared.domain.valueobject.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre modelos de dominio y DTOs REST
 *
 * Ubicación: infrastructure/adapter/input/rest/mapper
 * Razón: Mapea entre domain models y REST DTOs (responsabilidad de la capa de infraestructura)
 */
@Component
public class LifecycleDtoMapper {

    // =============================================
    // MAPPERS: Domain → Response DTOs
    // =============================================

    /**
     * Convierte StateTransition → StateTransitionResponse
     */
    public StateTransitionResponse toResponse(StateTransition transition) {
        if (transition == null) {
            return null;
        }

        return StateTransitionResponse.builder()
                .id(transition.getId())
                .cardId(transition.getCardId().getValue())
                .fromState(transition.getFromState())
                .fromStateDescription(getStateDescription(transition.getFromState()))
                .toState(transition.getToState())
                .toStateDescription(getStateDescription(transition.getToState()))
                .event(transition.getEvent())
                .eventDescription(getEventDescription(transition.getEvent()))
                .reason(transition.getReason())
                .performedBy(transition.getPerformedBy())
                .transitionedAt(transition.getTransitionedAt())
                .successful(transition.isSuccessful())
                .errorMessage(transition.getErrorMessage())
                .description(transition.getDescription())
                .build();
    }

    /**
     * Convierte StateTransition → LifecycleHistoryResponse
     */
    public LifecycleHistoryResponse toHistoryResponse(StateTransition transition) {
        if (transition == null) {
            return null;
        }

        return LifecycleHistoryResponse.builder()
                .id(transition.getId())
                .fromState(transition.getFromState())
                .fromStateDescription(getStateDescription(transition.getFromState()))
                .toState(transition.getToState())
                .toStateDescription(getStateDescription(transition.getToState()))
                .event(transition.getEvent())
                .eventDescription(getEventDescription(transition.getEvent()))
                .reason(transition.getReason())
                .performedBy(transition.getPerformedBy())
                .transitionedAt(transition.getTransitionedAt())
                .successful(transition.isSuccessful())
                .errorMessage(transition.getErrorMessage())
                .build();
    }

    /**
     * Convierte lista de StateTransition → lista de LifecycleHistoryResponse
     */
    public List<LifecycleHistoryResponse> toHistoryResponseList(List<StateTransition> transitions) {
        if (transitions == null) {
            return List.of();
        }

        return transitions.stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    // =============================================
    // MAPPERS: Request DTOs → Domain Commands
    // =============================================

    /**
     * Convierte IssueCardRequest → Value Objects
     */
    public IssueCardCommand toIssueCardCommand(String cardId, IssueCardRequest request) {
        return new IssueCardCommand(
                CardId.of(cardId),
                CardNumber.of(request.getCardNumber()),
                CVV.of(request.getCvv()),
                PIN.of(request.getPin()), // Se validará y encriptará después
                ExpirationDate.of(request.getExpirationMonth(), request.getExpirationYear()),
                request.getReason(),
                request.getIssuedBy()
        );
    }

    /**
     * Record helper para IssueCard command
     */
    public record IssueCardCommand(
            CardId cardId,
            CardNumber cardNumber,
            CVV cvv,
            PIN pin,
            ExpirationDate expirationDate,
            String reason,
            String issuedBy
    ) {}

    // =============================================
    // HELPERS PRIVADOS
    // =============================================

    /**
     * Convierte String de estado a descripción legible
     *
     * @param state String del estado (ej: "ACTIVE", "APPROVED", null)
     * @return Descripción legible (ej: "Activa", "Aprobada", null)
     */
    private String getStateDescription(String state) {
        if (state == null) {
            return null;
        }

        try {
            return CardStatus.valueOf(state).getDescription();
        } catch (IllegalArgumentException e) {
            // Si no se puede convertir a enum, retornar el String original
            return state;
        }
    }

    /**
     * Convierte String de evento a descripción legible
     *
     * @param event String del evento (ej: "APPROVE", "ACTIVATE", null)
     * @return Descripción legible (ej: "Aprobar", "Activar", null)
     */
    private String getEventDescription(String event) {
        if (event == null) {
            return null;
        }

        try {
            return CardLifecycleEvent.valueOf(event).getDescription();
        } catch (IllegalArgumentException e) {
            // Si no se puede convertir a enum, retornar el String original
            return event;
        }
    }
}


