package com.unisono.card.data.infrastructure.adapter.output.persistence.adapter;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.data.domain.port.output.EncryptionPort;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.CardEntity;
import com.unisono.card.data.infrastructure.adapter.output.persistence.mapper.CardEntityMapper;
import com.unisono.card.data.infrastructure.adapter.output.persistence.repository.JpaCardRepository;
import com.unisono.card.shared.domain.valueobject.CVV;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardNumber;
import com.unisono.card.shared.domain.valueobject.ClientId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de repositorio
 *
 * Implementa CardRepository (domain port) usando JPA
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardRepositoryAdapter implements CardRepository {

    private final JpaCardRepository jpaRepository;
    private final CardEntityMapper mapper;
    private final EncryptionPort encryptionPort;

    /*
    @Override
    public Card save(Card card) {
        log.debug("Saving card: {}", card.getId());

        // Encriptar datos sensibles antes de guardar
        String encryptedCardNumber = card.getCardNumber() != null ?
                encryptionPort.encrypt(card.getCardNumber().getValue()) : null;

        String encryptedCvv = card.getCvv() != null ?
                encryptionPort.encrypt(card.getCvv().getValue()) : null;

        // Convertir Domain → Entity
        CardEntity entity = mapper.toEntity(card, encryptedCardNumber, encryptedCvv);

        // Guardar en BD
        CardEntity savedEntity = jpaRepository.save(entity);

        // Convertir Entity → Domain (desencriptando)
        String decryptedCardNumber = savedEntity.getCardNumberEncrypted() != null ?
                encryptionPort.decrypt(savedEntity.getCardNumberEncrypted()) : null;

        String decryptedCvv = savedEntity.getCvvEncrypted() != null ?
                encryptionPort.decrypt(savedEntity.getCvvEncrypted()) : null;

        Card savedCard = mapper.toDomain(savedEntity, decryptedCardNumber, decryptedCvv);

        log.debug("Card saved successfully: {}", savedCard.getId());

        return savedCard;
    }*/

    /*
    @Override
    public Optional<Card> findById(CardId cardId) {
        log.debug("Finding card by ID: {}", cardId);

        return jpaRepository.findById(cardId.getValue())
                .map(this::entityToDomain);
    }*/

    @Override
    public List<Card> findByClientId(ClientId clientId) {
        log.debug("Finding cards for client: {}", clientId);

        return jpaRepository.findByClientId(clientId.getValue()).stream()
                .map(this::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Card> findByLastFourDigitsAndClientId(String lastFourDigits, ClientId clientId) {
        log.debug("Finding card by last 4 digits: {} for client: {}", lastFourDigits, clientId);

        return jpaRepository.findByLastFourDigitsAndClientId(lastFourDigits, clientId.getValue())
                .map(this::entityToDomain);
    }

    @Override
    public Optional<Card> findByCardNumber(String encryptedCardNumber) {
        log.debug("Finding card by encrypted card number");

        return jpaRepository.findByCardNumberEncrypted(encryptedCardNumber)
                .map(this::entityToDomain);
    }

    @Override
    public void delete(CardId cardId) {
        log.debug("Deleting card (soft delete): {}", cardId);

        jpaRepository.findById(cardId.getValue()).ifPresent(entity -> {
            entity.markAsDeleted();
            jpaRepository.save(entity);
        });
    }

    @Override
    public boolean existsById(CardId cardId) {
        return jpaRepository.findById(cardId.getValue()).isPresent();
    }

    @Override
    public Card save(Card card) {
        log.debug("Saving card: {}", card.getId());

        // Convertir Domain → Entity
        CardEntity entity = mapper.toEntity(card);

        // ✅ Encriptar datos sensibles ANTES de guardar
        if (entity.getCardNumber() != null) {
            entity.setCardNumber(encryptionPort.encrypt(entity.getCardNumber()));
        }

        if (entity.getCvv() != null) {
            entity.setCvv(encryptionPort.encrypt(entity.getCvv()));
        }

        // Guardar en BD
        CardEntity savedEntity = jpaRepository.save(entity);

        // ✅ Desencriptar datos sensibles DESPUÉS de guardar
        String decryptedCardNumber = savedEntity.getCardNumber() != null ?
                encryptionPort.decrypt(savedEntity.getCardNumber()) : null;

        String decryptedCvv = savedEntity.getCvv() != null ?
                encryptionPort.decrypt(savedEntity.getCvv()) : null;

        // Convertir Entity → Domain (con datos desencriptados)
        Card savedCard = mapper.toDomain(savedEntity);

        // Asignar los valores desencriptados
        if (decryptedCardNumber != null) {
            savedCard.setCardNumber(new CardNumber(decryptedCardNumber));
        }
        if (decryptedCvv != null) {
            savedCard.setCvv(new CVV(decryptedCvv));
        }

        log.debug("Card saved successfully: {}", savedCard.getId());
        return savedCard;
    }

    @Override
    public Optional<Card> findById(CardId cardId) {
        log.debug("Finding card by ID: {}", cardId);

        Optional<CardEntity> entityOpt = jpaRepository.findById(cardId.getValue());

        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        CardEntity entity = entityOpt.get();

        // ✅ Desencriptar datos sensibles
        String decryptedCardNumber = entity.getCardNumber() != null ?
                encryptionPort.decrypt(entity.getCardNumber()) : null;

        String decryptedCvv = entity.getCvv() != null ?
                encryptionPort.decrypt(entity.getCvv()) : null;

        // Convertir Entity → Domain
        Card card = mapper.toDomain(entity);

        // Asignar los valores desencriptados
        if (decryptedCardNumber != null) {
            card.setCardNumber(new CardNumber(decryptedCardNumber));
        }
        if (decryptedCvv != null) {
            card.setCvv(new CVV(decryptedCvv));
        }

        return Optional.of(card);
    }

    // ========================================
    // Helper method
    // ========================================

    private Card entityToDomain(CardEntity entity) {
        // Desencriptar datos sensibles
        String decryptedCardNumber = entity.getCardNumberEncrypted() != null ?
                encryptionPort.decrypt(entity.getCardNumberEncrypted()) : null;

        String decryptedCvv = entity.getCvvEncrypted() != null ?
                encryptionPort.decrypt(entity.getCvvEncrypted()) : null;

        return mapper.toDomain(entity, decryptedCardNumber, decryptedCvv);
    }
}
