package com.unisono.card.payment.infrastructure.adapter.output.persistence.mapper;

import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.CardData;
import com.unisono.card.payment.domain.model.MerchantInfo;
import com.unisono.card.payment.domain.model.PaymentMethod;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.model.TransactionType;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.TransactionEntity;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ida y vuelta dominio → entidad → dominio: el aggregate se reconstruye con todo su estado y sin eventos
 */
class TransactionEntityMapperTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");

    private final TransactionEntityMapper mapper = new TransactionEntityMapper();

    @Test
    void shouldRebuildAnApprovedPurchaseWithItsWholeState() {
        Transaction purchase = Transaction.createPurchase(CARD_ID, cardData(), merchant(),
                TransactionAmount.withConversion(Money.of(new BigDecimal("100.00"), "USD"),
                        Money.ofARS(new BigDecimal("98000.00")), new BigDecimal("980.00")),
                PaymentMethod.CHIP);
        purchase.authorize(AuthorizationCode.approved("A1B2C3"), 15);
        purchase.approve();
        purchase.setTerminalInfo("TERM-01", "051");
        TransactionEntity entity = mapper.toEntity(purchase);
        entity.setVersion(3);

        Transaction rebuilt = mapper.toDomain(entity);

        assertThat(rebuilt.getTransactionId()).isEqualTo(purchase.getTransactionId());
        assertThat(rebuilt.getCardId()).isEqualTo(CARD_ID);
        assertThat(rebuilt.getType()).isEqualTo(TransactionType.PURCHASE);
        assertThat(rebuilt.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(rebuilt.getPaymentMethod()).isEqualTo(PaymentMethod.CHIP);
        assertThat(rebuilt.getAmount().getAmount()).isEqualTo(purchase.getAmount().getAmount());
        assertThat(rebuilt.getAmount().getLocalAmount()).isEqualTo(purchase.getAmount().getLocalAmount());
        assertThat(rebuilt.getAmount().getExchangeRate()).isEqualByComparingTo("980.00");
        assertThat(rebuilt.getAuthorizationCode().getCode()).isEqualTo("A1B2C3");
        assertThat(rebuilt.getAuthorizationCode().isApproved()).isTrue();
        assertThat(rebuilt.getRiskScore()).isEqualTo(15);
        assertThat(rebuilt.getTerminalId()).isEqualTo("TERM-01");
        assertThat(rebuilt.getPosEntryMode()).isEqualTo("051");
        assertThat(rebuilt.getTransactionDate()).isEqualTo(purchase.getTransactionDate());
        assertThat(rebuilt.getCardData().getLastFourDigits()).isEqualTo("0366");
        assertThat(rebuilt.getMerchantInfo().getMerchantId()).isEqualTo("MERCH-001");
        assertThat(rebuilt.getVersion()).isEqualTo(3);
        assertThat(rebuilt.getDomainEvents()).isEmpty();
    }

    @Test
    void shouldRebuildARefundLinkedToItsOriginal() {
        TransactionId originalId = TransactionId.generate();
        Transaction refund = Transaction.createRefund(CARD_ID, cardData(), merchant(),
                TransactionAmount.of(Money.ofARS(500)), originalId);
        refund.approveRefund(originalId);

        Transaction rebuilt = mapper.toDomain(mapper.toEntity(refund));

        assertThat(rebuilt.getType()).isEqualTo(TransactionType.REFUND);
        assertThat(rebuilt.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(rebuilt.getOriginalTransactionId()).isEqualTo(originalId);
    }

    @Test
    void shouldRebuildADeclinedTransactionWithItsReason() {
        Transaction declined = Transaction.createPurchase(CARD_ID, cardData(), merchant(),
                TransactionAmount.of(Money.ofARS(1_000)), PaymentMethod.ONLINE);
        declined.decline("Insufficient funds", AuthorizationCode.declined("51"));

        Transaction rebuilt = mapper.toDomain(mapper.toEntity(declined));

        assertThat(rebuilt.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(rebuilt.getDeclineReason()).isEqualTo("Insufficient funds");
        assertThat(rebuilt.getAuthorizationCode().getResponseCode()).isEqualTo("51");
        assertThat(rebuilt.getAuthorizationCode().isApproved()).isFalse();
    }

    @Test
    void shouldKeepTheRebuiltAggregateWorking() {
        Transaction purchase = Transaction.createPurchase(CARD_ID, cardData(), merchant(),
                TransactionAmount.of(Money.ofARS(1_000)), PaymentMethod.CHIP);
        purchase.authorize(AuthorizationCode.approved("X9"), 5);
        purchase.approve();
        Transaction rebuilt = mapper.toDomain(mapper.toEntity(purchase));

        rebuilt.settle();

        assertThat(rebuilt.getStatus()).isEqualTo(TransactionStatus.SETTLED);
        assertThat(rebuilt.getSettlementDate()).isNotNull();
        assertThat(rebuilt.getDomainEvents()).hasSize(1);
    }

    @Test
    void shouldRejectIncompleteState() {
        assertThatThrownBy(() -> Transaction.reconstitute(Transaction.PersistedState.builder()
                .transactionId(TransactionId.generate())
                .cardId(CARD_ID)
                .build()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cardData");
    }

    private static CardData cardData() {
        LocalDate expiration = LocalDate.now().plusYears(2);
        return CardData.of("tok_123", "0366", "Juan Pérez",
                ExpirationDate.of(expiration.getMonthValue(), expiration.getYear()));
    }

    private static MerchantInfo merchant() {
        return MerchantInfo.of("MERCH-001", "Supermercado Uno", "5411", "CABA", "AR");
    }
}
