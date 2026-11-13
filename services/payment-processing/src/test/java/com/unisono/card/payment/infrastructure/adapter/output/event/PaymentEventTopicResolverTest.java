package com.unisono.card.payment.infrastructure.adapter.output.event;

import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.unisono.card.payment.support.TestTransactions.approvedPurchase;
import static com.unisono.card.payment.support.TestTransactions.approvedRefund;
import static com.unisono.card.payment.support.TestTransactions.declinedPurchase;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sin configuración, cada evento de pago va al topic del catálogo compartido (KafkaTopics)
 */
@SpringJUnitConfig(PaymentEventTopicResolver.class)
class PaymentEventTopicResolverTest {

    @Autowired
    private PaymentEventTopicResolver resolver;

    @Test
    void shouldRouteEveryPaymentEventToItsCatalogTopic() {
        Transaction approved = approvedPurchase(1_000);
        Transaction reversed = approvedPurchase(1_000);
        reversed.reverse("Cliente");
        Transaction settled = approvedPurchase(1_000);
        settled.settle();

        Map<String, String> topics = topicsOf(approved, declinedPurchase(1_000, "51"), reversed, settled,
                approvedRefund(500, TransactionId.generate()));

        assertThat(topics).containsEntry("PaymentInitiated", KafkaTopics.PAYMENT_INITIATED)
                .containsEntry("PaymentAuthorized", KafkaTopics.PAYMENT_AUTHORIZED)
                .containsEntry("PaymentProcessed", KafkaTopics.PAYMENT_PROCESSED)
                .containsEntry("PaymentDeclined", KafkaTopics.PAYMENT_DECLINED)
                .containsEntry("PaymentReversed", KafkaTopics.PAYMENT_REVERSED)
                .containsEntry("TransactionSettled", KafkaTopics.TRANSACTION_SETTLED)
                .containsEntry("RefundInitiated", KafkaTopics.REFUND_INITIATED)
                .containsEntry("RefundProcessed", KafkaTopics.REFUND_PROCESSED);
    }

    private Map<String, String> topicsOf(Transaction... transactions) {
        return List.of(transactions).stream()
                .flatMap(t -> t.getDomainEvents().stream())
                .collect(Collectors.toMap(DomainEvent::getEventType, resolver::resolveTopicForEvent, (a, b) -> a));
    }
}
