package com.unisono.card.lifecycle.infrastructure.adapter.input.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para cancelar tarjeta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para cancelar una tarjeta permanentemente")
public class CancelCardRequest {

    @NotBlank(message = "La razón de cancelación es obligatoria")
    @Schema(description = "Razón de cancelación", example = "Cliente solicitó cancelación")
    private String reason;

    @NotBlank(message = "El usuario que cancela es obligatorio")
    @Schema(description = "Usuario que cancela", example = "cliente@email.com")
    private String cancelledBy;
}
