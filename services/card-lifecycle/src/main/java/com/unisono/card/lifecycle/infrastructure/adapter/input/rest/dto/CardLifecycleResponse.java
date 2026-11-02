package com.unisono.card.lifecycle.infrastructure.adapter.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO genérico para operaciones de lifecycle
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de operaciones de ciclo de vida de tarjeta")
public class CardLifecycleResponse {

    @Schema(description = "ID de la tarjeta", example = "uuid-xxx")
    private String cardId;

    @Schema(description = "Mensaje de resultado", example = "Tarjeta activada exitosamente")
    private String message;

    @Schema(description = "Estado actual de la tarjeta", example = "ACTIVE")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp de la operación")
    private LocalDateTime timestamp;
}
