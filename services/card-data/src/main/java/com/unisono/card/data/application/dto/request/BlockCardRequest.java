package com.unisono.card.data.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @Size(max = 100, message = "El autor no puede exceder 100 caracteres")
    @Schema(description = "Quién ejecuta el cambio; se publica en el evento", example = "admin@bank.com")
    private String performedBy;
}
