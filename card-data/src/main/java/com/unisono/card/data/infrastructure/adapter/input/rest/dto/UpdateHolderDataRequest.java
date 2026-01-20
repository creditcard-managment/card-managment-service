package com.unisono.card.data.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHolderDataRequest {

    @Email(message = "El email debe ser válido")
    private String holderEmail;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Número de teléfono inválido (formato E.164)")
    private String holderPhoneNumber;

    @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
    private String holderAddress;
}

