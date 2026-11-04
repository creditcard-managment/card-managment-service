package com.unisono.card.lifecycle.application.service;

import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.port.output.StateTransitionRepository;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Consultas del historial de transiciones. Las transiciones se ejecutan en los servicios
 * de cada caso de uso (Approve/Reject/Issue/Activate/Block/CancelCardService).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardLifecycleService {

    private final StateTransitionRepository transitionRepository;

    // =============================================
    // CONSULTAS
    // =============================================

    /**
     * Obtiene el historial completo de transiciones de una tarjeta
     */
    @Transactional(readOnly = true)
    public List<StateTransition> getHistory(CardId cardId) {
        log.info("Obteniendo historial de tarjeta: {}", cardId.getValue());
        return transitionRepository.findByCardId(cardId);
    }

    /**
     * Obtiene solo las transiciones exitosas
     */
    @Transactional(readOnly = true)
    public List<StateTransition> getSuccessfulHistory(CardId cardId) {
        log.info("Obteniendo historial exitoso de tarjeta: {}", cardId.getValue());
        return transitionRepository.findSuccessfulByCardId(cardId);
    }

    /**
     * Obtiene solo las transiciones fallidas
     */
    @Transactional(readOnly = true)
    public List<StateTransition> getFailedHistory(CardId cardId) {
        log.info("Obteniendo historial fallido de tarjeta: {}", cardId.getValue());
        return transitionRepository.findFailedByCardId(cardId);
    }

    /**
     * Obtiene la última transición
     */
    @Transactional(readOnly = true)
    public Optional<StateTransition> getLastTransition(CardId cardId) {
        log.info("Obteniendo última transición de tarjeta: {}", cardId.getValue());
        return transitionRepository.findLastTransition(cardId);
    }
}
