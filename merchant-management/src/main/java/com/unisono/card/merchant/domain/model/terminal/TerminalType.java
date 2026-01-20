package com.unisono.card.merchant.domain.model.terminal;

import lombok.Getter;

/**
 * Tipos de terminales de pago
 */
@Getter
public enum TerminalType {

    POS(
            "POS",
            "Point of Sale Terminal",
            "Terminal de Punto de Venta físico",
            true,
            false
    ),

    VIRTUAL(
            "Virtual",
            "Virtual Terminal",
            "Terminal virtual para pagos online",
            false,
            true
    ),

    MOBILE(
            "Móvil",
            "Mobile Terminal",
            "Terminal móvil (smartphone/tablet)",
            true,
            false
    ),

    MPOS(
            "mPOS",
            "Mobile Point of Sale",
            "Terminal móvil con lector externo",
            true,
            false
    ),

    UNATTENDED(
            "Desatendido",
            "Unattended Terminal",
            "Terminal automático (kiosco, vending machine)",
            true,
            false
    ),

    GATEWAY(
            "Gateway",
            "Payment Gateway",
            "Gateway de pagos para integración API",
            false,
            true
    );

    private final String displayName;
    private final String description;
    private final String descriptionEs;
    private final boolean cardPresent;
    private final boolean cardNotPresent;

    TerminalType(String displayName, String description, String descriptionEs,
                 boolean cardPresent, boolean cardNotPresent) {
        this.displayName = displayName;
        this.description = description;
        this.descriptionEs = descriptionEs;
        this.cardPresent = cardPresent;
        this.cardNotPresent = cardNotPresent;
    }

    public boolean requiresPhysicalDevice() {
        return cardPresent;
    }

    public boolean isOnlineOnly() {
        return cardNotPresent && !cardPresent;
    }
}
