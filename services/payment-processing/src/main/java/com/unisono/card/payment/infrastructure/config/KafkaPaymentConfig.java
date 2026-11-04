package com.unisono.card.payment.infrastructure.config;

import com.unisono.card.events.domain.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for payment processing events
 */
@Slf4j
@Configuration
public class KafkaPaymentConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.acks:all}")
    private String acks;

    @Value("${spring.kafka.producer.retries:3}")
    private Integer retries;

    @Value("${spring.kafka.producer.compression-type:lz4}")
    private String compressionType;

    @Value("${spring.kafka.topics.partitions:3}")
    private Integer defaultPartitions;

    @Value("${spring.kafka.topics.replication-factor:2}")
    private Short defaultReplicationFactor;

    /**
     * Producer factory configuration
     */
    @Bean
    public ProducerFactory<String, DomainEvent> paymentEventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Connection
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serialization
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Reliability
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Performance
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        // JSON Serializer configuration
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        configProps.put(JsonSerializer.TYPE_MAPPINGS, buildTypeMappings());

        log.info("Configured Kafka producer for payment events: {}", bootstrapServers);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka Template for publishing events
     */
    @Bean
    public KafkaTemplate<String, DomainEvent> kafkaTemplate(
            ProducerFactory<String, DomainEvent> producerFactory) {

        KafkaTemplate<String, DomainEvent> template = new KafkaTemplate<>(producerFactory);

        // Enable observation for metrics/tracing
        template.setObservationEnabled(true);

        log.info("Created KafkaTemplate for payment events");

        return template;
    }

    // ==================== Topic Definitions ====================

    @Bean
    public NewTopic paymentInitiatedTopic(
            @Value("${spring.kafka.topics.payment.initiated:payment.processing.initiated}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic paymentAuthorizedTopic(
            @Value("${spring.kafka.topics.payment.authorized:payment.processing.authorized}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic paymentProcessedTopic(
            @Value("${spring.kafka.topics.payment.processed:payment.processing.completed}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic paymentDeclinedTopic(
            @Value("${spring.kafka.topics.payment.declined:payment.processing.declined}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic paymentReversedTopic(
            @Value("${spring.kafka.topics.payment.reversed:payment.reversal.completed}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic refundInitiatedTopic(
            @Value("${spring.kafka.topics.refund.initiated:payment.refund.initiated}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic refundProcessedTopic(
            @Value("${spring.kafka.topics.refund.processed:payment.refund.processed}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic transactionSettledTopic(
            @Value("${spring.kafka.topics.transaction.settled:payment.settlement.completed}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic transactionCompletedTopic(
            @Value("${spring.kafka.topics.transaction.completed:payment.transaction.completed}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic fraudDetectedTopic(
            @Value("${spring.kafka.topics.fraud.detected:payment.fraud.detected}") String topicName) {
        return createTopic(topicName);
    }

    /**
     * Create a topic with default configuration
     */
    private NewTopic createTopic(String topicName) {
        log.info("Creating Kafka topic: {} with {} partitions and replication factor {}",
                topicName, defaultPartitions, defaultReplicationFactor);

        return TopicBuilder.name(topicName)
                .partitions(defaultPartitions)
                .replicas(defaultReplicationFactor.intValue())
                .build();
    }

    /**
     * Build type mappings for JSON serialization
     */
    private String buildTypeMappings() {
        return "PaymentInitiated:com.unisono.card.payment.domain.event.PaymentInitiated," +
                "PaymentAuthorized:com.unisono.card.payment.domain.event.PaymentAuthorized," +
                "PaymentProcessed:com.unisono.card.payment.domain.event.PaymentProcessed," +
                "PaymentDeclined:com.unisono.card.payment.domain.event.PaymentDeclined," +
                "PaymentReversed:com.unisono.card.payment.domain.event.PaymentReversed," +
                "RefundInitiated:com.unisono.card.payment.domain.event.RefundInitiated," +
                "RefundProcessed:com.unisono.card.payment.domain.event.RefundProcessed," +
                "TransactionSettled:com.unisono.card.payment.domain.event.TransactionSettled," +
                "TransactionCompleted:com.unisono.card.payment.domain.event.TransactionCompleted," +
                "FraudDetected:com.unisono.card.payment.domain.event.FraudDetected";
    }
}
