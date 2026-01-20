package com.unisono.card.merchant.domain.model.fees;

import lombok.Getter;

/**
 * Tipos de estructura de fees
 */
@Getter
public enum FeeType {

    PERCENTAGE(
            "Porcentaje",
            "Fee calculated as a percentage of transaction amount"
    ),

    FIXED(
            "Fijo",
            "Fixed fee amount regardless of transaction size"
    ),

    PERCENTAGE_PLUS_FIXED(
            "Porcentaje + Fijo",
            "Combination of percentage and fixed fee"
    ),

    TIERED(
            "Escalonado",
            "Different rates for different transaction amount tiers"
    ),

    INTERCHANGE_PLUS(
            "Interchange Plus",
            "Interchange rate plus a markup"
    );

    private final String displayName;
    private final String description;

    FeeType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isVariableRate() {
        return this == PERCENTAGE ||
                this == PERCENTAGE_PLUS_FIXED ||
                this == TIERED ||
                this == INTERCHANGE_PLUS;
    }
}
