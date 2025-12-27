package com.unisono.card.data.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para bloquear una tarjeta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para bloquear una tarjeta")
public class BlockCardRequest {

    @NotBlank(message = "La razón del bloqueo es obligatoria")
    @Size(max = 500, message = "La razón no puede exceder 500 caracteres")
    @Schema(description = "Razón del bloqueo", example = "Pérdida de tarjeta")
    private String reason;
}
