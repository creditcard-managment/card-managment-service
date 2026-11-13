package com.unisono.card.payment.infrastructure.adapter.output.event;

import com.unisono.card.events.config.KafkaTopics;
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

    @Value("${spring.kafka.topics.payment.initiated:" + KafkaTopics.PAYMENT_INITIATED + "}")
    private String paymentInitiatedTopic;

    @Value("${spring.kafka.topics.payment.authorized:" + KafkaTopics.PAYMENT_AUTHORIZED + "}")
    private String paymentAuthorizedTopic;

    @Value("${spring.kafka.topics.payment.processed:" + KafkaTopics.PAYMENT_PROCESSED + "}")
    private String paymentProcessedTopic;

    @Value("${spring.kafka.topics.payment.declined:" + KafkaTopics.PAYMENT_DECLINED + "}")
    private String paymentDeclinedTopic;

    @Value("${spring.kafka.topics.payment.reversed:" + KafkaTopics.PAYMENT_REVERSED + "}")
    private String paymentReversedTopic;

    @Value("${spring.kafka.topics.refund.initiated:" + KafkaTopics.REFUND_INITIATED + "}")
    private String refundInitiatedTopic;

    @Value("${spring.kafka.topics.refund.processed:" + KafkaTopics.REFUND_PROCESSED + "}")
    private String refundProcessedTopic;

    @Value("${spring.kafka.topics.transaction.settled:" + KafkaTopics.TRANSACTION_SETTLED + "}")
    private String transactionSettledTopic;

    @Value("${spring.kafka.topics.payment.default:" + KafkaTopics.PAYMENT_DEFAULT + "}")
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
