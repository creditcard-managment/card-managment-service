package com.unisono.card.lifecycle.domain.model;

import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Modelo de dominio: Transición de estado
 */
@Getter
@Builder // ✅ Builder público
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Constructor sin args protegido
public class StateTransition {

    private Long id;
    private CardId cardId;
    private String fromState;  // String (puede ser null en primera transición)
    private String toState;    // String
    private String event;      // String
    private String reason;
    private String performedBy;
    private LocalDateTime transitionedAt;
    private Boolean successful;
    private String errorMessage;

    /**
     * Factory method para crear una transición exitosa
     */
    public static StateTransition createSuccessful(
            CardId cardId,
            String fromState,
            String toState,
            String event,
            String reason,
            String performedBy
    ) {
        return StateTransition.builder()
                .cardId(cardId)
                .fromState(fromState)
                .toState(toState)
                .event(event)
                .reason(reason)
                .performedBy(performedBy)
                .transitionedAt(LocalDateTime.now())
                .successful(true)
                .build();
    }

    /**
     * Factory method para una transición fallida que conoce el estado destino buscado.
     * {@code toState} es el destino que se intentó alcanzar (la columna es NOT NULL);
     * {@code successful = false} indica que no se llegó a él.
     */
    public static StateTransition createFailed(
            CardId cardId,
            String fromState,
            String intendedToState,
            String event,
            String reason,
            String errorMessage,
            String performedBy
    ) {
        return StateTransition.builder()
                .cardId(cardId)
                .fromState(fromState)
                .toState(intendedToState)
                .event(event)
                .reason(reason)
                .performedBy(performedBy)
                .transitionedAt(LocalDateTime.now())
                .successful(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Obtiene una descripción legible de la transición
     */
    public String getDescription() {
        if (successful) {
            return String.format(
                    "Tarjeta %s: %s → %s (Evento: %s) - Razón: %s - Por: %s",
                    cardId.getValue(),
                    fromState != null ? getStateDescription(fromState) : "N/A",
                    toState != null ? getStateDescription(toState) : "N/A",
                    getEventDescription(event),
                    reason != null ? reason : "N/A",
                    performedBy
            );
        } else {
            return String.format(
                    "Tarjeta %s: Transición fallida desde %s (Evento: %s) - Error: %s",
                    cardId.getValue(),
                    fromState != null ? getStateDescription(fromState) : "N/A",
                    getEventDescription(event),
                    errorMessage
            );
        }
    }

    /**
     * Convierte String de estado a descripción legible
     */
    private String getStateDescription(String state) {
        try {
            return CardStatus.valueOf(state).getDescription();
        } catch (IllegalArgumentException e) {
            return state;
        }
    }

    /**
     * Convierte String de evento a descripción legible
     */
    private String getEventDescription(String eventName) {
        try {
            return CardLifecycleEvent.valueOf(eventName).getDescription();
        } catch (IllegalArgumentException e) {
            return eventName;
        }
    }

    /**
     * Verifica si la transición fue exitosa
     */
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(successful);
    }

    /**
     * Verifica si la transición falló
     */
    public boolean isFailed() {
        return Boolean.FALSE.equals(successful);
    }
}
