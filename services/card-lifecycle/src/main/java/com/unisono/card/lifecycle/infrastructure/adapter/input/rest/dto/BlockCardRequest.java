package com.unisono.card.lifecycle.infrastructure.adapter.input.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para bloquear una tarjeta")
public class BlockCardRequest {

    @Schema(description = "Razón del bloqueo", example = "Tarjeta reportada como perdida")
    @NotBlank(message = "La razón es obligatoria")
    private String reason;

    @Schema(description = "Usuario que bloquea", example = "usuario@email.com")
    @NotBlank(message = "El usuario que bloquea es obligatorio")
    private String blockedBy;

    @Schema(
            description = "Tipo de bloqueo",
            example = "NORMAL",
            allowableValues = {"NORMAL", "FRAUD"},
            defaultValue = "NORMAL"
    )
    private String blockType; // ✅ Campo necesario (puede ser null, default = NORMAL)
}
