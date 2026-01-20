package com.unisono.card.data.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReplaceCardRequest {

    @NotBlank(message = "La razón del reemplazo es obligatoria")
    private String reason; // "STOLEN", "LOST", "DAMAGED", "EXPIRED"
}
