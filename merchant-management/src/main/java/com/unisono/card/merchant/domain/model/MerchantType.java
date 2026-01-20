package com.unisono.card.merchant.domain.model;

import lombok.Getter;

/**
 * Tipo de comercio según canal de venta
 */
@Getter
public enum MerchantType {

    PHYSICAL_STORE("Tienda Física", "Physical store with POS terminals", true, false),
    ONLINE("E-commerce", "Online store without physical presence", false, true),
    HYBRID("Híbrido", "Both physical and online presence", true, true),
    MOBILE("Móvil", "Mobile-first business (food trucks, delivery, etc.)", true, false),
    MARKETPLACE("Marketplace", "Platform connecting buyers and sellers", false, true),
    SUBSCRIPTION("Suscripción", "Recurring payment model", false, true);

    private final String displayName;
    private final String description;
    private final boolean requiresPhysicalTerminal;
    private final boolean requiresOnlineGateway;

    MerchantType(String displayName, String description, boolean requiresPhysicalTerminal, boolean requiresOnlineGateway) {
        this.displayName = displayName;
        this.description = description;
        this.requiresPhysicalTerminal = requiresPhysicalTerminal;
        this.requiresOnlineGateway = requiresOnlineGateway;
    }

    public boolean canAcceptCardPresent() {
        return requiresPhysicalTerminal;
    }

    public boolean canAcceptCardNotPresent() {
        return requiresOnlineGateway;
    }

    public boolean isFullyDigital() {
        return !requiresPhysicalTerminal && requiresOnlineGateway;
    }
}
