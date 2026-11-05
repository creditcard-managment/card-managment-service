package com.unisono.card.data.application.dto.request;

import com.unisono.card.shared.domain.valueobject.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para actualizar el estado de una tarjeta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para cambiar el estado de una tarjeta")
public class UpdateCardStatusRequest {

    @NotNull(message = "El estado es obligatorio")
    @Schema(description = "Nuevo estado de la tarjeta", example = "APPROVED")
    private CardStatus status;

    @Size(max = 500, message = "La razón no puede exceder 500 caracteres")
    @Schema(description = "Razón del cambio (opcional)", example = "Cliente cumple requisitos")
    private String reason;

    @Size(max = 100, message = "El autor no puede exceder 100 caracteres")
    @Schema(description = "Quién ejecuta el cambio; se publica en el evento", example = "admin@bank.com")
    private String performedBy;
}
