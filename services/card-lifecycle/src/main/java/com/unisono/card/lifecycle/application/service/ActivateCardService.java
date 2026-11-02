package com.unisono.card.lifecycle.application.service;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.port.input.ActivateCardUseCase;
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
public class ActivateCardService implements ActivateCardUseCase {

    private final CardDataRestClient cardAdapter; // ✅ Usar CardAdapter
    private final StateTransitionRepository stateTransitionRepository;

    @Override
    @Transactional
    public StateTransition execute(ActivateCardCommand command) {
        log.info("Ejecutando activación de tarjeta: {}", command.cardId().getValue());

        // 1. Cambiar estado a ACTIVE vía REST a card-data
        cardAdapter.updateStatus(command.cardId(), "ACTIVE");

        // 2. Actualizar fecha de activación vía REST a card-data
        cardAdapter.updateActivatedAt(command.cardId(), LocalDateTime.now());

        // 3. Registrar transición en card-lifecycle
        StateTransition transition = StateTransition.builder()
                .cardId(command.cardId())
                .fromState("ISSUED")
                .toState("ACTIVE")
                .event("ACTIVATE")
                .reason(command.reason())
                .performedBy(command.activatedBy())
                .transitionedAt(LocalDateTime.now())
                .successful(true)
                .build();

        StateTransition saved = stateTransitionRepository.save(transition);

        log.info("Tarjeta activada exitosamente: {}", command.cardId().getValue());

        return saved;
    }
}
