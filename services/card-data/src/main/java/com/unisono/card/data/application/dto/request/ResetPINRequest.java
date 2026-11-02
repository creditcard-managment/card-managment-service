package com.unisono.card.data.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para resetear PIN
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para resetear PIN de una tarjeta")
public class ResetPINRequest {

    @NotBlank(message = "El PIN actual es obligatorio")
    @Pattern(regexp = "^\\d{4}$", message = "El PIN actual debe tener 4 dígitos")
    @Schema(description = "PIN actual de 4 dígitos", example = "1234")
    private String oldPin;

    @NotBlank(message = "El nuevo PIN es obligatorio")
    @Pattern(regexp = "^\\d{4}$", message = "El nuevo PIN debe tener 4 dígitos")
    @Schema(description = "Nuevo PIN de 4 dígitos", example = "5678")
    private String newPin;
}
