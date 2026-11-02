package com.unisono.card.data.application.dto.response;

import com.unisono.card.data.domain.model.CardCategory;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.data.domain.model.CardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO para Card (vista detallada)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información detallada de una tarjeta")
public class CardDetailResponse {

    @Schema(description = "ID de la tarjeta")
    private String id;

    @Schema(description = "ID del cliente")
    private String clientId;

    @Schema(description = "Número de tarjeta enmascarado")
    private String maskedCardNumber;

    @Schema(description = "Últimos 4 dígitos")
    private String lastFourDigits;

    @Schema(description = "Tipo de tarjeta")
    private CardType type;

    @Schema(description = "Categoría de tarjeta")
    private CardCategory category;

    @Schema(description = "Estado de la tarjeta")
    private CardStatus status;

    @Schema(description = "Fecha de vencimiento (MM/YY)")
    private String expirationDate;

    @Schema(description = "Indica si está vencida")
    private Boolean isExpired;

    @Schema(description = "Indica si requiere renovación")
    private Boolean requiresRenewal;

    // Datos del titular
    @Schema(description = "Nombre completo del titular")
    private String holderName;

    @Schema(description = "Email del titular")
    private String holderEmail;

    @Schema(description = "Teléfono del titular")
    private String holderPhone;

    @Schema(description = "Documento del titular")
    private String holderDocument;

    @Schema(description = "Fecha de nacimiento del titular")
    private LocalDate holderBirthDate;

    @Schema(description = "Dirección del titular")
    private String holderAddress;

    // Auditoría
    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;

    @Schema(description = "Fecha de activación")
    private LocalDateTime activatedAt;

    @Schema(description = "Fecha de bloqueo")
    private LocalDateTime blockedAt;

    @Schema(description = "Razón del bloqueo")
    private String blockReason;

    @Schema(description = "Indica si la tarjeta está bloqueada")
    private Boolean isBlocked;

    @Schema(description = "Indica si la tarjeta es operacional")
    private Boolean isOperational;

    @Schema(description = "Indica si tiene PIN configurado")
    private Boolean hasPinSet;
}
