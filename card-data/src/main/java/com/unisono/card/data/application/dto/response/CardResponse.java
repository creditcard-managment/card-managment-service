package com.unisono.card.data.application.dto.response;


import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.model.CardCategory;
import com.unisono.card.data.domain.model.CardType;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardResponse {

    @Schema(description = "ID de la tarjeta", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "ID del cliente", example = "12345678")
    private String clientId;

    @Schema(description = "Número de tarjeta enmascarado", example = "**** **** **** 1234")
    private String maskedCardNumber;

    @Schema(description = "Últimos 4 dígitos", example = "1234")
    private String lastFourDigits;

    @Schema(description = "Tipo de tarjeta", example = "VISA")
    private CardType type;

    @Schema(description = "Categoría de tarjeta", example = "CLASSIC")
    private CardCategory category;

    @Schema(description = "Estado de la tarjeta", example = "ACTIVE")
    private CardStatus status;

    @Schema(description = "Fecha de vencimiento (MM/YY)", example = "12/28")
    private String expirationDate;

    @Schema(description = "Nombre del titular", example = "Juan Pérez")
    private String holderName;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de activación")
    private LocalDateTime activatedAt;

    @Schema(description = "Indica si la tarjeta está bloqueada")
    private Boolean isBlocked;

    @Schema(description = "Razón del bloqueo (si aplica)")
    private String blockReason;

    /**
     * Convierte una entidad Card del dominio a CardResponse DTO
     */
    public static CardResponse from(Card card) {
        return CardResponse.builder()
                .id(card.getId().getValue())
                .clientId(card.getClientId().getValue())
                .maskedCardNumber(card.getCardNumber() != null ? card.getCardNumber().getMasked(): null)
                .lastFourDigits(card.getCardNumber() != null ? card.getCardNumber().getLastFourDigits() : null)
                .type(card.getType())
                .category(card.getCategory())
                .status(card.getStatus())
                .expirationDate(formatExpirationDate(card.getExpirationDate()))
                .holderName(card.getHolder().getFullName().toString() != null ? card.getHolder().getFullName().toString() : null)
                .createdAt(card.getCreatedAt())
                .activatedAt(card.getActivatedAt())
                .isBlocked(card.getStatus() == CardStatus.BLOCKED)
                .blockReason(card.getBlockReason())
                .build();
    }

    /**
     * Formatea la fecha de expiración como MM/YY
     */
    private static String formatExpirationDate(ExpirationDate expirationDate) {
        if (expirationDate == null) {
            return null;
        }
        return String.format("%02d/%02d",
                expirationDate.getMonth(),
                expirationDate.getYear() % 100); // Últimos 2 dígitos del año
    }
}
