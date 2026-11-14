package com.unisono.card.payment.infrastructure.config;

import com.unisono.card.events.config.KafkaTopics;
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
    public ProducerFactory<String, Object> paymentEventProducerFactory() {
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

        // Timeouts acotados: el productor debe resolver cada envío (éxito o error) antes de que
        // PaymentOutboxRelay deje de esperar (payment.outbox.send-timeout-ms). Así un envío fallido
        // no queda en el buffer para entregarse más tarde y duplicarse con el reintento del relay.
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);

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
     * Kafka Template used by PaymentOutboxRelay (sends the JSON stored in the outbox)
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {

        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);

        // Enable observation for metrics/tracing
        template.setObservationEnabled(true);

        log.info("Created KafkaTemplate for payment events");

        return template;
    }

    // ==================== Topic Definitions ====================

    @Bean
    public NewTopic paymentInitiatedTopic(
            @Value("${spring.kafka.topics.payment.initiated:" + KafkaTopics.PAYMENT_INITIATED + "}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic paymentAuthorizedTopic(
            @Value("${spring.kafka.topics.payment.authorized:" + KafkaTopics.PAYMENT_AUTHORIZED + "}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic paymentProcessedTopic(
            @Value("${spring.kafka.topics.payment.processed:" + KafkaTopics.PAYMENT_PROCESSED + "}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic paymentDeclinedTopic(
            @Value("${spring.kafka.topics.payment.declined:" + KafkaTopics.PAYMENT_DECLINED + "}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic paymentReversedTopic(
            @Value("${spring.kafka.topics.payment.reversed:" + KafkaTopics.PAYMENT_REVERSED + "}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic refundInitiatedTopic(
            @Value("${spring.kafka.topics.refund.initiated:" + KafkaTopics.REFUND_INITIATED + "}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic refundProcessedTopic(
            @Value("${spring.kafka.topics.refund.processed:" + KafkaTopics.REFUND_PROCESSED + "}") String topicName) {
        return createTopic(topicName);
    }

    @Bean
    public NewTopic transactionSettledTopic(
            @Value("${spring.kafka.topics.transaction.settled:" + KafkaTopics.TRANSACTION_SETTLED + "}") String topicName) {
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
                "TransactionSettled:com.unisono.card.payment.domain.event.TransactionSettled";
    }
}
