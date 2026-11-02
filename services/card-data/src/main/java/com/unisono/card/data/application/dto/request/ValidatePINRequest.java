package com.unisono.card.data.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para validar PIN
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para validar PIN de una tarjeta")
public class ValidatePINRequest {

    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "^\\d{4}$", message = "El PIN debe tener 4 dígitos")
    @Schema(description = "PIN de 4 dígitos", example = "1234")
    private String pin;
}
