package com.unisono.card.merchant.domain.model;

import lombok.Getter;

/**
 * Estados del ciclo de vida de un Merchant
 */
@Getter
public enum MerchantStatus {

    PENDING_APPROVAL(
            "Pendiente de Aprobación",
            "Merchant registered but awaiting approval",
            false,
            true
    ),

    ACTIVE(
            "Activo",
            "Merchant approved and can process transactions",
            true,
            true
    ),

    SUSPENDED(
            "Suspendido",
            "Temporarily unable to process transactions",
            false,
            true
    ),

    UNDER_REVIEW(
            "En Revisión",
            "Under investigation for fraud or compliance issues",
            false,
            true
    ),

    CLOSED(
            "Cerrado",
            "Permanently closed, cannot be reactivated",
            false,
            false
    ),

    BLOCKED(
            "Bloqueado",
            "Blocked due to policy violation or excessive chargebacks",
            false,
            false
    );

    private final String displayName;
    private final String description;
    private final boolean canProcessTransactions;
    private final boolean canBeModified;

    MerchantStatus(String displayName, String description, boolean canProcessTransactions, boolean canBeModified) {
        this.displayName = displayName;
        this.description = description;
        this.canProcessTransactions = canProcessTransactions;
        this.canBeModified = canBeModified;
    }

    public boolean isOperational() {
        return this == ACTIVE;
    }

    public boolean isFinal() {
        return this == CLOSED || this == BLOCKED;
    }

    public boolean canTransitionTo(MerchantStatus newStatus) {
        switch (this) {
            case PENDING_APPROVAL:
                return newStatus == ACTIVE || newStatus == CLOSED;
            case ACTIVE:
                return newStatus == SUSPENDED || newStatus == UNDER_REVIEW || newStatus == CLOSED;
            case SUSPENDED:
                return newStatus == ACTIVE || newStatus == CLOSED || newStatus == BLOCKED;
            case UNDER_REVIEW:
                return newStatus == ACTIVE || newStatus == SUSPENDED || newStatus == BLOCKED;
            case CLOSED:
            case BLOCKED:
                return false; // Estados finales
            default:
                return false;
        }
    }
}
