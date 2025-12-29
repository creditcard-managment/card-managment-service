package com.unisono.card.lifecycle.infrastructure.adapter.input.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para aprobar tarjeta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para aprobar una tarjeta")
public class ApproveCardRequest {

    @NotBlank(message = "La razón de aprobación es obligatoria")
    @Schema(description = "Razón de la aprobación", example = "Cliente cumple con requisitos crediticios")
    private String reason;

    @NotBlank(message = "El aprobador es obligatorio")
    @Schema(description = "Usuario que aprueba", example = "admin@bank.com")
    private String approvedBy;
}
