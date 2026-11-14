package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.BlockCardForFraudDeclineUseCase;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.data.domain.port.output.ProcessedEventPort;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Bloquea por fraude la tarjeta de un pago rechazado por fraude (responseCode 59 de payment-processing).
 *
 * Todo pasa en una transacción: el registro del eventId, el bloqueo y el card.blocked en el outbox. Un evento
 * repetido no hace nada, así una tarjeta que un operador desbloqueó no se vuelve a bloquear por el mismo rechazo.
 * Solo se bloquean las tarjetas que pueden bloquearse (ACTIVE o ISSUED); en cualquier otro estado el evento se
 * registra y se ignora.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDeclineService implements BlockCardForFraudDeclineUseCase {

    private final CardRepository cardRepository;
    private final ProcessedEventPort processedEvents;
    private final CardEventEmitter eventEmitter;

    @Override
    @Transactional
    public Outcome execute(FraudDeclineCommand command) {
        if (!processedEvents.markProcessed(command.eventId(), KafkaTopics.PAYMENT_DECLINED)) {
            log.info("🔁 Fraud decline {} for card {} already processed, skipping", command.eventId(), command.cardId());
            return Outcome.DUPLICATE;
        }

        Optional<Card> found = cardRepository.findById(command.cardId());
        if (found.isEmpty()) {
            log.warn("Fraud decline {} for unknown card {}: nothing to block", command.eventId(), command.cardId());
            return Outcome.CARD_NOT_FOUND;
        }

        Card card = found.get();
        CardStatus previousStatus = card.getStatus();
        if (!previousStatus.canBlock()) {
            log.warn("Fraud decline {} for card {} in status {}: not blocked", command.eventId(), command.cardId(),
                    previousStatus);
            return Outcome.NOT_BLOCKABLE;
        }

        String reason = reasonFor(command);
        card.blockForFraud(reason);
        Card saved = cardRepository.save(card);
        eventEmitter.statusChanged(saved, previousStatus, reason, CardEventEmitter.SYSTEM_ACTOR);

        log.warn("🚨 Card {} blocked for FRAUD after declined transaction {} ({})", command.cardId(),
                command.transactionId(), command.declineReason());
        return Outcome.BLOCKED;
    }

    private static String reasonFor(FraudDeclineCommand command) {
        String detail = command.declineReason() != null && !command.declineReason().isBlank()
                ? ": " + command.declineReason() : "";
        String reason = "Bloqueo automático por pago rechazado por fraude (transacción " + command.transactionId()
                + ")" + detail;
        // block_reason es varchar(500)
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}
