package com.unisono.card.data.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO para registrar la fecha de activación de una tarjeta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para registrar la fecha de activación")
public class UpdateActivatedAtRequest {

    @NotNull(message = "La fecha de activación es obligatoria")
    @Schema(description = "Fecha y hora de activación (ISO-8601)", example = "2026-11-02T10:15:30")
    private LocalDateTime activatedAt;
}
