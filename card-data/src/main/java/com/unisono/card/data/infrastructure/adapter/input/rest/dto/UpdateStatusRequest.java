package com.unisono.card.data.infrastructure.adapter.input.rest.dto;

import com.unisono.card.shared.domain.valueobject.CardStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "El estado es obligatorio")
    private CardStatus status;

    private String reason; // Opcional: razón del cambio de estado
}