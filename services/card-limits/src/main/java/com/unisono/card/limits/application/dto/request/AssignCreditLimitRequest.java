package com.unisono.card.limits.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO para asignar límite de crédito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para asignar límite de crédito a una tarjeta")
public class AssignCreditLimitRequest {

    @NotBlank(message = "El ID de la tarjeta es obligatorio")
    @Schema(description = "ID de la tarjeta", example = "550e8400-e29b-41d4-a716-446655440000")
    private String cardId;

    @NotNull(message = "El límite de compras es obligatorio")
    @DecimalMin(value = "1000.00", message = "El límite de compras debe ser al menos $1,000")
    @DecimalMax(value = "10000000.00", message = "El límite de compras no puede exceder $10,000,000")
    @Schema(description = "Límite de compras", example = "50000.00")
    private BigDecimal purchaseLimitAmount;

    @NotBlank(message = "La moneda es obligatoria")
    @Size(min = 3, max = 3, message = "La moneda debe ser un código ISO de 3 letras")
    @Schema(description = "Código de moneda ISO", example = "ARS")
    private String currency;

    @DecimalMin(value = "0.0", message = "El porcentaje debe ser al menos 0")
    @DecimalMax(value = "1.0", message = "El porcentaje no puede exceder 1 (100%)")
    @Schema(description = "Porcentaje del límite de compras para adelantos", example = "0.30", defaultValue = "0.30")
    @Builder.Default  // ⭐ AGREGAR ESTA ANOTACIÓN
    private Double cashAdvancePercentage = 0.30; // 30% por defecto

    @DecimalMin(value = "0.00", message = "El límite diario no puede ser negativo")
    @Schema(description = "Límite diario de transacciones (opcional)", example = "100000.00")
    private BigDecimal dailyLimitAmount;

    @DecimalMin(value = "0.00", message = "El límite mensual no puede ser negativo")
    @Schema(description = "Límite mensual de transacciones (opcional)", example = "500000.00")
    private BigDecimal monthlyLimitAmount;

    @NotBlank(message = "El usuario que asigna el límite es obligatorio")
    @Schema(description = "Usuario que asigna el límite", example = "admin@unisono.com")
    private String assignedBy;
}
