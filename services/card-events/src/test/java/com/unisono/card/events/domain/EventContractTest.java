package com.unisono.card.events.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.events.config.EventSerializationConfig;
import com.unisono.card.events.domain.payment.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato de los eventos: lo que publica un productor lo puede leer un consumidor, sin perder campos.
 */
class EventContractTest {

    private static final String CARD_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 2, 10, 15, 30);
    private static final String TX_ID = "6f1c2d3e-8a9b-4c5d-9e0f-1a2b3c4d5e6f";

    private final ObjectMapper mapper = EventSerializationConfig.objectMapper();

    static Stream<DomainEvent> events() {
        return Stream.of(
                new CardCreatedEvent(CARD_ID, "12345678", "Juan Pérez", "VISA", "CLASSIC", "system"),
                new CardApprovedEvent(CARD_ID, "Cumple requisitos", "admin"),
                new CardRejectedEvent(CARD_ID, "No cumple requisitos", "admin"),
                new CardIssuedEvent(CARD_ID, "0366", 12, 2030, "Emisión", "system"),
                new CardActivatedEvent(CARD_ID, AT, "Activación", "cliente"),
                new CardBlockedEvent(CARD_ID, "FRAUD", AT, "Fraude", "antifraude"),
                new CardUnblockedEvent(CARD_ID, AT, "Recuperada", "admin"),
                new CardCancelledEvent(CARD_ID, AT, "Baja", "cliente"),
                new CardExpiredEvent(CARD_ID, AT, "Vencimiento del plástico", "system"),
                new CreditLimitAssignedEvent(CARD_ID, new BigDecimal("150000.50"), new BigDecimal("30000.10"), "ARS", "admin"),
                new CreditLimitUpdatedEvent(CARD_ID, new BigDecimal("100.00"), new BigDecimal("200.00"), "ARS", "Aumento", "admin"),
                new CreditLimitConsumedEvent(CARD_ID, "tx-1", new BigDecimal("10.25"), new BigDecimal("89.75"), "ARS"),
                new PaymentInitiatedEvent("evt-1", AT, TX_ID, CARD_ID, "PURCHASE", new BigDecimal("98000.00"), "ARS", new BigDecimal("100.00"), "USD", new BigDecimal("980.00"), AT),
                new PaymentAuthorizedEvent("evt-2", AT, TX_ID, CARD_ID, "A1B2C3", 12, AT),
                new PaymentProcessedEvent("evt-3", AT, TX_ID, CARD_ID, "APPROVED", new BigDecimal("98000.00"), "ARS", new BigDecimal("100.00"), "USD", new BigDecimal("980.00"), AT),
                new PaymentDeclinedEvent("evt-4", AT, TX_ID, CARD_ID, "59", "Fraude", AT),
                new PaymentReversedEvent("evt-5", AT, TX_ID, CARD_ID, new BigDecimal("98000.00"), "ARS", new BigDecimal("100.00"), "USD", new BigDecimal("980.00"), "Cliente", AT),
                new RefundInitiatedEvent("evt-6", AT, "refund-1", TX_ID, CARD_ID, new BigDecimal("98000.00"), "ARS", new BigDecimal("100.00"), "USD", new BigDecimal("980.00"), AT),
                new RefundProcessedEvent("evt-7", AT, "refund-1", TX_ID, CARD_ID, new BigDecimal("98000.00"), "ARS", new BigDecimal("100.00"), "USD", new BigDecimal("980.00"), AT),
                new TransactionSettledEvent("evt-8", AT, TX_ID, CARD_ID, new BigDecimal("98000.00"), "ARS", new BigDecimal("100.00"), "USD", new BigDecimal("980.00"), AT)
        );
    }

    @ParameterizedTest
    @MethodSource("events")
    void survivesRoundTripWithEveryFieldSet(DomainEvent event) throws Exception {
        String json = mapper.writeValueAsString(event);
        JsonNode published = mapper.readTree(json);

        DomainEvent consumed = mapper.readValue(json, event.getClass());

        assertThat(mapper.readTree(mapper.writeValueAsString(consumed))).isEqualTo(published);
        assertThat(consumed.getEventId()).isEqualTo(event.getEventId());
        assertThat(published.get("eventType").asText()).isEqualTo(event.getClass().getSimpleName());
        published.fields().forEachRemaining(field ->
                assertThat(field.getValue().isNull()).as(field.getKey()).isFalse());
    }
}
