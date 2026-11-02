package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.event.CardCreatedDomainEvent;
import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.CreateCardUseCase;
import com.unisono.card.data.domain.port.input.GetCardUseCase;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final ApplicationEventPublisher eventPublisher;
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

        log.info("🔵 Publishing CardCreatedDomainEvent to Spring...");
        eventPublisher.publishEvent(new CardCreatedDomainEvent(savedCard));
        log.info("🔵 ✅ CardCreatedDomainEvent published to Spring");

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

    /**
     * Actualiza solo el estado de la tarjeta
     */
    @Transactional
    public void updateStatus(String cardId, String status) {
        log.info("Actualizando estado de tarjeta: {} -> {}", cardId, status);

        Card card = cardRepository.findById(CardId.of(cardId))
                .orElseThrow(() -> new IllegalArgumentException("Tarjeta no encontrada: " + cardId));

        // Actualizar status
        card.updateStatus(CardStatus.valueOf(status));

        cardRepository.save(card);

        log.info("Estado actualizado exitosamente");
    }

    /**
     * Actualiza la fecha de activación
     */
    @Transactional
    public void updateActivatedAt(String cardId, LocalDateTime activatedAt) {
        log.info("Actualizando fecha de activación de tarjeta: {}", cardId);

        Card card = cardRepository.findById(CardId.of(cardId))
                .orElseThrow(() -> new IllegalArgumentException("Tarjeta no encontrada: " + cardId));

        // Actualizar fecha de activación
        card.setActivatedAt(activatedAt);

        cardRepository.save(card);

        log.info("Fecha de activación actualizada exitosamente");
    }

    /**
     * Actualiza los datos de la tarjeta (número, cvv, pin, expiración)
     */
    @Transactional
    public void updateCardData(
            String cardId,
            String cardNumber,
            String cvv,
            String pin,
            Integer expirationMonth,
            Integer expirationYear
    ) {
        log.info("Actualizando datos de tarjeta: {}", cardId);

        Card card = cardRepository.findById(CardId.of(cardId))
                .orElseThrow(() -> new IllegalArgumentException("Tarjeta no encontrada: " + cardId));

        // Crear Value Objects
        CardNumber cardNumberVO = CardNumber.of(cardNumber);
        CVV cvvVO = CVV.of(cvv);
        PIN pinVO = PIN.of(pin);
        ExpirationDate expirationDate = ExpirationDate.of(expirationMonth, expirationYear);

        // Actualizar datos en el dominio
        card.updateCardData(cardNumberVO, cvvVO, pinVO, expirationDate);

        cardRepository.save(card);

        log.info("Datos de tarjeta actualizados exitosamente");
    }
}
