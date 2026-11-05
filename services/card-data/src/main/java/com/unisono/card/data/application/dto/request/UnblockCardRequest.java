package com.unisono.card.data.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Request: Desbloquear tarjeta (body opcional)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para desbloquear una tarjeta")
public class UnblockCardRequest {

    @Size(max = 500, message = "La razón no puede exceder 500 caracteres")
    @Schema(description = "Razón del desbloqueo", example = "El cliente recuperó la tarjeta")
    private String reason;

    @Size(max = 100, message = "El autor no puede exceder 100 caracteres")
    @Schema(description = "Quién ejecuta el cambio; se publica en el evento", example = "admin@bank.com")
    private String performedBy;
}
