package com.unisono.card.merchant.infrastructure.adapter.messaging.kafka.config;

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

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de Kafka para el módulo Merchant
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ==========================================
    // Topic Definitions
    // ==========================================

    @Bean
    public NewTopic merchantRegisteredTopic() {
        return TopicBuilder.name("merchant.events.merchantregistered")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic merchantActivatedTopic() {
        return TopicBuilder.name("merchant.events.merchantactivated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic merchantSuspendedTopic() {
        return TopicBuilder.name("merchant.events.merchantsuspended")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic terminalActivatedTopic() {
        return TopicBuilder.name("merchant.events.terminalactivated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic terminalDeactivatedTopic() {
        return TopicBuilder.name("merchant.events.terminaldeactivated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic feeStructureUpdatedTopic() {
        return TopicBuilder.name("merchant.events.feestructureupdated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic contractRenewedTopic() {
        return TopicBuilder.name("merchant.events.contractrenewed")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
