package com.unisono.card.merchant.domain.model.contract;

import lombok.Getter;

/**
 * Estados del contrato
 */
@Getter
public enum ContractStatus {

    DRAFT("Borrador", "Contract is being drafted", false),
    PENDING_SIGNATURE("Pendiente de Firma", "Awaiting signatures", false),
    ACTIVE("Activo", "Contract is active and in effect", true),
    SUSPENDED("Suspendido", "Temporarily suspended", false),
    EXPIRED("Expirado", "Contract has expired", false),
    TERMINATED("Terminado", "Contract terminated before expiration", false),
    RENEWED("Renovado", "Contract has been renewed", false);

    private final String displayName;
    private final String description;
    private final boolean operational;

    ContractStatus(String displayName, String description, boolean operational) {
        this.displayName = displayName;
        this.description = description;
        this.operational = operational;
    }

    public boolean isValid() {
        return this == ACTIVE;
    }

    public boolean isFinal() {
        return this == TERMINATED || this == EXPIRED;
    }
}
