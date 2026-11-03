package com.unisono.card.payment.domain.model;

import com.unisono.card.payment.domain.event.RefundProcessed;
import com.unisono.card.payment.domain.exception.InvalidTransactionException;
import com.unisono.card.payment.domain.service.TransactionValidationService;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionRefundTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");
    private static final TransactionAmount AMOUNT = TransactionAmount.of(Money.ofARS(1500));

    @Test
    void shouldApprovePendingRefundWithoutAuthorization() {
        TransactionId original = TransactionId.generate();
        Transaction refund = refund();

        refund.approveRefund(original);

        assertThat(refund.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(refund.requiresSettlement()).isTrue();
        assertThat(refund.getDomainEvents())
                .filteredOn(RefundProcessed.class::isInstance)
                .singleElement()
                .satisfies(event -> assertThat(((RefundProcessed) event).getOriginalTransactionId()).isEqualTo(original));
    }

    @Test
    void shouldNotApproveRefundTwice() {
        Transaction refund = refund();
        refund.approveRefund(TransactionId.generate());

        assertThatThrownBy(() -> refund.approveRefund(TransactionId.generate()))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void shouldNotApprovePurchaseAsRefund() {
        Transaction purchase = Transaction.createPurchase(CARD_ID, cardData(), merchant(), AMOUNT, PaymentMethod.CHIP);

        assertThatThrownBy(() -> purchase.approveRefund(TransactionId.generate()))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Only refunds");
    }

    @Test
    void shouldCreateCashAdvanceWithItsType() {
        Transaction cashAdvance = Transaction.createCashAdvance(CARD_ID, cardData(), merchant(), AMOUNT, PaymentMethod.CHIP);

        assertThat(cashAdvance.getType()).isEqualTo(TransactionType.CASH_ADVANCE);
        assertThat(cashAdvance.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void shouldNotAllowReversingRefunds() {
        Transaction refund = refund();
        refund.approveRefund(TransactionId.generate());

        assertThatThrownBy(() -> new TransactionValidationService().validateCanReverse(refund))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("REFUND");
    }

    @Test
    void shouldAllowReversingApprovedPurchase() {
        Transaction purchase = Transaction.createPurchase(CARD_ID, cardData(), merchant(), AMOUNT, PaymentMethod.CHIP);
        purchase.authorize(AuthorizationCode.approved("AUTH01"), 10);
        purchase.approve();

        new TransactionValidationService().validateCanReverse(purchase);
    }

    private static Transaction refund() {
        return Transaction.createRefund(CARD_ID, cardData(), merchant(), AMOUNT, TransactionId.generate());
    }

    private static CardData cardData() {
        YearMonth expiration = YearMonth.now().plusYears(3);
        return CardData.of("tok_123", "0366", "Juan Pérez",
                ExpirationDate.of(expiration.getMonthValue(), expiration.getYear()));
    }

    private static MerchantInfo merchant() {
        return MerchantInfo.of("6f1c2d3e-0000-4000-8000-000000000001", "Supermercado Uno", "5411", "CABA", "AR");
    }
}
