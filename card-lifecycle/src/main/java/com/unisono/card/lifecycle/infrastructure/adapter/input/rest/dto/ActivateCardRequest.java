package com.unisono.card.lifecycle.infrastructure.adapter.input.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para activar tarjeta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para activar una tarjeta")
public class ActivateCardRequest {

    @NotBlank(message = "La razón es obligatoria")
    @Schema(description = "Razón de activación", example = "Cliente solicitó activación vía app")
    private String reason;

    @NotBlank(message = "El usuario es obligatorio")
    @Schema(description = "Usuario que activa", example = "cliente@email.com")
    private String activatedBy;
}
