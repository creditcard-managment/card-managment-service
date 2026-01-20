package com.unisono.card.lifecycle.infrastructure.messaging.consumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.events.domain.CardCreatedEvent;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.entity.StateTransitionEntity;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.repository.JpaStateTransitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================
 * 6 CASOS DE USO DE KAFKA CONSUMERS
 * ============================================================
 *
 * Descomenta solo el consumer que quieras probar.
 * Asegúrate de que el containerFactory coincida con la config.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardEventConsumerExamples {

    private final JpaStateTransitionRepository stateTransitionRepository;
    private final ObjectMapper kafkaObjectMapper;

    // Para idempotencia (Caso 6)
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    // ============================================================
    // CASO 1: AUTO COMMIT
    // ============================================================
    // El commit se hace automáticamente cada 5 segundos
    // ⚠️ PELIGRO: Si falla el procesamiento, el mensaje se pierde
    // ============================================================
    /*
    @KafkaListener(
        topics = KafkaTopics.CARD_CREATED,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "autoCommitKafkaListenerContainerFactory"
    )
    public void consumeAutoCommit(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("🔵 [AUTO_COMMIT] Partition: {}, Offset: {}", partition, offset);

        try {
            CardCreatedEvent event = kafkaObjectMapper.readValue(eventJson, CardCreatedEvent.class);
            log.info("🔵 Processing card: {}", event.getCardId());

            registerCreateTransition(event);

            log.info("✅ [AUTO_COMMIT] Processed successfully");
            // ⚠️ NO hay acknowledge() - el commit es automático

        } catch (Exception e) {
            log.error("❌ [AUTO_COMMIT] ERROR: {}", e.getMessage());
            // ⚠️ El mensaje se perderá porque el commit es automático
        }
    }
    */

    // ============================================================
    // CASO 2: MANUAL COMMIT (TU IMPLEMENTACIÓN ACTUAL)
    // ============================================================
    // Control total: solo haces commit cuando llamas acknowledge()
    // ✅ SEGURO: Si falla, el mensaje se reprocesa
    // ============================================================
    /*
    @KafkaListener(
        topics = KafkaTopics.CARD_CREATED,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "manualKafkaListenerContainerFactory"
    )
    public void consumeManual(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("🟢 [MANUAL] Partition: {}, Offset: {}", partition, offset);

        try {
            CardCreatedEvent event = kafkaObjectMapper.readValue(eventJson, CardCreatedEvent.class);
            log.info("🟢 Processing card: {}", event.getCardId());

            registerCreateTransition(event);

            // ✅ Commit MANUAL solo si todo salió bien
            acknowledgment.acknowledge();
            log.info("✅ [MANUAL] Acknowledged successfully");

        } catch (Exception e) {
            log.error("❌ [MANUAL] ERROR: {}", e.getMessage());
            // ✅ NO hacemos acknowledge - el mensaje se reprocesará
        }
    }
    */

    // ============================================================
    // CASO 3: MANUAL IMMEDIATE
    // ============================================================
    // El commit se hace inmediatamente después del acknowledge()
    // Útil cuando necesitas garantía de procesamiento mensaje a mensaje
    // ============================================================
    /*
    @KafkaListener(
        topics = KafkaTopics.CARD_CREATED,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "manualImmediateKafkaListenerContainerFactory"
    )
    public void consumeManualImmediate(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("🟡 [MANUAL_IMMEDIATE] Partition: {}, Offset: {}", partition, offset);

        try {
            CardCreatedEvent event = kafkaObjectMapper.readValue(eventJson, CardCreatedEvent.class);
            log.info("🟡 Processing card: {}", event.getCardId());

            registerCreateTransition(event);

            // ✅ Commit INMEDIATO - no espera a batch
            acknowledgment.acknowledge();
            log.info("✅ [MANUAL_IMMEDIATE] Acknowledged and committed immediately");

        } catch (Exception e) {
            log.error("❌ [MANUAL_IMMEDIATE] ERROR: {}", e.getMessage());
        }
    }
    */

    // ============================================================
    // CASO 4: BATCH PROCESSING
    // ============================================================
    // Recibe una lista de mensajes para procesar en lote
    // ✅ IDEAL para inserts masivos en BD
    // ============================================================
    /*
    @KafkaListener(
        topics = KafkaTopics.CARD_CREATED,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeBatch(
            List<ConsumerRecord<String, String>> records,
            Acknowledgment acknowledgment
    ) {
        log.info("🟣 [BATCH] Received {} messages", records.size());

        List<StateTransitionEntity> transitions = new ArrayList<>();

        try {
            for (ConsumerRecord<String, String> record : records) {
                String eventJson = record.value();
                CardCreatedEvent event = kafkaObjectMapper.readValue(eventJson, CardCreatedEvent.class);

                log.info("🟣 Processing card: {} (Partition: {}, Offset: {})",
                        event.getCardId(),
                        record.partition(),
                        record.offset());

                StateTransitionEntity transition = StateTransitionEntity.builder()
                        .cardId(event.getCardId())
                        .fromState(null)
                        .toState("REQUESTED")
                        .event("CREATE")
                        .reason("Solicitud inicial de tarjeta")
                        .performedBy(event.getCreatedBy())
                        .transitionedAt(event.getOccurredOn())
                        .successful(true)
                        .build();

                transitions.add(transition);
            }

            // ✅ INSERT MASIVO - mucho más eficiente
            stateTransitionRepository.saveAll(transitions);

            // ✅ Acknowledge todo el batch
            acknowledgment.acknowledge();
            log.info("✅ [BATCH] Saved {} transitions and acknowledged", transitions.size());

        } catch (Exception e) {
            log.error("❌ [BATCH] ERROR processing batch: {}", e.getMessage());
            // ✅ NO acknowledge - se reprocesa todo el batch
        }
    }
    */

    // ============================================================
    // CASO 5: RETRY WITH DLQ
    // ============================================================
    // Reintentos automáticos: 1s → 2s → 4s
    // Si falla 3 veces → va a Dead Letter Queue (dlq-procesamiento-errores)
    // ============================================================
    /*
    @KafkaListener(
        topics = KafkaTopics.CARD_CREATED,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "retryDlqKafkaListenerContainerFactory"
    )
    public void consumeWithRetryDlq(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("🔴 [RETRY_DLQ] Partition: {}, Offset: {}", partition, offset);

        try {
            CardCreatedEvent event = kafkaObjectMapper.readValue(eventJson, CardCreatedEvent.class);
            log.info("🔴 Processing card: {}", event.getCardId());

            // Simulación: Falla en cards con ID que terminen en "999"
            if (event.getCardId().endsWith("999")) {
                throw new RuntimeException("Simulated error for card ending in 999");
            }

            registerCreateTransition(event);

            log.info("✅ [RETRY_DLQ] Processed successfully");

        } catch (Exception e) {
            log.error("❌ [RETRY_DLQ] ERROR (attempt will retry): {}", e.getMessage());
            // ✅ El DefaultErrorHandler reintentará automáticamente
            // Si falla 3 veces → DeadLetterPublishingRecoverer lo envía a DLQ
            throw e; // Importante: re-throw para activar el retry
        }
    }
    */

    // ============================================================
    // CASO 6: IDEMPOTENT PROCESSING
    // ============================================================
    // Evita procesar el mismo mensaje dos veces
    // Útil en caso de rebalanceos o reprocesamiento
    // ============================================================
    /*
    @KafkaListener(
        topics = KafkaTopics.CARD_CREATED,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "idempotentKafkaListenerContainerFactory"
    )
    public void consumeIdempotent(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("🟠 [IDEMPOTENT] Partition: {}, Offset: {}", partition, offset);

        try {
            CardCreatedEvent event = kafkaObjectMapper.readValue(eventJson, CardCreatedEvent.class);

            // ✅ Verificar si ya procesamos este evento
            if (processedEventIds.contains(event.getEventId())) {
                log.warn("⚠️ [IDEMPOTENT] Event {} already processed, skipping", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            log.info("🟠 Processing card: {}", event.getCardId());
            registerCreateTransition(event);

            // ✅ Marcar como procesado
            processedEventIds.add(event.getEventId());

            acknowledgment.acknowledge();
            log.info("✅ [IDEMPOTENT] Processed and marked as done");

        } catch (Exception e) {
            log.error("❌ [IDEMPOTENT] ERROR: {}", e.getMessage());
        }
    }
    */

    // ============================================================
    // MÉTODO COMPARTIDO
    // ============================================================
    private void registerCreateTransition(CardCreatedEvent event) {
        StateTransitionEntity transition = StateTransitionEntity.builder()
                .cardId(event.getCardId())
                .fromState(null)
                .toState("REQUESTED")
                .event("CREATE")
                .reason("Solicitud inicial de tarjeta")
                .performedBy(event.getCreatedBy())
                .transitionedAt(event.getOccurredOn())
                .successful(true)
                .build();

        stateTransitionRepository.save(transition);
    }
}
