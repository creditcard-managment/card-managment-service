package com.unisono.card.limits.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO para CreditLimit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información de límite de crédito")
public class CreditLimitResponse {

    @Schema(description = "ID del límite de crédito")
    private String id;

    @Schema(description = "ID de la tarjeta")
    private String cardId;

    @Schema(description = "Límite de compras")
    private BigDecimal purchaseLimit;

    @Schema(description = "Límite de adelantos")
    private BigDecimal cashAdvanceLimit;

    @Schema(description = "Crédito disponible")
    private BigDecimal availableCredit;

    @Schema(description = "Crédito consumido")
    private BigDecimal consumedCredit;

    @Schema(description = "Límite diario")
    private BigDecimal dailyLimit;

    @Schema(description = "Disponible hoy")
    private BigDecimal dailyAvailable;

    @Schema(description = "Límite mensual")
    private BigDecimal monthlyLimit;

    @Schema(description = "Disponible este mes")
    private BigDecimal monthlyAvailable;

    @Schema(description = "Moneda")
    private String currency;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    @Schema(description = "Última actualización")
    private LocalDateTime updatedAt;
}
