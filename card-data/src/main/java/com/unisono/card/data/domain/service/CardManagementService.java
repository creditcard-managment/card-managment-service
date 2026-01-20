package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.event.CardCreatedDomainEvent;
import com.unisono.card.data.domain.event.CardHolderDataUpdatedEvent;
import com.unisono.card.data.domain.event.CardPinUpdatedEvent;
import com.unisono.card.data.domain.exception.CardNotFoundException;
import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.CreateCardUseCase;
import com.unisono.card.data.domain.port.input.GetCardUseCase;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.data.infrastructure.adapter.input.rest.dto.UpdateHolderDataRequest;
import com.unisono.card.data.infrastructure.adapter.input.rest.dto.UpdatePinRequest;
import com.unisono.card.events.domain.CardCancelledDomainEvent;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CVV;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardNumber;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.domain.valueobject.ClientId;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import com.unisono.card.shared.domain.valueobject.PIN;
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

    private final PinEncryptionService pinEncryptionService;

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

    @Transactional
    public void updateHolderData(String cardId, UpdateHolderDataRequest request) {

        log.info("Updating holder data for card: {}", cardId);

        // 1. Convertir String a CardId y buscar la tarjeta
        Card card = cardRepository.findById(new CardId(cardId))
                .orElseThrow(() -> new CardNotFoundException(new CardId(cardId)));

        // 2. Validar que la tarjeta esté activa (opcional según reglas de negocio)
        if (card.getStatus() == CardStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update holder data for a cancelled card");
        }

        // 3. Actualizar solo los campos que vienen (partial update)
        boolean hasChanges = false;

        if (request.getHolderEmail() != null && !request.getHolderEmail().isBlank()) {
            card.updateHolderEmail(request.getHolderEmail());
            hasChanges = true;
            log.debug("Email updated for card: {}", cardId);
        }

        if (request.getHolderPhoneNumber() != null && !request.getHolderPhoneNumber().isBlank()) {
            card.updateHolderPhoneNumber(request.getHolderPhoneNumber());
            hasChanges = true;
            log.debug("Phone number updated for card: {}", cardId);
        }

        if (request.getHolderAddress() != null && !request.getHolderAddress().isBlank()) {
            card.updateHolderAddress(request.getHolderAddress());
            hasChanges = true;
            log.debug("Address updated for card: {}", cardId);
        }

        if (!hasChanges) {
            log.warn("No valid fields to update for card: {}", cardId);
            throw new IllegalArgumentException("At least one field must be provided for update");
        }

        // 4. Guardar cambios
        cardRepository.save(card);

        log.info("Holder data updated successfully for card: {}", cardId);

        // 5. Publicar evento de dominio
        eventPublisher.publishEvent(new CardHolderDataUpdatedEvent(
                new CardId(cardId),
                request.getHolderEmail(),
                request.getHolderPhoneNumber(),
                request.getHolderAddress(),
                LocalDateTime.now()
        ));

        log.info("🟢 CardHolderDataUpdatedEvent published for card: {}", cardId);
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

    @Transactional
    public Card replaceCard(String cardId, String reason) {
        log.info("Replacing card: {} due to: {}", cardId, reason);

        // 1. Convertir String a CardId y buscar tarjeta anterior
        Card oldCard = cardRepository.findById(new CardId(cardId))
                .orElseThrow(() -> new CardNotFoundException(cardId));

        // 2. Validar que no esté ya cancelada
        if (oldCard.getStatus() == CardStatus.CANCELLED) {
            throw new IllegalStateException("Cannot replace an already cancelled card");
        }

        // 3. Crear nueva tarjeta reutilizando los datos del titular
        Card newCard = Card.create(
                oldCard.getClientId(),
                oldCard.getHolder(),      // ← MISMO titular
                oldCard.getType(),        // ← MISMO tipo
                oldCard.getCategory()     // ← MISMA categoría
        );

        // 4. Cancelar tarjeta anterior
        oldCard.cancel(reason);
        cardRepository.save(oldCard);

        // 5. Publicar evento de cancelación
        log.info("🔴 Publishing CardCancelledDomainEvent to Spring...");
        eventPublisher.publishEvent(new CardCancelledDomainEvent(
                oldCard.getId(),
                oldCard.getCardNumber().getMasked(),
                reason,
                LocalDateTime.now()
        ));
        log.info("🔴 ✅ CardCancelledDomainEvent published");

        // 6. Guardar nueva tarjeta y publicar evento
        Card savedCard = saveAndPublishCardCreated(newCard);

        log.info("Card {} replaced successfully with new card: {}", cardId, savedCard.getId());

        return savedCard;
    }
    @Transactional
    public void updatePin(String cardId, UpdatePinRequest request) {

        log.info("Updating PIN for card: {}", cardId);

        // 1. Buscar la tarjeta
        Card card = cardRepository.findById(new CardId(cardId))
                .orElseThrow(() -> new CardNotFoundException(new CardId(cardId)));

        // 2. Validar que la tarjeta esté activa
        if (card.getStatus() == CardStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update PIN for a cancelled card");
        }

        // 3. Verificar que el PIN actual sea correcto
        if (!pinEncryptionService.matches(request.getCurrentPin(), card.getPinHash())) {
            log.warn("Invalid current PIN attempt for card: {}", cardId);
            throw new IllegalArgumentException("Current PIN is incorrect");
        }

        // 4. Validar que el nuevo PIN sea diferente
        if (request.getCurrentPin().equals(request.getNewPin())) {
            throw new IllegalArgumentException("New PIN must be different from current PIN");
        }

        // 5. Encriptar el nuevo PIN
        String newPinHash = pinEncryptionService.encrypt(request.getNewPin());

        // 6. Actualizar el PIN en el dominio
        card.updatePin(card.getPinHash(), newPinHash);

        // 7. Guardar
        cardRepository.save(card);

        log.info("PIN updated successfully for card: {}", cardId);

        // 8. Publicar evento
        eventPublisher.publishEvent(new CardPinUpdatedEvent(
                new CardId(cardId),
                LocalDateTime.now()
        ));

        log.info("🟣 CardPinUpdatedEvent published for card: {}", cardId);
    }

    // ========================================
    // MÉTODO PRIVADO REUTILIZABLE
    // ========================================
    private Card saveAndPublishCardCreated(Card card) {
        // Guardar en BD
        Card savedCard = cardRepository.save(card);

        log.info("Card created successfully with ID: {}", savedCard.getId());

        // Publicar evento
        log.info("🔵 Publishing CardCreatedDomainEvent to Spring...");
        eventPublisher.publishEvent(new CardCreatedDomainEvent(savedCard));
        log.info("🔵 ✅ CardCreatedDomainEvent published to Spring");

        return savedCard;
    }
}
