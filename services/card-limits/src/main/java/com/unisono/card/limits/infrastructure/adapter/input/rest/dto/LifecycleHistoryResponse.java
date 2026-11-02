package com.unisono.card.limits.infrastructure.adapter.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO: Respuesta con el historial de una transición de estado
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Historial de transición de estado de una tarjeta")
public class LifecycleHistoryResponse {

    @Schema(description = "ID de la transición", example = "1")
    private Long id;

    @Schema(description = "Estado origen", example = "REQUESTED")
    private String fromState;

    @Schema(description = "Descripción del estado origen", example = "Solicitada")
    private String fromStateDescription;

    @Schema(description = "Estado destino", example = "APPROVED")
    private String toState;

    @Schema(description = "Descripción del estado destino", example = "Aprobada")
    private String toStateDescription;

    @Schema(description = "Evento que disparó la transición", example = "APPROVE")
    private String event;

    @Schema(description = "Descripción del evento", example = "Aprobar")
    private String eventDescription;

    @Schema(description = "Razón de la transición", example = "Cliente cumple con todos los requisitos")
    private String reason;

    @Schema(description = "Usuario que ejecutó la transición", example = "admin@bank.com")
    private String performedBy;

    @Schema(description = "Fecha y hora de la transición", example = "2025-12-28T16:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime transitionedAt;

    @Schema(description = "Indica si la transición fue exitosa", example = "true")
    private Boolean successful;

    @Schema(description = "Mensaje de error (si la transición falló)", example = "Cannot activate card: Credit limit not assigned")
    private String errorMessage;
}
