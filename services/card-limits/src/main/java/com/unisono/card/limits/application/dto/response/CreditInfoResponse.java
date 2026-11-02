package com.unisono.card.limits.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO con información detallada de crédito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información detallada de crédito disponible")
public class CreditInfoResponse {

    @Schema(description = "Límite total de compras")
    private BigDecimal purchaseLimit;

    @Schema(description = "Límite total de adelantos")
    private BigDecimal cashAdvanceLimit;

    @Schema(description = "Crédito disponible")
    private BigDecimal availableCredit;

    @Schema(description = "Crédito consumido")
    private BigDecimal consumedCredit;

    @Schema(description = "Porcentaje de utilización")
    private Double utilizationPercentage;

    @Schema(description = "Disponible hoy")
    private BigDecimal dailyAvailable;

    @Schema(description = "Disponible este mes")
    private BigDecimal monthlyAvailable;

    @Schema(description = "Moneda")
    private String currency;

    @Schema(description = "Indica si el crédito está agotado")
    private Boolean isExhausted;
}
