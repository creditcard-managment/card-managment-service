package com.unisono.card.limits.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO para deshacer el consumo de una transacción reversada o devuelta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para restaurar el crédito consumido por una transacción reversada o devuelta")
public class RestoreCreditRequest {

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Schema(description = "Monto reversado o devuelto", example = "1500.00")
    private BigDecimal amount;

    @NotBlank(message = "La moneda es obligatoria")
    @Schema(description = "Código de moneda ISO", example = "ARS")
    private String currency;

    @NotNull(message = "La fecha de la transacción original es obligatoria")
    @PastOrPresent(message = "La fecha de la transacción original no puede ser futura")
    @Schema(description = "Fecha de la transacción original (la que consumió el cupo)", example = "2026-11-03")
    private LocalDate originalTransactionDate;

    @NotBlank(message = "La descripción es obligatoria")
    @Schema(description = "Descripción", example = "Reverso de la compra 3f2a…")
    private String description;
}
