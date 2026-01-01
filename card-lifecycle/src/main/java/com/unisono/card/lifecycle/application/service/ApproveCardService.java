package com.unisono.card.lifecycle.application.service;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.port.input.ApproveCardUseCase;
import com.unisono.card.lifecycle.domain.port.output.StateTransitionRepository;
import com.unisono.card.lifecycle.infrastructure.adapter.output.card.CardDataRestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApproveCardService implements ApproveCardUseCase {

    private final CardDataRestClient cardAdapter; // ✅ Usar CardAdapter
    private final StateTransitionRepository stateTransitionRepository;

    @Override
    @Transactional
    public StateTransition execute(ApproveCardCommand command) {
        log.info("Ejecutando aprobación de tarjeta: {}", command.cardId().getValue());

        // 1. Cambiar estado a APPROVED vía REST a card-data
        cardAdapter.updateStatus(command.cardId(), "APPROVED");

        // 2. Registrar transición en card-lifecycle
        StateTransition transition = StateTransition.builder()
                .cardId(command.cardId())
                .fromState("REQUESTED")
                .toState("APPROVED")
                .event("APPROVE")
                .reason(command.reason())
                .performedBy(command.approvedBy())
                .transitionedAt(LocalDateTime.now())
                .successful(true)
                .build();

        StateTransition saved = stateTransitionRepository.save(transition);

        log.info("Tarjeta aprobada exitosamente: {}", command.cardId().getValue());

        return saved;
    }
}
