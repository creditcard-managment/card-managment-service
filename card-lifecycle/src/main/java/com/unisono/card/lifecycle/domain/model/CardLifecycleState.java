package com.unisono.card.lifecycle.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estados del ciclo de vida de una tarjeta
 */
@Getter
@RequiredArgsConstructor
public enum CardLifecycleState {

    REQUESTED("Solicitada"),
    APPROVED("Aprobada"),
    REJECTED("Rechazada"),
    ISSUED("Emitida"),
    ACTIVE("Activa"),
    BLOCKED("Bloqueada"),
    BLOCKED_FRAUD("Bloqueada por Fraude"),
    CANCELLED("Cancelada"),
    EXPIRED("Vencida");

    private final String description;
}