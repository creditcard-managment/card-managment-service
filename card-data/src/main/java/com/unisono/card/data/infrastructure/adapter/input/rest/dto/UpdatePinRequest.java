package com.unisono.card.data.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePinRequest {

    @NotBlank(message = "El PIN actual es obligatorio")
    @Pattern(regexp = "^\\d{4}$", message = "El PIN debe tener exactamente 4 dígitos")
    private String currentPin;

    @NotBlank(message = "El nuevo PIN es obligatorio")
    @Pattern(regexp = "^\\d{4}$", message = "El PIN debe tener exactamente 4 dígitos")
    private String newPin;
}
