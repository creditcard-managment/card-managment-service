package com.unisono.card.data.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Request: Bloquear tarjeta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para bloquear una tarjeta")
public class BlockCardRequest {

    @Schema(
            description = "Razón del bloqueo",
            example = "Tarjeta reportada como perdida por el cliente"
    )
    @NotBlank(message = "La razón del bloqueo es obligatoria")
    private String reason;
}
