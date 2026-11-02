package com.unisono.card.payment.domain.model;

/**
 * Enum representing the status of a transaction throughout its lifecycle
 */
public enum TransactionStatus {

    /**
     * Transaction initiated but not yet processed
     */
    PENDING("Pendiente"),

    /**
     * Transaction authorized (funds reserved)
     */
    AUTHORIZED("Autorizada"),

    /**
     * Transaction approved and processed
     */
    APPROVED("Aprobada"),

    /**
     * Transaction declined/rejected
     */
    DECLINED("Rechazada"),

    /**
     * Transaction reversed/cancelled
     */
    REVERSED("Reversada"),

    /**
     * Transaction settled (money transferred)
     */
    SETTLED("Liquidada"),

    /**
     * Transaction failed due to error
     */
    FAILED("Fallida");

    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isFinal() {
        return this == APPROVED || this == DECLINED || this == REVERSED || this == SETTLED || this == FAILED;
    }

    public boolean canBeReversed() {
        return this == AUTHORIZED || this == APPROVED;
    }

    public boolean canBeSettled() {
        return this == APPROVED;
    }
}
