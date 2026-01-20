package com.unisono.card.merchant.domain.model.terminal;

import lombok.Getter;

/**
 * Estados del ciclo de vida de una terminal
 */
@Getter
public enum TerminalStatus {

    INACTIVE(
            "Inactiva",
            "Terminal registered but not yet activated",
            false
    ),

    ACTIVE(
            "Activa",
            "Terminal is active and can process transactions",
            true
    ),

    SUSPENDED(
            "Suspendida",
            "Temporarily suspended, cannot process transactions",
            false
    ),

    MAINTENANCE(
            "Mantenimiento",
            "Under maintenance, temporarily unavailable",
            false
    ),

    BLOCKED(
            "Bloqueada",
            "Blocked due to security concerns or fraud",
            false
    ),

    DECOMMISSIONED(
            "Dada de Baja",
            "Permanently decommissioned, cannot be reactivated",
            false
    );

    private final String displayName;
    private final String description;
    private final boolean operational;

    TerminalStatus(String displayName, String description, boolean operational) {
        this.displayName = displayName;
        this.description = description;
        this.operational = operational;
    }

    public boolean canProcessTransactions() {
        return this == ACTIVE;
    }

    public boolean isFinal() {
        return this == DECOMMISSIONED;
    }

    public boolean canTransitionTo(TerminalStatus newStatus) {
        switch (this) {
            case INACTIVE:
                return newStatus == ACTIVE || newStatus == DECOMMISSIONED;
            case ACTIVE:
                return newStatus == SUSPENDED || newStatus == MAINTENANCE ||
                        newStatus == BLOCKED || newStatus == DECOMMISSIONED;
            case SUSPENDED:
                return newStatus == ACTIVE || newStatus == BLOCKED || newStatus == DECOMMISSIONED;
            case MAINTENANCE:
                return newStatus == ACTIVE || newStatus == DECOMMISSIONED;
            case BLOCKED:
                return newStatus == DECOMMISSIONED;
            case DECOMMISSIONED:
                return false; // Estado final
            default:
                return false;
        }
    }
}
