package com.unisono.card.merchant.domain.model.contract;

import lombok.Getter;

/**
 * Niveles de servicio disponibles
 */
@Getter
public enum ServiceTier {

    BASIC("Básico", "Basic service level for small merchants", 0),
    STANDARD("Estándar", "Standard service level for medium merchants", 1),
    PREMIUM("Premium", "Premium service level with dedicated support", 2),
    ENTERPRISE("Empresarial", "Enterprise level with custom SLA", 3);

    private final String displayName;
    private final String description;
    private final int priority;

    ServiceTier(String displayName, String description, int priority) {
        this.displayName = displayName;
        this.description = description;
        this.priority = priority;
    }

    public boolean isHigherThan(ServiceTier other) {
        return this.priority > other.priority;
    }

    public boolean isAtLeast(ServiceTier minimumTier) {
        return this.priority >= minimumTier.priority;
    }
}
