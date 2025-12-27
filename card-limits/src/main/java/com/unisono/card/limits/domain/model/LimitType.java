package com.unisono.card.limits.domain.model;

import lombok.Getter;

/**
 * Tipo de límite para el historial
 */
@Getter
public enum LimitType {
    PURCHASE("Límite de Compras"),
    CASH_ADVANCE("Límite de Adelantos"),
    DAILY("Límite Diario"),
    MONTHLY("Límite Mensual");

    private final String displayName;

    LimitType(String displayName) {
        this.displayName = displayName;
    }
}
