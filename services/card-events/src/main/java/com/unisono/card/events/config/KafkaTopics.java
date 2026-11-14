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
    public static final String CARD_EXPIRED = "card.expired";

    // ========================================
    // Topics de Credit Limits (sin productor: card-limits no publica eventos)
    // ========================================

    public static final String CREDIT_LIMIT_ASSIGNED = "credit-limit.assigned";
    public static final String CREDIT_LIMIT_UPDATED = "credit-limit.updated";
    public static final String CREDIT_LIMIT_CONSUMED = "credit-limit.consumed";
    public static final String CREDIT_LIMIT_RELEASED = "credit-limit.released";

    // ========================================
    // Topics de payment-processing (los publica su outbox)
    // ========================================

    public static final String PAYMENT_INITIATED = "payment.processing.initiated";
    public static final String PAYMENT_AUTHORIZED = "payment.processing.authorized";
    public static final String PAYMENT_PROCESSED = "payment.processing.completed";
    public static final String PAYMENT_DECLINED = "payment.processing.declined";
    public static final String PAYMENT_REVERSED = "payment.reversal.completed";
    public static final String REFUND_INITIATED = "payment.refund.initiated";
    public static final String REFUND_PROCESSED = "payment.refund.processed";
    public static final String TRANSACTION_SETTLED = "payment.settlement.completed";
    /** Destino de un evento de pago sin topic propio (no debería usarse) */
    public static final String PAYMENT_DEFAULT = "payment.events";

    // ========================================
    // Dead Letter Queue (manejo de errores)
    // ========================================

    public static final String DLQ_CARD_EVENTS = "dlq.card-events";
    /** Eventos de pago que card-data no pudo procesar (bloqueo por fraude) */
    public static final String DLQ_PAYMENT_EVENTS = "dlq.payment-events";
    /** Sin uso: card-limits no consume eventos */
    public static final String DLQ_LIMIT_EVENTS = "dlq.limit-events";
}
