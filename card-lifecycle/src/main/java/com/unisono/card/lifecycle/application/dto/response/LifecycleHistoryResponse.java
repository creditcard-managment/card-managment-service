package com.unisono.card.lifecycle.application.dto.response;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO para historial de transiciones
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registro histórico de transición de estado")
public class LifecycleHistoryResponse {

    @Schema(description = "ID de la transición")
    private Long id;

    @Schema(description = "Estado origen")
    private CardLifecycleState fromState;

    @Schema(description = "Descripción del estado origen")
    private String fromStateDescription;

    @Schema(description = "Estado destino")
    private CardLifecycleState toState;

    @Schema(description = "Descripción del estado destino")
    private String toStateDescription;

    @Schema(description = "Evento ejecutado")
    private CardLifecycleEvent event;

    @Schema(description = "Descripción del evento")
    private String eventDescription;

    @Schema(description = "Razón de la transición")
    private String reason;

    @Schema(description = "Usuario que ejecutó la transición")
    private String performedBy;

    @Schema(description = "Fecha y hora de la transición")
    private LocalDateTime transitionedAt;

    @Schema(description = "Indica si la transición fue exitosa")
    private Boolean successful;

    @Schema(description = "Mensaje de error (si falló)")
    private String errorMessage;
}
