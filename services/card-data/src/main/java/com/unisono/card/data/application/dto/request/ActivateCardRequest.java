package com.unisono.card.data.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para activar una tarjeta")
public class ActivateCardRequest {

    @Schema(description = "PIN para activación (4-6 dígitos)", example = "1234")
    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "^\\d{4,6}$", message = "PIN debe tener entre 4 y 6 dígitos")
    private String pin;
}
