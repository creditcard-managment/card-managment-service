package com.unisono.card.data.infrastructure.adapter.input.event;

import com.unisono.card.data.domain.port.input.BlockCardForFraudDeclineUseCase;
import com.unisono.card.data.domain.port.input.BlockCardForFraudDeclineUseCase.Outcome;
import com.unisono.card.data.infrastructure.config.KafkaConsumerConfig;
import com.unisono.card.events.config.EventSerializationConfig;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.payment.PaymentDeclinedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Consumidor de payment.processing.declined contra un broker Kafka real (embebido): solo los rechazos por fraude
 * llegan al caso de uso, y los que no se pueden procesar terminan en dlq.payment-events.
 */
@SpringJUnitConfig(PaymentDeclinedConsumerKafkaTest.TestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.PAYMENT_DECLINED, KafkaTopics.DLQ_PAYMENT_EVENTS})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "card.data.payments.consumer.auto-offset-reset=earliest",
        "card.data.payments.consumer.max-retries=1",
        "card.data.payments.consumer.initial-interval-ms=50"
})
@DirtiesContext
class PaymentDeclinedConsumerKafkaTest {

    private static final long WAIT_MS = 20_000;

    @Configuration
    @EnableKafka
    @Import({KafkaConsumerConfig.class, PaymentDeclinedConsumer.class})
    static class TestConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private KafkaTemplate<String, String> paymentDeadLetterKafkaTemplate;

    @MockBean
    private BlockCardForFraudDeclineUseCase blockForFraud;

    @Test
    void fraudDeclineReachesTheUseCaseAndOtherDeclinesDoNot() throws Exception {
        String fraudCard = UUID.randomUUID().toString();
        String fundsCard = UUID.randomUUID().toString();
        when(blockForFraud.execute(any())).thenReturn(Outcome.BLOCKED);

        send(declined(fundsCard, "51"));
        send(declined(fraudCard, "59"));

        verify(blockForFraud, timeout(WAIT_MS)).execute(argThat(c -> c.cardId().getValue().equals(fraudCard)
                && c.transactionId().equals("tx-" + fraudCard)));
        verify(blockForFraud, never()).execute(argThat(c -> c.cardId().getValue().equals(fundsCard)));
    }

    @Test
    void declineThatKeepsFailingGoesToTheDlq() throws Exception {
        String cardId = UUID.randomUUID().toString();
        when(blockForFraud.execute(argThat(c -> c != null && c.cardId().getValue().equals(cardId))))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        send(declined(cardId, "59"));

        await().atMost(Duration.ofMillis(WAIT_MS)).until(() -> inDlq(cardId));
    }

    @Test
    void unreadableMessageGoesToTheDlqWithoutReachingTheUseCase() throws Exception {
        String key = UUID.randomUUID().toString();

        paymentDeadLetterKafkaTemplate.send(KafkaTopics.PAYMENT_DECLINED, key, "{not json").get();

        await().atMost(Duration.ofMillis(WAIT_MS)).until(() -> inDlq(key));
        verify(blockForFraud, after(500).never()).execute(argThat(c -> c != null && c.cardId().getValue().equals(key)));
    }

    private PaymentDeclinedEvent declined(String cardId, String responseCode) {
        return new PaymentDeclinedEvent(UUID.randomUUID().toString(), LocalDateTime.now(), "tx-" + cardId, cardId,
                responseCode, "Rechazo", LocalDateTime.now());
    }

    private void send(PaymentDeclinedEvent event) throws Exception {
        paymentDeadLetterKafkaTemplate.send(KafkaTopics.PAYMENT_DECLINED, event.getCardId(),
                EventSerializationConfig.objectMapper().writeValueAsString(event)).get();
    }

    private boolean inDlq(String key) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), "false", broker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, KafkaTopics.DLQ_PAYMENT_EVENTS);
            List<ConsumerRecord<String, String>> records = new ArrayList<>();
            KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1)).forEach(records::add);
            return records.stream().anyMatch(r -> key.equals(r.key()));
        }
    }
}
