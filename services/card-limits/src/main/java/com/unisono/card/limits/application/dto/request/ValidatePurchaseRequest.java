package com.unisono.card.limits.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO para validar una compra/adelanto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para validar una compra o adelanto")
public class ValidatePurchaseRequest {

    @NotBlank(message = "El ID de la tarjeta es obligatorio")
    @Schema(description = "ID de la tarjeta", example = "550e8400-e29b-41d4-a716-446655440000")
    private String cardId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Schema(description = "Monto de la transacción", example = "5000.00")
    private BigDecimal amount;

    @NotBlank(message = "La moneda es obligatoria")
    @Schema(description = "Código de moneda ISO", example = "ARS")
    private String currency;
}
