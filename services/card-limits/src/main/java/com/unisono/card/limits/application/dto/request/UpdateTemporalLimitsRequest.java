package com.unisono.card.limits.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO para actualizar límites temporales (diario/mensual)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para actualizar límites temporales")
public class UpdateTemporalLimitsRequest {

    @DecimalMin(value = "0.01", message = "El límite diario debe ser mayor a cero")
    @Schema(description = "Nuevo límite diario", example = "150000.00")
    private BigDecimal dailyLimitAmount;

    @DecimalMin(value = "0.01", message = "El límite mensual debe ser mayor a cero")
    @Schema(description = "Nuevo límite mensual", example = "600000.00")
    private BigDecimal monthlyLimitAmount;

    @NotBlank(message = "La moneda es obligatoria")
    @Schema(description = "Código de moneda ISO", example = "ARS")
    private String currency;

    @NotBlank(message = "La razón del cambio es obligatoria")
    @Schema(description = "Razón del cambio", example = "Ajuste de límites temporales")
    private String reason;

    @NotBlank(message = "El usuario que realiza el cambio es obligatorio")
    @Schema(description = "Usuario que realiza el cambio", example = "admin@unisono.com")
    private String changedBy;
}
