package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.BlockCardUseCase;
import com.unisono.card.data.domain.port.input.UnblockCardUseCase;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de seguridad de tarjetas
 * Implementa los use cases de bloqueo/desbloqueo
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CardSecurityService implements BlockCardUseCase, UnblockCardUseCase {

    private final CardRepository cardRepository;

    // ========================================
    // BlockCardUseCase
    // ========================================

    @Override
    public Card execute(BlockCardCommand command) {
        log.info("Blocking card: {} - Reason: {}", command.cardId(), command.reason());

        Card card = getCardOrThrow(command.cardId());
        card.block(command.reason());

        Card savedCard = cardRepository.save(card);

        log.info("Card blocked successfully: {}", command.cardId());

        return savedCard;
    }

    @Override
    public Card executeForFraud(BlockCardCommand command) {
        log.warn("Blocking card for FRAUD: {} - Reason: {}", command.cardId(), command.reason());

        Card card = getCardOrThrow(command.cardId());
        card.blockForFraud(command.reason());

        Card savedCard = cardRepository.save(card);

        log.warn("Card blocked for FRAUD: {}", command.cardId());

        return savedCard;
    }

    // ========================================
    // UnblockCardUseCase
    // ========================================

    @Override
    public Card execute(CardId cardId) {
        log.info("Unblocking card: {}", cardId);

        Card card = getCardOrThrow(cardId);
        card.unblock();

        Card savedCard = cardRepository.save(card);

        log.info("Card unblocked successfully: {}", cardId);

        return savedCard;
    }

    // ========================================
    // Helper methods
    // ========================================

    private Card getCardOrThrow(CardId cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card", cardId.toString()));
    }
}
