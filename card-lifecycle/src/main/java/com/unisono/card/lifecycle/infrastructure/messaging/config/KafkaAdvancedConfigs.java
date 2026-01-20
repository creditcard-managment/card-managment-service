package com.unisono.card.lifecycle.infrastructure.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * KAFKA CONSUMER CONFIGURATIONS - ADVANCED PATTERNS
 * ============================================================
 *
 * Este archivo contiene 6 configuraciones diferentes de Kafka:
 *
 * 1. AUTO_COMMIT - Commit automático (default Spring Boot)
 * 2. MANUAL - Control total del commit
 * 3. MANUAL_IMMEDIATE - Commit inmediato por mensaje
 * 4. BATCH - Procesamiento por lotes
 * 5. RETRY_DLQ - Retry con Dead Letter Queue
 * 6. TRANSACTIONAL - Procesamiento transaccional
 *
 * Cada factory está comentada para que puedas activar/desactivar
 * según el caso de uso que quieras probar.
 */
@Slf4j
@Configuration
public class KafkaAdvancedConfigs {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    // ============================================================
    // CONFIGURACIÓN BASE - Compartida por todos los consumers
    // ============================================================
    private Map<String, Object> getBaseConsumerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    // ============================================================
    // CASO 1: AUTO COMMIT
    // ============================================================
    // Uso: Procesamiento simple sin requerimientos críticos
    // Ventaja: Simplicidad
    // Desventaja: Puede perder mensajes si falla antes del commit
    // ============================================================
    @Bean(name = "autoCommitKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> autoCommitKafkaListenerContainerFactory() {
        Map<String, Object> props = getBaseConsumerConfig();
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true); // ✅ Auto commit
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000); // Cada 5 segundos
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        ConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3); // 3 threads paralelos
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);

        return factory;
    }

    // ============================================================
    // CASO 2: MANUAL COMMIT (TU CONFIGURACIÓN ACTUAL)
    // ============================================================
    // Uso: Control total del momento del commit
    // Ventaja: Máxima flexibilidad, garantiza procesamiento
    // Desventaja: Debes manejar el acknowledge explícitamente
    // ============================================================
    @Bean(name = "manualKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> manualKafkaListenerContainerFactory() {
        Map<String, Object> props = getBaseConsumerConfig();
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // ✅ Manual
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        ConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(new DefaultErrorHandler());

        return factory;
    }

    // ============================================================
    // CASO 3: MANUAL IMMEDIATE
    // ============================================================
    // Uso: Commit inmediato después de cada mensaje
    // Ventaja: Garantía de procesamiento mensaje por mensaje
    // Desventaja: Menor throughput por commits frecuentes
    // ============================================================
    @Bean(name = "manualImmediateKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> manualImmediateKafkaListenerContainerFactory() {
        Map<String, Object> props = getBaseConsumerConfig();
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10); // Menos mensajes por poll

        ConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(new DefaultErrorHandler());

        return factory;
    }

    // ============================================================
    // CASO 4: BATCH PROCESSING
    // ============================================================
    // Uso: Procesar múltiples mensajes a la vez
    // Ventaja: Alto throughput, ideal para inserts masivos
    // Desventaja: Más complejo de manejar errores
    // ============================================================
    @Bean(name = "batchKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> batchKafkaListenerContainerFactory() {
        Map<String, Object> props = getBaseConsumerConfig();
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // ✅ Lotes grandes
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 50000); // Esperar más datos
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 1000); // Max 1 segundo

        ConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(2);
        factory.setBatchListener(true); // ✅ Activar modo batch
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(new DefaultErrorHandler());

        return factory;
    }

    // ============================================================
    // CASO 5: RETRY WITH DLQ (Dead Letter Queue)
    // ============================================================
    // Uso: Reintentos automáticos + DLQ para mensajes problemáticos
    // Ventaja: Resiliencia, no bloquea el consumer
    // Desventaja: Configuración más compleja
    // ============================================================
    @Bean(name = "retryDlqKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> retryDlqKafkaListenerContainerFactory(
            KafkaTemplate<String, Object> kafkaTemplate) {

        Map<String, Object> props = getBaseConsumerConfig();
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        ConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(2);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // ✅ Configurar RETRY con Exponential Backoff
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);    // 1 segundo
        backOff.setMultiplier(2.0);           // 2x cada intento
        backOff.setMaxInterval(15000L);       // Max 15 segundos

        // ✅ Error Handler con DLQ
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate),
                backOff
        );

        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class); // No reintentar estos
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    // ============================================================
    // CASO 6: IDEMPOTENT + ERROR HANDLING
    // ============================================================
    // Uso: Procesamiento idempotente con manejo robusto de errores
    // Ventaja: Evita duplicados, tolerante a fallos
    // Desventaja: Requiere lógica adicional en el consumer
    // ============================================================
    @Bean(name = "idempotentKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> idempotentKafkaListenerContainerFactory() {
        Map<String, Object> props = getBaseConsumerConfig();
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"); // ✅ Solo mensajes committed

        // ✅ ErrorHandlingDeserializer para evitar crashes por mensajes corruptos
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);

        ConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // ✅ Fixed BackOff: 3 reintentos con 5 segundos entre cada uno
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(5000L, 3));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    // ============================================================
    // NOTA IMPORTANTE: KafkaTemplate para DLQ
    // ============================================================
    // Necesitas este bean para que funcione el caso 5 (Retry + DLQ)
    // Descomenta si vas a usar retryDlqKafkaListenerContainerFactory
    // ============================================================
    /*
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(props);
    }
    */
}
