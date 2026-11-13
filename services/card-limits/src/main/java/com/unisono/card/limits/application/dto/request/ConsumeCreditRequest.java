package com.unisono.card.limits.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO para consumir crédito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para consumir crédito")
public class ConsumeCreditRequest {

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Schema(description = "Monto a consumir", example = "5000.00")
    private BigDecimal amount;

    @NotBlank(message = "La moneda es obligatoria")
    @Schema(description = "Código de moneda ISO", example = "ARS")
    private String currency;

    @NotBlank(message = "La descripción es obligatoria")
    @Schema(description = "Descripción de la transacción", example = "Compra en Mercado Libre")
    private String description;

    @Size(max = 64, message = "El id de operación no puede superar 64 caracteres")
    @Schema(description = "Id de la operación, opcional. Con un id la operación es idempotente "
            + "y se puede cancelar con POST /{creditLimitId}/operations/{operationId}/cancel",
            example = "6f1c2d3e-8a9b-4c5d-9e0f-1a2b3c4d5e6f")
    private String operationId;
}
