package com.unisono.card.lifecycle.infrastructure.adapter.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO: Respuesta con información de una transición de estado
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información de una transición de estado")
public class StateTransitionResponse {

    @Schema(description = "ID de la transición", example = "1")
    private Long id;

    @Schema(description = "ID de la tarjeta", example = "550e8400-e29b-41d4-a716-446655440001")
    private String cardId;

    @Schema(description = "Estado origen", example = "REQUESTED")
    private String fromState; // ✅ String

    @Schema(description = "Descripción del estado origen", example = "Solicitada")
    private String fromStateDescription; // ✅ NUEVO

    @Schema(description = "Estado destino", example = "APPROVED")
    private String toState; // ✅ String

    @Schema(description = "Descripción del estado destino", example = "Aprobada")
    private String toStateDescription; // ✅ NUEVO

    @Schema(description = "Evento que disparó la transición", example = "APPROVE")
    private String event; // ✅ String

    @Schema(description = "Descripción del evento", example = "Aprobar")
    private String eventDescription; // ✅ NUEVO

    @Schema(description = "Razón de la transición", example = "Cliente cumple requisitos")
    private String reason;

    @Schema(description = "Usuario que ejecutó la transición", example = "admin@bank.com")
    private String performedBy;

    @Schema(description = "Fecha y hora de la transición", example = "2025-12-28T16:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime transitionedAt;

    @Schema(description = "Indica si la transición fue exitosa", example = "true")
    private Boolean successful;

    @Schema(description = "Mensaje de error (si falló)", example = "Cannot activate card")
    private String errorMessage;

    @Schema(description = "Descripción legible de la transición completa")
    private String description;
}
