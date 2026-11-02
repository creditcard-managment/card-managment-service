package com.unisono.card.shared.domain.valueobject;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estados del ciclo de vida de una tarjeta
 */
@Getter
@RequiredArgsConstructor
public enum CardStatus {

    // Estados iniciales
    REQUESTED("Solicitada"),
    APPROVED("Aprobada"),
    REJECTED("Rechazada"),

    // Estados operativos
    ISSUED("Emitida"),
    ACTIVE("Activa"),

    // Estados de bloqueo
    BLOCKED("Bloqueada"),
    BLOCKED_FRAUD("Bloqueada por Fraude"),

    // Estados finales
    CANCELLED("Cancelada"),
    EXPIRED("Vencida");

    private final String description;

    /**
     * Verifica si la tarjeta está en un estado operativo
     */
    public boolean isOperational() {
        return this == ACTIVE || this == ISSUED;
    }

    /**
     * Verifica si la tarjeta está bloqueada
     */
    public boolean isBlocked() {
        return this == BLOCKED || this == BLOCKED_FRAUD;
    }

    /**
     * Verifica si la tarjeta está en un estado final
     */
    public boolean isFinal() {
        return this == CANCELLED || this == REJECTED || this == EXPIRED;
    }

    /**
     * Verifica si se puede activar la tarjeta
     */
    public boolean canActivate() {
        return this == ISSUED;
    }

    /**
     * Verifica si se puede bloquear la tarjeta
     */
    public boolean canBlock() {
        return this == ACTIVE || this == ISSUED;
    }

    /**
     * Verifica si se puede pasar de este estado al estado destino
     */
    public boolean canTransitionTo(CardStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case REQUESTED -> target == APPROVED || target == REJECTED;
            case APPROVED -> target == ISSUED || target == CANCELLED;
            case ISSUED -> target == ACTIVE || target.isBlocked() || target == CANCELLED;
            case ACTIVE -> target.isBlocked() || target == EXPIRED || target == CANCELLED;
            case BLOCKED, BLOCKED_FRAUD -> target == ACTIVE || target == CANCELLED;
            case REJECTED, CANCELLED, EXPIRED -> false;
        };
    }
}
