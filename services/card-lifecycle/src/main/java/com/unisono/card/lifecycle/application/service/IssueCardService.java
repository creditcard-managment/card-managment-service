package com.unisono.card.lifecycle.application.service;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.port.input.IssueCardUseCase;

import com.unisono.card.lifecycle.domain.port.output.StateTransitionRepository;
import com.unisono.card.lifecycle.infrastructure.adapter.output.card.CardAdapter;
import com.unisono.card.lifecycle.infrastructure.adapter.output.card.CardDataRestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementación del caso de uso de emisión de tarjeta
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueCardService implements IssueCardUseCase {

    private final CardDataRestClient cardAdapter;
    private final StateTransitionRepository stateTransitionRepository;

    @Override
    @Transactional
    public StateTransition execute(IssueCardCommand command) {
        log.info("Ejecutando emisión de tarjeta: {}", command.cardId().getValue());

        // 1. Actualizar datos de la tarjeta vía REST a card-data
        cardAdapter.updateCardData(
                command.cardId(),
                command.cardNumber(),
                command.cvv(),
                command.pin(),
                command.expirationDate()
        );

        // 2. Cambiar estado a ISSUED vía REST a card-data
        cardAdapter.updateStatus(command.cardId(), "ISSUED");

        // 3. Registrar transición en card-lifecycle
        StateTransition transition = StateTransition.builder()
                .cardId(command.cardId())
                .fromState("APPROVED")
                .toState("ISSUED")
                .event("ISSUE")
                .reason(command.reason())
                .performedBy(command.issuedBy())
                .transitionedAt(LocalDateTime.now())
                .successful(true)
                .build();

        StateTransition saved = stateTransitionRepository.save(transition);

        log.info("Tarjeta emitida exitosamente: {}", command.cardId().getValue());

        return saved;
    }
}
