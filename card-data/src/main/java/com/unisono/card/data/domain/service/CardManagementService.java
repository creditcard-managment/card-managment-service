package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.CreateCardUseCase;
import com.unisono.card.data.domain.port.input.GetCardUseCase;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.ClientId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de gestión de tarjetas
 * Implementa los use cases de creación y consulta
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CardManagementService implements CreateCardUseCase, GetCardUseCase {

    private final CardRepository cardRepository;

    // ========================================
    // CreateCardUseCase
    // ========================================

    @Override
    public Card execute(CreateCardCommand command) {
        log.info("Creating new card for client: {}", command.clientId());

        // Crear la tarjeta (estado REQUESTED)
        Card card = Card.create(
                command.clientId(),
                command.holder(),
                command.type(),
                command.category()
        );

        // Guardar en BD
        Card savedCard = cardRepository.save(card);

        log.info("Card created successfully with ID: {}", savedCard.getId());

        return savedCard;
    }

    // ========================================
    // GetCardUseCase
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public Optional<Card> findById(CardId cardId) {
        log.debug("Finding card by ID: {}", cardId);
        return cardRepository.findById(cardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Card> findByClientId(ClientId clientId) {
        log.debug("Finding cards for client: {}", clientId);
        return cardRepository.findByClientId(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Card> findByLastFourDigits(String lastFourDigits, ClientId clientId) {
        log.debug("Finding card by last 4 digits: {} for client: {}", lastFourDigits, clientId);
        return cardRepository.findByLastFourDigitsAndClientId(lastFourDigits, clientId);
    }

    /**
     * Helper method: obtener tarjeta o lanzar excepción
     */
    public Card getCardOrThrow(CardId cardId) {
        return findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card", cardId.toString()));
    }
}
