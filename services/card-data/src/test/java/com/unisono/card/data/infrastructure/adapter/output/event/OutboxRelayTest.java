package com.unisono.card.data.infrastructure.adapter.output.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.OutboxEventEntity;
import com.unisono.card.data.infrastructure.adapter.output.persistence.repository.JpaOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private JpaOutboxEventRepository repository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(repository, kafkaTemplate, objectMapper, transactionManager);
        ReflectionTestUtils.setField(relay, "batchSize", 50);
        ReflectionTestUtils.setField(relay, "sendTimeoutMs", 1000L);
    }

    @Test
    void shouldPublishPendingEventsInOrder() {
        OutboxEventEntity first = event(1L, "card-1");
        OutboxEventEntity second = event(2L, "card-2");
        when(repository.lockNextPendingBatch(50)).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(sent());

        int published = relay.publishBatch();

        assertThat(published).isEqualTo(2);
        assertThat(first.getPublishedAt()).isNotNull();
        assertThat(second.getPublishedAt()).isNotNull();
        assertThat(first.getAttempts()).isEqualTo(1);
        verify(repository).saveAll(List.of(first, second));
    }

    @Test
    void shouldSendStoredPayloadWithAggregateIdAsKey() throws Exception {
        OutboxEventEntity event = event(1L, "card-1");
        when(repository.lockNextPendingBatch(50)).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(sent());

        relay.publishBatch();

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("card.created"), eq("card-1"), payload.capture());
        assertThat(payload.getValue()).isInstanceOf(JsonNode.class);
        assertThat(payload.getValue()).isEqualTo(objectMapper.readTree(event.getPayload()));
    }

    @Test
    void shouldStopBatchAndKeepEventPendingWhenSendFails() {
        OutboxEventEntity first = event(1L, "card-1");
        OutboxEventEntity second = event(2L, "card-2");
        when(repository.lockNextPendingBatch(50)).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(anyString(), eq("card-1"), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));

        int published = relay.publishBatch();

        assertThat(published).isZero();
        assertThat(first.getPublishedAt()).isNull();
        assertThat(first.getAttempts()).isEqualTo(1);
        assertThat(first.getLastError()).contains("broker unavailable");
        // El segundo no se envía para no publicar fuera de orden
        verify(kafkaTemplate, never()).send(anyString(), eq("card-2"), any());
        assertThat(second.getAttempts()).isZero();
    }

    @Test
    void shouldDoNothingWhenThereAreNoPendingEvents() {
        when(repository.lockNextPendingBatch(50)).thenReturn(List.of());

        assertThat(relay.publishBatch()).isZero();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    private static CompletableFuture<SendResult<String, Object>> sent() {
        return CompletableFuture.completedFuture(null);
    }

    private static OutboxEventEntity event(Long id, String aggregateId) {
        return OutboxEventEntity.builder()
                .id(id)
                .eventId("evt-" + id)
                .aggregateId(aggregateId)
                .topic("card.created")
                .eventType("CardCreatedEvent")
                .payload("{\"eventId\":\"evt-" + id + "\",\"cardId\":\"" + aggregateId + "\"}")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
