package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.CreateCardUseCase;
import com.unisono.card.data.domain.port.input.GetCardUseCase;
import com.unisono.card.data.domain.port.input.UpdateCardStatusUseCase;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.data.domain.port.output.EventOutboxPort;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CardCreatedEvent;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de gestión de tarjetas
 * Implementa los use cases de creación, consulta y cambio de estado
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CardManagementService implements CreateCardUseCase, GetCardUseCase, UpdateCardStatusUseCase {

    private final CardRepository cardRepository;
    private final EventOutboxPort eventOutbox;
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

        // Encolar el evento en la misma transacción (outbox); OutboxRelay lo publica en Kafka
        eventOutbox.enqueue(KafkaTopics.CARD_CREATED, savedCard.getId().getValue(), new CardCreatedEvent(
                savedCard.getId().getValue(),
                savedCard.getClientId().getValue(),
                savedCard.getHolder().getFullName(),
                savedCard.getType().name(),
                savedCard.getCategory().name(),
                "system"
        ));

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

    // ========================================
    // UpdateCardStatusUseCase
    // ========================================

    @Override
    public Card execute(UpdateStatusCommand command) {
        log.info("Actualizando estado de tarjeta: {} -> {}", command.cardId(), command.newStatus());

        Card card = getCardOrThrow(command.cardId());

        // Valida la transición en el aggregate (BusinessRuleException si no está permitida)
        card.updateStatus(command.newStatus());

        Card savedCard = cardRepository.save(card);

        log.info("Estado actualizado exitosamente");

        return savedCard;
    }

    /**
     * Cancela la tarjeta registrando motivo y fecha (cancelled_reason, cancelled_at).
     * BusinessRuleException si la transición a CANCELLED no está permitida.
     */
    public Card cancel(CardId cardId, String reason) {
        log.info("Cancelando tarjeta: {} - Razón: {}", cardId, reason);

        Card card = getCardOrThrow(cardId);

        card.cancel(reason);

        Card savedCard = cardRepository.save(card);

        log.info("Tarjeta cancelada exitosamente: {}", cardId);

        return savedCard;
    }

    /**
     * Actualiza la fecha de activación
     */
    public void updateActivatedAt(CardId cardId, LocalDateTime activatedAt) {
        log.info("Actualizando fecha de activación de tarjeta: {}", cardId);

        Card card = getCardOrThrow(cardId);

        card.setActivatedAt(activatedAt);

        cardRepository.save(card);

        log.info("Fecha de activación actualizada exitosamente");
    }

    /**
     * Actualiza los datos de la tarjeta (número, cvv, pin, expiración)
     */
    public void updateCardData(
            CardId cardId,
            String cardNumber,
            String cvv,
            String pin,
            int expirationMonth,
            int expirationYear
    ) {
        log.info("Actualizando datos de tarjeta: {}", cardId);

        Card card = getCardOrThrow(cardId);

        // Crear Value Objects (InvalidValueObjectException si el formato es inválido)
        CardNumber cardNumberVO = CardNumber.of(cardNumber);
        CVV cvvVO = CVV.of(cvv);
        PIN pinVO = PIN.of(pin);
        ExpirationDate expirationDate = ExpirationDate.forIssuance(expirationMonth, expirationYear);

        card.updateCardData(cardNumberVO, cvvVO, pinVO, expirationDate);

        cardRepository.save(card);

        log.info("Datos de tarjeta actualizados exitosamente");
    }
}
