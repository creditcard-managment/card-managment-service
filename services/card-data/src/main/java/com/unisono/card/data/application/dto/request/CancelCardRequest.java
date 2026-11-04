package com.unisono.card.data.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para cancelar una tarjeta (usado por card-lifecycle)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para cancelar una tarjeta")
public class CancelCardRequest {

    @NotBlank(message = "La razón de la cancelación es obligatoria")
    @Size(max = 500, message = "La razón no puede exceder 500 caracteres")
    @Schema(description = "Razón de la cancelación", example = "Cliente solicitó cancelación")
    private String reason;
}
