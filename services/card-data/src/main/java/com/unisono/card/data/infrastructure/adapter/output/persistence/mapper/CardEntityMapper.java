package com.unisono.card.data.infrastructure.adapter.output.persistence.mapper;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.model.CardHolder;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.CardEntity;
import com.unisono.card.shared.domain.valueobject.*;
import com.unisono.card.shared.infrastructure.security.AesEncryptionService;
import com.unisono.card.shared.infrastructure.security.BCryptHashingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper entre Card (domain) y CardEntity (persistence)
 */
@Component
@RequiredArgsConstructor
public class CardEntityMapper {

    private final AesEncryptionService encryptionService;
    private final BCryptHashingService hashingService;

    /**
     * Convierte de Domain a Entity
     * Encripta datos sensibles automáticamente
     */
    public CardEntity toEntity(Card card) {
        if (card == null) {
            return null;
        }

        CardEntity entity = new CardEntity();

        entity.setId(card.getId().getValue());
        entity.setClientId(card.getClientId().getValue());

        // Holder
        entity.setHolderFirstName(card.getHolder().getFirstName());
        entity.setHolderLastName(card.getHolder().getLastName());
        entity.setHolderDocumentNumber(card.getHolder().getDocumentNumber());
        entity.setHolderEmail(card.getHolder().getEmail());
        entity.setHolderPhoneNumber(card.getHolder().getPhoneNumber());
        entity.setHolderBirthDate(card.getHolder().getBirthDate());
        entity.setHolderAddress(card.getHolder().getAddress());

        // Datos sensibles - ENCRIPTAR
        if (card.getCardNumber() != null) {
            entity.setCardNumberEncrypted(encryptionService.encrypt(card.getCardNumber().getValue()));
            entity.setLastFourDigits(card.getCardNumber().getLastFourDigits());
        }

        if (card.getCvv() != null) {
            entity.setCvvEncrypted(encryptionService.encrypt(card.getCvv().getValue()));
        }

        // PIN: hashear si viene plano, o usar hash existente
        if (card.getPINPlainText() != null) {
            String hashedPin = hashingService.hash(card.getPINPlainText().getValue());
            entity.setPinHash(hashedPin);
        } else {
            entity.setPinHash(card.getPinHash());
        }
        entity.setPinFailedAttempts(card.getPinFailedAttempts());

        // Expiración
        if (card.getExpirationDate() != null) {
            entity.setExpirationMonth(card.getExpirationDate().getMonth());
            entity.setExpirationYear(card.getExpirationDate().getYear());
        }

        // Tipo, categoría, estado
        entity.setType(card.getType());
        entity.setCategory(card.getCategory());
        entity.setStatus(card.getStatus());

        // Fechas
        entity.setCreatedAt(card.getCreatedAt());
        entity.setUpdatedAt(card.getUpdatedAt());
        entity.setActivatedAt(card.getActivatedAt());
        entity.setBlockedAt(card.getBlockedAt());
        entity.setIssuedAt(card.getIssuedAt());
        entity.setCancelledAt(card.getCancelledAt());
        entity.setExpiredAt(card.getExpiredAt());

        // Razones
        entity.setBlockReason(card.getBlockReason());
        entity.setCancelledReason(card.getCancelledReason());

        // Soft delete
        entity.setDeleted(card.getDeleted());
        entity.setDeletedAt(card.getDeletedAt());

        return entity;
    }

    /**
     * Sobrecarga: Convierte de Domain a Entity con datos ya encriptados
     * Útil cuando la encriptación se hace en otro lugar
     */
    public CardEntity toEntity(Card card, String encryptedCardNumber, String encryptedCvv) {
        if (card == null) {
            return null;
        }

        CardEntity entity = new CardEntity();

        entity.setId(card.getId().getValue());
        entity.setClientId(card.getClientId().getValue());

        // Holder
        entity.setHolderFirstName(card.getHolder().getFirstName());
        entity.setHolderLastName(card.getHolder().getLastName());
        entity.setHolderDocumentNumber(card.getHolder().getDocumentNumber());
        entity.setHolderEmail(card.getHolder().getEmail());
        entity.setHolderPhoneNumber(card.getHolder().getPhoneNumber());
        entity.setHolderBirthDate(card.getHolder().getBirthDate());
        entity.setHolderAddress(card.getHolder().getAddress());

        // ✅ Usar datos ya encriptados
        entity.setCardNumberEncrypted(encryptedCardNumber);
        if (card.getCardNumber() != null) {
            entity.setLastFourDigits(card.getCardNumber().getLastFourDigits());
        }
        entity.setCvvEncrypted(encryptedCvv);

        // PIN: hashear si viene plano, o usar hash existente
        if (card.getPINPlainText() != null) {
            String hashedPin = hashingService.hash(card.getPINPlainText().getValue());
            entity.setPinHash(hashedPin);
        } else {
            entity.setPinHash(card.getPinHash());
        }
        entity.setPinFailedAttempts(card.getPinFailedAttempts());

        // Expiración
        if (card.getExpirationDate() != null) {
            entity.setExpirationMonth(card.getExpirationDate().getMonth());
            entity.setExpirationYear(card.getExpirationDate().getYear());
        }

        // Tipo, categoría, estado
        entity.setType(card.getType());
        entity.setCategory(card.getCategory());
        entity.setStatus(card.getStatus());

        // Fechas
        entity.setCreatedAt(card.getCreatedAt());
        entity.setUpdatedAt(card.getUpdatedAt());
        entity.setActivatedAt(card.getActivatedAt());
        entity.setBlockedAt(card.getBlockedAt());
        entity.setIssuedAt(card.getIssuedAt());
        entity.setCancelledAt(card.getCancelledAt());
        entity.setExpiredAt(card.getExpiredAt());

        // Razones
        entity.setBlockReason(card.getBlockReason());
        entity.setCancelledReason(card.getCancelledReason());

        // Soft delete
        entity.setDeleted(card.getDeleted());
        entity.setDeletedAt(card.getDeletedAt());

        return entity;
    }

