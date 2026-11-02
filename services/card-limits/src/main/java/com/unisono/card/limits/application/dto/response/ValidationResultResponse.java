package com.unisono.card.limits.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO para resultado de validación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resultado de validación de transacción")
public class ValidationResultResponse {

    @Schema(description = "Indica si la transacción fue aprobada")
    private Boolean approved;

    @Schema(description = "Razón de aprobación o rechazo")
    private String reason;

    @Schema(description = "Crédito disponible")
    private BigDecimal availableCredit;

    @Schema(description = "Monto solicitado")
    private BigDecimal requestedAmount;

    @Schema(description = "Moneda")
    private String currency;
}
