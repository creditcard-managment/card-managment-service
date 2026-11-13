package com.unisono.card.payment.infrastructure.adapter.output.event;

import com.unisono.card.payment.infrastructure.observability.ConsistencyMonitor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentOutboxEventEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.NotLeaderOrFollowerException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxRelayTest {

    @Mock
    private JpaPaymentOutboxEventRepository repository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private ConsistencyMonitor monitor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PaymentOutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new PaymentOutboxRelay(repository, kafkaTemplate, objectMapper, transactionManager, monitor);
        ReflectionTestUtils.setField(relay, "batchSize", 50);
        ReflectionTestUtils.setField(relay, "sendTimeoutMs", 1000L);
    }

    @Test
    void shouldPublishPendingEventsInOrder() {
        PaymentOutboxEventEntity first = event(1L, "card-1");
        PaymentOutboxEventEntity second = event(2L, "card-2");
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
        PaymentOutboxEventEntity event = event(1L, "card-1");
        when(repository.lockNextPendingBatch(50)).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(sent());

        relay.publishBatch();

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("payment.processing.completed"), eq("card-1"), payload.capture());
        assertThat(payload.getValue()).isInstanceOf(JsonNode.class);
        assertThat(payload.getValue()).isEqualTo(objectMapper.readTree(event.getPayload()));
    }

    @Test
    void shouldStopBatchAndKeepEventPendingOnTransientError() {
        PaymentOutboxEventEntity first = event(1L, "card-1");
        PaymentOutboxEventEntity second = event(2L, "card-2");
        when(repository.lockNextPendingBatch(50)).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(anyString(), eq("card-1"), any()))
                .thenReturn(CompletableFuture.failedFuture(new KafkaProducerException(null, "send failed",
                        new TimeoutException("Topic payment.processing.completed not present in metadata after 5000 ms."))));

        int published = relay.publishBatch();

        assertThat(published).isZero();
        assertThat(first.getPublishedAt()).isNull();
        assertThat(first.getFailedAt()).isNull();
        assertThat(first.getAttempts()).isEqualTo(1);
        assertThat(first.getLastError()).contains("TimeoutException").contains("not present in metadata");
        // El segundo no se envía para no publicar fuera de orden
        verify(kafkaTemplate, never()).send(anyString(), eq("card-2"), any());
        assertThat(second.getAttempts()).isZero();
        verify(monitor).outboxPublishFailed(true);
    }

    @Test
    void shouldMarkEventAsFailedAndContinueOnPermanentError() {
        PaymentOutboxEventEntity tooLarge = event(1L, "card-1");
        PaymentOutboxEventEntity next = event(2L, "card-2");
        when(repository.lockNextPendingBatch(50)).thenReturn(List.of(tooLarge, next));
        when(kafkaTemplate.send(anyString(), eq("card-1"), any()))
                .thenReturn(CompletableFuture.failedFuture(new KafkaProducerException(null, "send failed",
                        new RecordTooLargeException("The message is 2000000 bytes"))));
        when(kafkaTemplate.send(anyString(), eq("card-2"), any())).thenReturn(sent());

        int published = relay.publishBatch();

        assertThat(published).isEqualTo(1);
        assertThat(tooLarge.getFailedAt()).isNotNull();
        assertThat(tooLarge.getPublishedAt()).isNull();
        assertThat(tooLarge.getLastError()).contains("RecordTooLargeException");
        assertThat(next.getPublishedAt()).isNotNull();
        verify(monitor).outboxPublishFailed(false);
    }

    @Test
    void shouldMarkEventWithInvalidPayloadAsFailed() {
        PaymentOutboxEventEntity broken = event(1L, "card-1");
        broken.setPayload("{not json");
        PaymentOutboxEventEntity next = event(2L, "card-2");
        when(repository.lockNextPendingBatch(50)).thenReturn(List.of(broken, next));
        when(kafkaTemplate.send(anyString(), eq("card-2"), any())).thenReturn(sent());

        relay.publishBatch();

        assertThat(broken.getFailedAt()).isNotNull();
        verify(kafkaTemplate, never()).send(anyString(), eq("card-1"), any());
        assertThat(next.getPublishedAt()).isNotNull();
    }

    @Test
    void shouldClassifyErrors() {
        assertThat(PaymentOutboxRelay.isTransient(new ExecutionException(new KafkaProducerException(null, "x",
                new NotLeaderOrFollowerException("leader moved"))))).isTrue();
        assertThat(PaymentOutboxRelay.isTransient(new java.util.concurrent.TimeoutException())).isTrue();
        assertThat(PaymentOutboxRelay.isTransient(new ExecutionException(new KafkaProducerException(null, "x",
                new InvalidTopicException("bad topic"))))).isFalse();
        assertThat(PaymentOutboxRelay.isTransient(new IllegalStateException("unexpected"))).isFalse();
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

    private static PaymentOutboxEventEntity event(Long id, String aggregateId) {
        return PaymentOutboxEventEntity.builder()
                .id(id)
                .eventId("evt-" + id)
                .aggregateId(aggregateId)
                .topic("payment.processing.completed")
                .eventType("PaymentProcessed")
                .payload("{\"eventId\":\"evt-" + id + "\",\"cardId\":\"" + aggregateId + "\"}")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
