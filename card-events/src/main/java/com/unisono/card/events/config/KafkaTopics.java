package com.unisono.card.events.config;

/**
 * Constantes para los nombres de topics de Kafka
 */
public final class KafkaTopics {

    private KafkaTopics() {
        throw new AssertionError("No instances allowed");
    }

    // ========================================
    // Topics de Card Lifecycle
    // ========================================

    public static final String CARD_CREATED = "card.created";
    public static final String CARD_APPROVED = "card.approved";
    public static final String CARD_REJECTED = "card.rejected";
    public static final String CARD_ISSUED = "card.issued";
    public static final String CARD_ACTIVATED = "card.activated";
    public static final String CARD_BLOCKED = "card.blocked";
    public static final String CARD_UNBLOCKED = "card.unblocked";
    public static final String CARD_CANCELLED = "card.cancelled";

    // ========================================
    // Topics de Credit Limits
    // ========================================

    public static final String CREDIT_LIMIT_ASSIGNED = "credit-limit.assigned";
    public static final String CREDIT_LIMIT_UPDATED = "credit-limit.updated";
    public static final String CREDIT_LIMIT_CONSUMED = "credit-limit.consumed";
    public static final String CREDIT_LIMIT_RELEASED = "credit-limit.released";

    // ========================================
    // Topics de Transacciones (futuro)
    // ========================================

    public static final String TRANSACTION_AUTHORIZED = "transaction.authorized";
    public static final String TRANSACTION_DECLINED = "transaction.declined";
    public static final String TRANSACTION_REVERSED = "transaction.reversed";

    // ========================================
    // Dead Letter Queue (manejo de errores)
    // ========================================

    public static final String DLQ_CARD_EVENTS = "dlq.card-events";
    public static final String DLQ_LIMIT_EVENTS = "dlq.limit-events";
}
