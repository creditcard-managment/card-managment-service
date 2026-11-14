package com.unisono.card.payment.infrastructure.adapter.output.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.events.config.EventSerializationConfig;
import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.events.domain.payment.PaymentAuthorizedEvent;
import com.unisono.card.events.domain.payment.PaymentDeclinedEvent;
import com.unisono.card.events.domain.payment.PaymentInitiatedEvent;
import com.unisono.card.events.domain.payment.PaymentProcessedEvent;
import com.unisono.card.events.domain.payment.PaymentReversedEvent;
import com.unisono.card.events.domain.payment.RefundInitiatedEvent;
import com.unisono.card.events.domain.payment.RefundProcessedEvent;
import com.unisono.card.events.domain.payment.TransactionSettledEvent;
import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.PaymentMethod;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.unisono.card.payment.support.TestTransactions.CARD_ID;
import static com.unisono.card.payment.support.TestTransactions.approvedPurchase;
import static com.unisono.card.payment.support.TestTransactions.approvedRefund;
import static com.unisono.card.payment.support.TestTransactions.cardData;
import static com.unisono.card.payment.support.TestTransactions.declinedPurchase;
import static com.unisono.card.payment.support.TestTransactions.merchant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cada evento de dominio se traduce a su contrato de card-events, y ese JSON se vuelve a leer con la clase
 * del contrato (lo que un consumidor Java no podía hacer con las clases del dominio)
 */
class PaymentIntegrationEventMapperTest {

    private final PaymentIntegrationEventMapper mapper = new PaymentIntegrationEventMapper();
    private final ObjectMapper json = EventSerializationConfig.objectMapper();

    @Test
    void shouldTranslateAPurchaseWithCurrencyConversion() throws Exception {
        Transaction purchase = Transaction.createPurchase(CARD_ID, cardData(), merchant(),
                TransactionAmount.withConversion(Money.of(new BigDecimal("100.00"), "USD"),
                        Money.ofARS(new BigDecimal("98000.00")), new BigDecimal("980.00")),
                PaymentMethod.ONLINE);
        purchase.authorize(AuthorizationCode.approved("A1B2C3"), 12);
        purchase.approve();
        List<DomainEvent> events = purchase.getDomainEvents();

        PaymentInitiatedEvent initiated = roundTrip(events.get(0), PaymentInitiatedEvent.class);
        assertThat(initiated.getEventId()).isEqualTo(events.get(0).getEventId());
        assertThat(initiated.getOccurredOn()).isEqualTo(events.get(0).getOccurredOn().withNano(0));
        assertThat(initiated.getTransactionId()).isEqualTo(purchase.getTransactionId().toString());
        assertThat(initiated.getCardId()).isEqualTo(CARD_ID.getValue());
        assertThat(initiated.getTransactionType()).isEqualTo("PURCHASE");
        // Lo que se le cobra a la tarjeta, y el original de la conversión
        assertThat(initiated.getAmount()).isEqualByComparingTo("98000.00");
        assertThat(initiated.getCurrency()).isEqualTo("ARS");
        assertThat(initiated.getOriginalAmount()).isEqualByComparingTo("100.00");
        assertThat(initiated.getOriginalCurrency()).isEqualTo("USD");
        assertThat(initiated.getExchangeRate()).isEqualByComparingTo("980.00");

        PaymentAuthorizedEvent authorized = roundTrip(events.get(1), PaymentAuthorizedEvent.class);
        assertThat(authorized.getAuthorizationCode()).isEqualTo("A1B2C3");
        assertThat(authorized.getRiskScore()).isEqualTo(12);
        assertThat(authorized.getAuthorizedAt()).isNotNull();

        PaymentProcessedEvent processed = roundTrip(events.get(2), PaymentProcessedEvent.class);
        assertThat(processed.getStatus()).isEqualTo("APPROVED");
        assertThat(processed.getAmount()).isEqualByComparingTo("98000.00");
    }

    @Test
    void shouldLeaveTheConversionFieldsEmptyWithoutConversion() throws Exception {
        PaymentInitiatedEvent initiated = roundTrip(approvedPurchase(1_500).getDomainEvents().get(0),
                PaymentInitiatedEvent.class);

        assertThat(initiated.getAmount()).isEqualByComparingTo("1500");
        assertThat(initiated.getCurrency()).isEqualTo("ARS");
        assertThat(initiated.getOriginalAmount()).isNull();
        assertThat(initiated.getOriginalCurrency()).isNull();
        assertThat(initiated.getExchangeRate()).isNull();
    }

    @Test
    void shouldCarryTheResponseCodeOfADecline() throws Exception {
        List<DomainEvent> events = declinedPurchase(1_000, "59").getDomainEvents();

        PaymentDeclinedEvent declined = roundTrip(events.get(events.size() - 1), PaymentDeclinedEvent.class);

        assertThat(declined.getResponseCode()).isEqualTo("59");
        assertThat(declined.getDeclineReason()).isEqualTo("Declined in test");
        assertThat(declined.getDeclinedAt()).isNotNull();
    }

    @Test
    void shouldTranslateReversalsSettlementsAndRefunds() throws Exception {
        Transaction reversed = approvedPurchase(1_000);
        reversed.clearDomainEvents();
        reversed.reverse("Cliente desconoce");
        PaymentReversedEvent reversal = roundTrip(reversed.getDomainEvents().get(0), PaymentReversedEvent.class);
        assertThat(reversal.getReversalReason()).isEqualTo("Cliente desconoce");
        assertThat(reversal.getAmount()).isEqualByComparingTo("1000");

        Transaction settled = approvedPurchase(2_000);
        settled.clearDomainEvents();
        settled.settle();
        TransactionSettledEvent settlement = roundTrip(settled.getDomainEvents().get(0), TransactionSettledEvent.class);
        assertThat(settlement.getSettledAt()).isNotNull();
        assertThat(settlement.getAmount()).isEqualByComparingTo("2000");

        TransactionId originalId = TransactionId.generate();
        Transaction refund = approvedRefund(500, originalId);
        RefundInitiatedEvent initiated = roundTrip(refund.getDomainEvents().get(0), RefundInitiatedEvent.class);
        RefundProcessedEvent processed = roundTrip(refund.getDomainEvents().get(1), RefundProcessedEvent.class);
        assertThat(initiated.getRefundTransactionId()).isEqualTo(refund.getTransactionId().toString());
        assertThat(initiated.getOriginalTransactionId()).isEqualTo(originalId.toString());
        assertThat(processed.getAmount()).isEqualByComparingTo("500");
        assertThat(processed.getProcessedAt()).isNotNull();
    }

    @Test
    void shouldRejectEventsWithoutAContract() {
        DomainEvent foreign = new com.unisono.card.events.domain.CardApprovedEvent("card", "REQUESTED", "x", "y");

        assertThatThrownBy(() -> mapper.toIntegrationEvent(foreign)).isInstanceOf(IllegalArgumentException.class);
    }

    private <T extends DomainEvent> T roundTrip(DomainEvent domainEvent, Class<T> contract) throws Exception {
        DomainEvent integration = mapper.toIntegrationEvent(domainEvent);
        assertThat(integration).isInstanceOf(contract);
        String payload = json.writeValueAsString(integration);
        // Sin objetos anidados de Value Objects: ids, montos y fechas son valores simples
        assertThat(json.readTree(payload).path("transactionId").isObject()).isFalse();
        assertThat(json.readTree(payload).path("cardId").isTextual()).isTrue();
        return json.readValue(payload, contract);
    }
}
