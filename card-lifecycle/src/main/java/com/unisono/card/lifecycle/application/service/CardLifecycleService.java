package com.unisono.card.lifecycle.application.service;

import com.unisono.card.data.domain.model.*;
import com.unisono.card.lifecycle.domain.model.CardLifecycle;
import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.port.output.CardPort;
import com.unisono.card.lifecycle.domain.port.output.StateTransitionRepository;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CVV;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardNumber;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardLifecycleService {

    private final StateTransitionRepository transitionRepository;
    private final CardPort cardPort;

    // =============================================
    // COMANDOS DE TRANSICIÓN DE ESTADO
    // =============================================

    /**
     * Aprueba una tarjeta (REQUESTED → APPROVED)
     */
    @Transactional
    public void approveCard(CardId cardId, String reason, String approvedBy) {
        log.info("Aprobando tarjeta: {} - Razón: {} - Por: {}", cardId.getValue(), reason, approvedBy);

        Card card = findCardOrThrow(cardId);
        String previousState = card.getStatus().name();

        try {
            // ✅ Usar CardLifecycle
            CardLifecycle lifecycle = new CardLifecycle(card);
            lifecycle.approve();

            cardPort.save(card);

            StateTransition transition = StateTransition.createSuccessful(
                    cardId, previousState, card.getStatus().name(), "APPROVE", reason, approvedBy
            );
            transitionRepository.save(transition);

            log.info("Tarjeta aprobada exitosamente: {}", cardId.getValue());

        } catch (Exception e) {
            log.error("Error aprobando tarjeta: {}", cardId.getValue(), e);

            StateTransition failedTransition = StateTransition.createFailed(
                    cardId, previousState, "APPROVE", e.getMessage(), approvedBy
            );
            transitionRepository.save(failedTransition);

            throw e;
        }
    }

    /**
     * Rechaza una tarjeta (REQUESTED → REJECTED)
     */
    @Transactional
    public void rejectCard(CardId cardId, String reason, String rejectedBy) {
        log.info("Rechazando tarjeta: {} - Razón: {} - Por: {}", cardId.getValue(), reason, rejectedBy);

        Card card = findCardOrThrow(cardId);
        String previousState = card.getStatus().name();

        try {
            // ✅ Usar CardLifecycle
            CardLifecycle lifecycle = new CardLifecycle(card);
            lifecycle.reject();

            cardPort.save(card);

            StateTransition transition = StateTransition.createSuccessful(
                    cardId, previousState, card.getStatus().name(), "REJECT", reason, rejectedBy
            );
            transitionRepository.save(transition);

            log.info("Tarjeta rechazada: {}", cardId.getValue());

        } catch (Exception e) {
            log.error("Error rechazando tarjeta: {}", cardId.getValue(), e);

            StateTransition failedTransition = StateTransition.createFailed(
                    cardId, previousState, "REJECT", e.getMessage(), rejectedBy
            );
            transitionRepository.save(failedTransition);

            throw e;
        }
    }

    /**
     * Emite una tarjeta (APPROVED → ISSUED)
     */
    @Transactional
    public void issueCard(
            CardId cardId,
            CardNumber cardNumber,
            CVV cvv,
            String pinHash,
            ExpirationDate expirationDate,
            String reason,
            String issuedBy
    ) {
        log.info("Emitiendo tarjeta: {} - Razón: {} - Por: {}", cardId.getValue(), reason, issuedBy);

        Card card = findCardOrThrow(cardId);
        String previousState = card.getStatus().name();

        try {
            // ✅ Usar CardLifecycle
            CardLifecycle lifecycle = new CardLifecycle(card);
            lifecycle.issue(cardNumber, cvv, pinHash, expirationDate);

            cardPort.save(card);

            StateTransition transition = StateTransition.createSuccessful(
                    cardId, previousState, card.getStatus().name(), "ISSUE", reason, issuedBy
            );
            transitionRepository.save(transition);

            log.info("Tarjeta emitida: {} - Últimos 4 dígitos: {}",
                    cardId.getValue(), card.getCardNumber().getLastFourDigits());

        } catch (Exception e) {
            log.error("Error emitiendo tarjeta: {}", cardId.getValue(), e);

            StateTransition failedTransition = StateTransition.createFailed(
                    cardId, previousState, "ISSUE", e.getMessage(), issuedBy
            );
            transitionRepository.save(failedTransition);

            throw e;
        }
    }

    /**
     * Activa una tarjeta (ISSUED → ACTIVE)
     */
    @Transactional
    public void activateCard(CardId cardId, String reason, String activatedBy) {
        log.info("Activando tarjeta: {} - Razón: {} - Por: {}", cardId.getValue(), reason, activatedBy);

        Card card = findCardOrThrow(cardId);
        String previousState = card.getStatus().name();

        try {
            // ✅ Usar CardLifecycle
            CardLifecycle lifecycle = new CardLifecycle(card);
            lifecycle.activate();

            cardPort.save(card);

            StateTransition transition = StateTransition.createSuccessful(
                    cardId, previousState, card.getStatus().name(), "ACTIVATE", reason, activatedBy
            );
            transitionRepository.save(transition);

            log.info("Tarjeta activada: {}", cardId.getValue());

        } catch (Exception e) {
            log.error("Error activando tarjeta: {}", cardId.getValue(), e);

            StateTransition failedTransition = StateTransition.createFailed(
                    cardId, previousState, "ACTIVATE", e.getMessage(), activatedBy
            );
            transitionRepository.save(failedTransition);

            throw e;
        }
    }

    /**
     * Bloquea una tarjeta (ACTIVE → BLOCKED)
     */
    @Transactional
    public void blockCard(CardId cardId, String reason, String blockedBy) {
        log.info("Bloqueando tarjeta: {} - Razón: {} - Por: {}", cardId.getValue(), reason, blockedBy);

        Card card = findCardOrThrow(cardId);
        String previousState = card.getStatus().name();

        try {
            // ✅ Usar CardLifecycle
            CardLifecycle lifecycle = new CardLifecycle(card);
            lifecycle.block();

            cardPort.save(card);

            StateTransition transition = StateTransition.createSuccessful(
                    cardId, previousState, card.getStatus().name(), "BLOCK", reason, blockedBy
            );
            transitionRepository.save(transition);

            log.info("Tarjeta bloqueada: {}", cardId.getValue());

        } catch (Exception e) {
            log.error("Error bloqueando tarjeta: {}", cardId.getValue(), e);

            StateTransition failedTransition = StateTransition.createFailed(
                    cardId, previousState, "BLOCK", e.getMessage(), blockedBy
            );
            transitionRepository.save(failedTransition);

            throw e;
        }
    }

    /**
     * Bloquea una tarjeta por fraude (ACTIVE → BLOCKED_FRAUD)
     */
    @Transactional
    public void blockCardForFraud(CardId cardId, String reason, String blockedBy) {
        log.info("Bloqueando tarjeta por fraude: {} - Razón: {} - Por: {}", cardId.getValue(), reason, blockedBy);

        Card card = findCardOrThrow(cardId);
        String previousState = card.getStatus().name();

        try {
            // ✅ Usar CardLifecycle
            CardLifecycle lifecycle = new CardLifecycle(card);
            lifecycle.blockForFraud();

            cardPort.save(card);

            StateTransition transition = StateTransition.createSuccessful(
                    cardId, previousState, card.getStatus().name(), "BLOCK_FRAUD", reason, blockedBy
            );
            transitionRepository.save(transition);

            log.info("Tarjeta bloqueada por fraude: {}", cardId.getValue());

        } catch (Exception e) {
            log.error("Error bloqueando tarjeta por fraude: {}", cardId.getValue(), e);

            StateTransition failedTransition = StateTransition.createFailed(
                    cardId, previousState, "BLOCK_FRAUD", e.getMessage(), blockedBy
            );
            transitionRepository.save(failedTransition);

            throw e;
        }
    }

    /**
     * Cancela una tarjeta
     */
    @Transactional
    public void cancelCard(CardId cardId, String reason, String cancelledBy) {
        log.info("Cancelando tarjeta: {} - Razón: {} - Por: {}", cardId.getValue(), reason, cancelledBy);

        Card card = findCardOrThrow(cardId);
        String previousState = card.getStatus().name();

        try {
            // ✅ Usar CardLifecycle
            CardLifecycle lifecycle = new CardLifecycle(card);
            lifecycle.cancel();

            cardPort.save(card);

            StateTransition transition = StateTransition.createSuccessful(
                    cardId, previousState, card.getStatus().name(), "CANCEL", reason, cancelledBy
            );
            transitionRepository.save(transition);

            log.info("Tarjeta cancelada: {}", cardId.getValue());

        } catch (Exception e) {
            log.error("Error cancelando tarjeta: {}", cardId.getValue(), e);

            StateTransition failedTransition = StateTransition.createFailed(
                    cardId, previousState, "CANCEL", e.getMessage(), cancelledBy
            );
            transitionRepository.save(failedTransition);

            throw e;
        }
    }

    // =============================================
    // CONSULTAS
    // =============================================

    @Transactional(readOnly = true)
    public List<StateTransition> getHistory(CardId cardId) {
        log.info("Obteniendo historial de tarjeta: {}", cardId.getValue());
        return transitionRepository.findByCardId(cardId);
    }

    @Transactional(readOnly = true)
    public List<StateTransition> getSuccessfulHistory(CardId cardId) {
        log.info("Obteniendo historial exitoso de tarjeta: {}", cardId.getValue());
        return transitionRepository.findSuccessfulByCardId(cardId);
    }

    @Transactional(readOnly = true)
    public List<StateTransition> getFailedHistory(CardId cardId) {
        log.info("Obteniendo historial fallido de tarjeta: {}", cardId.getValue());
        return transitionRepository.findFailedByCardId(cardId);
    }

    @Transactional(readOnly = true)
    public Optional<StateTransition> getLastTransition(CardId cardId) {
        log.info("Obteniendo última transición de tarjeta: {}", cardId.getValue());
        return transitionRepository.findLastTransition(cardId);
    }

    // =============================================
    // HELPERS PRIVADOS
    // =============================================

    private Card findCardOrThrow(CardId cardId) {
        return cardPort.findById(cardId)
                .orElseThrow(() -> new NotFoundException(
                        "Tarjeta no encontrada: " + cardId.getValue()
                ));
    }
}
