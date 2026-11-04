package com.unisono.card.lifecycle.infrastructure.adapter.input.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "^\\d{4}$", message = "El PIN debe ser exactamente 4 dígitos numéricos")
    @Schema(description = "PIN de 4 dígitos; se verifica contra card-data", example = "4821")
    private String pin;
    @NotBlank(message = "La razón es obligatoria")
    @Schema(description = "Razón de activación", example = "Cliente solicitó activación vía app")
    private String reason;

    @NotBlank(message = "El usuario es obligatorio")
    @Schema(description = "Usuario que activa", example = "cliente@email.com")
    private String activatedBy;
}
