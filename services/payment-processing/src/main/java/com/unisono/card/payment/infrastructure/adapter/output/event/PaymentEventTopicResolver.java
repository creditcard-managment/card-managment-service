package com.unisono.card.payment.infrastructure.adapter.output.event;

import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.payment.domain.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves Kafka topic names for domain events
 */
@Slf4j
@Component
public class PaymentEventTopicResolver {

    @Value("${spring.kafka.topics.payment.initiated:payment.processing.initiated}")
    private String paymentInitiatedTopic;

    @Value("${spring.kafka.topics.payment.authorized:payment.processing.authorized}")
    private String paymentAuthorizedTopic;

    @Value("${spring.kafka.topics.payment.processed:payment.processing.completed}")
    private String paymentProcessedTopic;

    @Value("${spring.kafka.topics.payment.declined:payment.processing.declined}")
    private String paymentDeclinedTopic;

    @Value("${spring.kafka.topics.payment.reversed:payment.reversal.completed}")
    private String paymentReversedTopic;

    @Value("${spring.kafka.topics.refund.initiated:payment.refund.initiated}")
    private String refundInitiatedTopic;

    @Value("${spring.kafka.topics.refund.processed:payment.refund.processed}")
    private String refundProcessedTopic;

    @Value("${spring.kafka.topics.transaction.settled:payment.settlement.completed}")
    private String transactionSettledTopic;

    @Value("${spring.kafka.topics.payment.default:payment.events}")
    private String defaultTopic;

    /**
     * Resolve topic name based on event type (Java 17 compatible)
     */
    public String resolveTopicForEvent(DomainEvent event) {
        String topic;

        if (event instanceof PaymentInitiated) {
            topic = paymentInitiatedTopic;
        } else if (event instanceof PaymentAuthorized) {
            topic = paymentAuthorizedTopic;
        } else if (event instanceof PaymentProcessed) {
            topic = paymentProcessedTopic;
        } else if (event instanceof PaymentDeclined) {
            topic = paymentDeclinedTopic;
        } else if (event instanceof PaymentReversed) {
            topic = paymentReversedTopic;
        } else if (event instanceof RefundInitiated) {
            topic = refundInitiatedTopic;
        } else if (event instanceof RefundProcessed) {
            topic = refundProcessedTopic;
        } else if (event instanceof TransactionSettled) {
            topic = transactionSettledTopic;
        } else {
            log.warn("Unknown event type: {}, using default topic", event.getEventType());
            topic = defaultTopic;
        }

        log.trace("Resolved topic {} for event type {}", topic, event.getEventType());
        return topic;
    }
}