    /**
     * Convierte de Entity a Domain
     * Desencripta datos sensibles automáticamente
     */
    public Card toDomain(CardEntity entity) {
        if (entity == null) {
            return null;
        }

        // Mapear holder
        CardHolder holder = CardHolder.builder()
                .firstName(entity.getHolderFirstName())
                .lastName(entity.getHolderLastName())
                .documentNumber(entity.getHolderDocumentNumber())
                .email(entity.getHolderEmail())
                .phoneNumber(entity.getHolderPhoneNumber())
                .birthDate(entity.getHolderBirthDate())
                .address(entity.getHolderAddress())
                .build();

        // Desencriptar datos sensibles
        CardNumber cardNumber = null;
        if (entity.getCardNumberEncrypted() != null) {
            String decrypted = encryptionService.decrypt(entity.getCardNumberEncrypted());
            cardNumber = CardNumber.of(decrypted);
        }

        CVV cvv = null;
        if (entity.getCvvEncrypted() != null) {
            String decrypted = encryptionService.decrypt(entity.getCvvEncrypted());
            cvv = CVV.of(decrypted);
        }

        ExpirationDate expirationDate = null;
        if (entity.getExpirationMonth() != null && entity.getExpirationYear() != null) {
            expirationDate = ExpirationDate.of(
                    entity.getExpirationMonth(),
                    entity.getExpirationYear()
            );
        }

        // Reconstituir desde persistencia
        return Card.reconstituteFromPersistence(
                CardId.of(entity.getId()),
                ClientId.of(entity.getClientId()),
                holder,
                cardNumber,
                cvv,
                entity.getPinHash(),
                entity.getPinFailedAttempts() != null ? entity.getPinFailedAttempts() : 0,
                entity.getType(),
                entity.getCategory(),
                expirationDate,
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getActivatedAt(),
                entity.getBlockedAt(),
                entity.getIssuedAt(),
                entity.getCancelledAt(),
                entity.getExpiredAt(),
                entity.getBlockReason(),
                entity.getCancelledReason(),
                entity.getDeleted(),
                entity.getDeletedAt()
        );
    }

    /**
     * Sobrecarga: Convierte de Entity a Domain con datos ya desencriptados
     * Útil cuando la desencriptación se hace en otro lugar
     */
    public Card toDomain(CardEntity entity, String decryptedCardNumber, String decryptedCvv) {
        if (entity == null) {
            return null;
        }

        // Mapear holder
        CardHolder holder = CardHolder.builder()
                .firstName(entity.getHolderFirstName())
                .lastName(entity.getHolderLastName())
                .documentNumber(entity.getHolderDocumentNumber())
                .email(entity.getHolderEmail())
                .phoneNumber(entity.getHolderPhoneNumber())
                .birthDate(entity.getHolderBirthDate())
                .address(entity.getHolderAddress())
                .build();

        // ✅ Usar datos ya desencriptados
        CardNumber cardNumber = null;
        if (decryptedCardNumber != null) {
            cardNumber = CardNumber.of(decryptedCardNumber);
        }

        CVV cvv = null;
        if (decryptedCvv != null) {
            cvv = CVV.of(decryptedCvv);
        }

        ExpirationDate expirationDate = null;
        if (entity.getExpirationMonth() != null && entity.getExpirationYear() != null) {
            expirationDate = ExpirationDate.of(
                    entity.getExpirationMonth(),
                    entity.getExpirationYear()
            );
        }

        // Reconstituir desde persistencia
        return Card.reconstituteFromPersistence(
                CardId.of(entity.getId()),
                ClientId.of(entity.getClientId()),
                holder,
                cardNumber,
                cvv,
                entity.getPinHash(),
                entity.getPinFailedAttempts() != null ? entity.getPinFailedAttempts() : 0,
                entity.getType(),
                entity.getCategory(),
                expirationDate,
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getActivatedAt(),
                entity.getBlockedAt(),
                entity.getIssuedAt(),
                entity.getCancelledAt(),
                entity.getExpiredAt(),
                entity.getBlockReason(),
                entity.getCancelledReason(),
                entity.getDeleted(),
                entity.getDeletedAt()
        );
    }
}
