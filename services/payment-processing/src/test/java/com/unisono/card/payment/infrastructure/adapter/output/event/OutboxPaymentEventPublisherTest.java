package com.unisono.card.payment.infrastructure.adapter.output.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.payment.domain.event.PaymentReversed;
import com.unisono.card.payment.domain.event.RefundProcessed;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentOutboxEventEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentOutboxEventRepository;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPaymentEventPublisherTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private JpaPaymentOutboxEventRepository repository;
    @Mock
    private PaymentEventTopicResolver topicResolver;

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private OutboxPaymentEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPaymentEventPublisher(repository, topicResolver, objectMapper);
    }

    @Test
    void shouldStoreTheSerializedEventWithTheCardAsKey() throws Exception {
        TransactionId transactionId = TransactionId.generate();
        PaymentReversed event = new PaymentReversed(transactionId, CARD_ID, TransactionAmount.of(Money.ofARS(1_500)),
                "Cliente", LocalDateTime.now());
        when(topicResolver.resolveTopicForEvent(event)).thenReturn("payment.reversal.completed");

        publisher.publish(event);

        ArgumentCaptor<PaymentOutboxEventEntity> saved = ArgumentCaptor.forClass(PaymentOutboxEventEntity.class);
        verify(repository).save(saved.capture());
        PaymentOutboxEventEntity entity = saved.getValue();
        assertThat(entity.getEventId()).isEqualTo(event.getEventId());
        assertThat(entity.getAggregateId()).isEqualTo(CARD_ID.getValue());
        assertThat(entity.getTopic()).isEqualTo("payment.reversal.completed");
        assertThat(entity.getEventType()).isEqualTo("PaymentReversed");
        assertThat(entity.getPublishedAt()).isNull();

        JsonNode payload = objectMapper.readTree(entity.getPayload());
        assertThat(payload.path("eventId").asText()).isEqualTo(event.getEventId());
        assertThat(payload.path("reversalReason").asText()).isEqualTo("Cliente");
        assertThat(payload.path("transactionId").toString()).contains(transactionId.toString());
    }

    @Test
    void shouldUseTheCardAsKeyForRefundEventsToo() {
        RefundProcessed event = new RefundProcessed(TransactionId.generate(), TransactionId.generate(), CARD_ID,
                TransactionAmount.of(Money.ofARS(500)), LocalDateTime.now());
        when(topicResolver.resolveTopicForEvent(event)).thenReturn("payment.refund.processed");

        publisher.publishAll(List.of(event));

        ArgumentCaptor<PaymentOutboxEventEntity> saved = ArgumentCaptor.forClass(PaymentOutboxEventEntity.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getAggregateId()).isEqualTo(CARD_ID.getValue());
    }

    @Test
    void shouldIgnoreNullEvents() {
        publisher.publish(null);
        publisher.publishAll(null);

        verify(repository, times(0)).save(any());
    }
}
