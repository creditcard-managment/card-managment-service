package com.unisono.card.data.infrastructure.adapter.input.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.data.domain.port.input.BlockCardForFraudDeclineUseCase;
import com.unisono.card.data.domain.port.input.BlockCardForFraudDeclineUseCase.FraudDeclineCommand;
import com.unisono.card.data.domain.port.input.BlockCardForFraudDeclineUseCase.Outcome;
import com.unisono.card.events.config.EventSerializationConfig;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.payment.PaymentDeclinedEvent;
import com.unisono.card.shared.domain.valueobject.CardId;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consume payment.processing.declined: un rechazo por fraude (responseCode {@value #FRAUD_RESPONSE_CODE}) bloquea la
 * tarjeta como BLOCKED_FRAUD ({@link BlockCardForFraudDeclineUseCase}). Los demás rechazos se confirman sin hacer nada.
 *
 * Grupo propio que arranca desde el final del topic la primera vez
 * ({@code card.data.payments.consumer.auto-offset-reset}, por defecto latest): un rechazo viejo retenido en Kafka no
 * bloquea tarjetas al desplegar. Las excepciones llegan al error handler de
 * {@code paymentEventsContainerFactory}: reintentos y después dlq.payment-events.
 */
@Slf4j
@Component
public class PaymentDeclinedConsumer {

    static final String FRAUD_RESPONSE_CODE = "59";

    private final BlockCardForFraudDeclineUseCase blockForFraud;
    private final ObjectMapper objectMapper = EventSerializationConfig.objectMapper();
    private final MeterRegistry meterRegistry;

    public PaymentDeclinedConsumer(BlockCardForFraudDeclineUseCase blockForFraud, MeterRegistry meterRegistry) {
        this.blockForFraud = blockForFraud;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(
            id = "payment-declined",
            topics = KafkaTopics.PAYMENT_DECLINED,
            groupId = "${card.data.payments.consumer.group-id:card-data-payment-consumer}",
            containerFactory = "paymentEventsContainerFactory",
            properties = "auto.offset.reset=${card.data.payments.consumer.auto-offset-reset:latest}",
            autoStartup = "${card.data.payments.consumer.enabled:true}"
    )
    public void consume(@Payload String eventJson, Acknowledgment acknowledgment) throws JsonProcessingException {
        PaymentDeclinedEvent event = objectMapper.readValue(eventJson, PaymentDeclinedEvent.class);

        if (!FRAUD_RESPONSE_CODE.equals(event.getResponseCode())) {
            acknowledgment.acknowledge();
            return;
        }

        Outcome outcome = blockForFraud.execute(new FraudDeclineCommand(
                event.getEventId(),
                CardId.of(event.getCardId()),
                event.getTransactionId(),
                event.getDeclineReason(),
                event.getDeclinedAt()));
        meterRegistry.counter("card.data.fraud.declines", "outcome", outcome.name().toLowerCase()).increment();
        acknowledgment.acknowledge();
    }
}
