package com.unisono.card.lifecycle.infrastructure.adapter.input.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para rechazar una tarjeta")
public class RejectCardRequest {

    @Schema(description = "Razón del rechazo", example = "Cliente no cumple requisitos crediticios")
    @NotBlank(message = "La razón es obligatoria")
    private String reason;

    @Schema(description = "Usuario que rechaza", example = "admin@bank.com")
    @NotBlank(message = "El usuario que rechaza es obligatorio")
    private String rejectedBy;
}