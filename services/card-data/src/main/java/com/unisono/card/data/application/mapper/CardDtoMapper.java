package com.unisono.card.data.application.mapper;

import com.unisono.card.data.application.dto.request.CreateCardRequest;
import com.unisono.card.data.application.dto.response.CardDetailResponse;
import com.unisono.card.data.application.dto.response.CardResponse;
import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.model.CardHolder;
import com.unisono.card.data.domain.port.input.CreateCardUseCase;
import com.unisono.card.shared.domain.valueobject.ClientId;
import org.springframework.stereotype.Component;

/**
 * Mapper entre DTOs y Domain Models
 */
@Component
public class CardDtoMapper {

    /**
     * CreateCardRequest → CreateCardCommand
     */
    public CreateCardUseCase.CreateCardCommand toCommand(CreateCardRequest request) {
        CardHolder holder = CardHolder.create(
                request.getFirstName(),
                request.getLastName(),
                request.getDocumentNumber(),
                request.getEmail(),
                request.getPhoneNumber(),
                request.getBirthDate()
        );

        if (request.getAddress() != null) {
            holder.updateAddress(request.getAddress());
        }

        return new CreateCardUseCase.CreateCardCommand(
                ClientId.of(request.getClientId()),
                holder,
                request.getType(),
                request.getCategory()
        );
    }

    /**
     * Card → CardResponse (vista resumida)
     */
    public CardResponse toResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId().getValue())
                .clientId(card.getClientId().getValue())
                .maskedCardNumber(card.getMaskedCardNumber())
                .lastFourDigits(card.getCardNumber() != null ?
                        card.getCardNumber().getLastFourDigits() : null)
                .type(card.getType())
                .category(card.getCategory())
                .status(card.getStatus())
                .expirationDate(card.getExpirationDate() != null ?
                        card.getExpirationDate().toString() : null)
                .holderName(card.getHolder() != null ?
                        card.getHolder().getFullName() : null)
                .createdAt(card.getCreatedAt())
                .activatedAt(card.getActivatedAt())
                .isBlocked(card.isBlocked())
                .blockReason(card.getBlockReason())
                .build();
    }

    /**
     * Card → CardDetailResponse (vista detallada)
     */
    public CardDetailResponse toDetailResponse(Card card) {
        CardDetailResponse.CardDetailResponseBuilder builder = CardDetailResponse.builder()
                .id(card.getId().getValue())
                .clientId(card.getClientId().getValue())
                .maskedCardNumber(card.getMaskedCardNumber())
                .lastFourDigits(card.getCardNumber() != null ?
                        card.getCardNumber().getLastFourDigits() : null)
                .type(card.getType())
                .category(card.getCategory())
                .status(card.getStatus())
                .expirationDate(card.getExpirationDate() != null ?
                        card.getExpirationDate().toString() : null)
                .isExpired(card.getExpirationDate() != null ?
                        card.getExpirationDate().isExpired() : null)
                .requiresRenewal(card.requiresRenewal())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .activatedAt(card.getActivatedAt())
                .blockedAt(card.getBlockedAt())
                .blockReason(card.getBlockReason())
                .isBlocked(card.isBlocked())
                .isOperational(card.isOperational())
                .hasPinSet(card.isPINSet());

        // Datos del titular
        if (card.getHolder() != null) {
            builder
                    .holderName(card.getHolder().getFullName())
                    .holderEmail(card.getHolder().getEmail())
                    .holderPhone(card.getHolder().getPhoneNumber())
                    .holderDocument(card.getHolder().getDocumentNumber())
                    .holderBirthDate(card.getHolder().getBirthDate())
                    .holderAddress(card.getHolder().getAddress());
        }

        return builder.build();
    }
}
