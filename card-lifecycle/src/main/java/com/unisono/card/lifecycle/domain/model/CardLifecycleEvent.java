package com.unisono.card.lifecycle.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Eventos que disparan transiciones de estado
 */
@Getter
@RequiredArgsConstructor
public enum CardLifecycleEvent {

    APPROVE("Aprobar"),
    REJECT("Rechazar"),
    ISSUE("Emitir"),
    ACTIVATE("Activar"),
    BLOCK("Bloquear"),
    BLOCK_FRAUD("Bloquear por Fraude"),
    UNBLOCK("Desbloquear"),
    CANCEL("Cancelar"),
    EXPIRE("Expirar");

    private final String description; // ✅ IMPORTANTE
}