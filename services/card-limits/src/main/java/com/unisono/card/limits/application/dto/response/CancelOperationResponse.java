package com.unisono.card.limits.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Resultado de cancelar una operación de crédito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resultado de cancelar una operación de crédito")
public class CancelOperationResponse {

    @Schema(description = "Id de la operación", example = "6f1c2d3e-8a9b-4c5d-9e0f-1a2b3c4d5e6f")
    private String operationId;

    @Schema(description = "CANCELLED: estaba aplicada y se deshizo. VOIDED: no había llegado y se rechazará si llega. "
            + "ALREADY_CANCELLED: ya estaba cancelada o anulada", example = "CANCELLED")
    private String outcome;

    @Schema(description = "Crédito disponible después de la cancelación", example = "95000.00")
    private BigDecimal availableCredit;

    @Schema(description = "Moneda", example = "ARS")
    private String currency;

    @Schema(description = "Cuánto quedó excedida la tarjeta (0 si no). Puede pasar al deshacer una restauración "
            + "cuyo crédito ya se había gastado: se vuelve a consumir igual, sin validar el disponible",
            example = "0.00")
    private BigDecimal overLimitAmount;
}
