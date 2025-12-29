package com.unisono.card.data.infrastructure.adapter.output.persistence.mapper;

import com.unisono.card.data.domain.model.*;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.CardEntity;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.CardHolderEmbeddable;
import com.unisono.card.shared.domain.valueobject.*;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * Mapper entre Card (Domain) y CardEntity (JPA)
 */
@Component
public class CardEntityMapper {

    /**
     * Convierte Domain → Entity
     */
    public CardEntity toEntity(Card card, String encryptedCardNumber, String encryptedCvv) {
        if (card == null) {
            return null;
        }

        CardEntity entity = CardEntity.builder()
                .id(card.getId().getValue())
                .clientId(card.getClientId().getValue())
                .holder(toEmbeddable(card.getHolder()))
                .cardNumberEncrypted(encryptedCardNumber)
                .lastFourDigits(card.getCardNumber() != null ? card.getCardNumber().getLastFourDigits() : null)
                .cvvEncrypted(encryptedCvv)
                .pinHash(card.getPinHash())
                .type(card.getType())
                .category(card.getCategory())
                .status(card.getStatus())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .activatedAt(card.getActivatedAt())
                .blockedAt(card.getBlockedAt())
                .blockReason(card.getBlockReason())
                .build();

        // Expiration date
        if (card.getExpirationDate() != null) {
            YearMonth ym = card.getExpirationDate().getYearMonth();
            entity.setExpirationMonth(ym.getMonthValue());
            entity.setExpirationYear(ym.getYear());
        }

        return entity;
    }

    /**
     * Convierte Entity → Domain
     * ⭐ USA el método reconstitute en lugar de builder
     */
    public Card toDomain(CardEntity entity, String decryptedCardNumber, String decryptedCvv) {
        if (entity == null) {
            return null;
        }

        // Reconstruir CardNumber y CVV
        CardNumber cardNumber = decryptedCardNumber != null ?
                CardNumber.of(decryptedCardNumber) : null;
        CVV cvv = decryptedCvv != null ?
                CVV.of(decryptedCvv) : null;

        // Reconstruir ExpirationDate
        ExpirationDate expirationDate = null;
        if (entity.getExpirationMonth() != null && entity.getExpirationYear() != null) {
            expirationDate = ExpirationDate.of(
                    entity.getExpirationMonth(),
                    entity.getExpirationYear()
            );
        }

        // ⭐ Usar el método factory reconstitute
        return Card.reconstituteFromPersistence(
                CardId.of(entity.getId()),
                ClientId.of(entity.getClientId()),
                toDomain(entity.getHolder()),
                cardNumber,
                cvv,
                entity.getPinHash(),
                entity.getType(),
                entity.getCategory(),
                expirationDate,
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getActivatedAt(),
                entity.getBlockedAt(),
                entity.getBlockReason()
        );
    }

    // ========================================
    // CardHolder mapping
    // ========================================

    private CardHolderEmbeddable toEmbeddable(CardHolder holder) {
        if (holder == null) {
            return null;
        }

        return CardHolderEmbeddable.builder()
                .firstName(holder.getFirstName())
                .lastName(holder.getLastName())
                .documentNumber(holder.getDocumentNumber())
                .email(holder.getEmail())
                .phoneNumber(holder.getPhoneNumber())
                .birthDate(holder.getBirthDate())
                .address(holder.getAddress())
                .build();
    }

    private CardHolder toDomain(CardHolderEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }

        return CardHolder.builder()
                .firstName(embeddable.getFirstName())
                .lastName(embeddable.getLastName())
                .documentNumber(embeddable.getDocumentNumber())
                .email(embeddable.getEmail())
                .phoneNumber(embeddable.getPhoneNumber())
                .birthDate(embeddable.getBirthDate())
                .address(embeddable.getAddress())
                .build();
    }
}
