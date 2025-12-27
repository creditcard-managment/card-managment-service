package com.unisono.card.limits.application.dto.response;

import com.unisono.card.limits.domain.model.LimitType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO para historial de límites
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registro histórico de cambio de límite")
public class CreditLimitHistoryResponse {

    @Schema(description = "ID del registro")
    private Long id;

    @Schema(description = "Tipo de límite modificado")
    private LimitType limitType;

    @Schema(description = "Acción realizada")
    private String action;

    @Schema(description = "Valor anterior")
    private BigDecimal previousValue;

    @Schema(description = "Valor nuevo")
    private BigDecimal newValue;

    @Schema(description = "Diferencia")
    private BigDecimal difference;

    @Schema(description = "Moneda")
    private String currency;

    @Schema(description = "Razón del cambio")
    private String reason;

    @Schema(description = "Usuario que realizó el cambio")
    private String changedBy;

    @Schema(description = "Fecha del cambio")
    private LocalDateTime changedAt;

    @Schema(description = "Descripción legible")
    private String description;
}
