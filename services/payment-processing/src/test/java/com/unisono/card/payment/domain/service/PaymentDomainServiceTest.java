package com.unisono.card.payment.domain.service;

import com.unisono.card.payment.domain.event.RefundProcessed;
import com.unisono.card.payment.domain.exception.CardValidationException;
import com.unisono.card.payment.domain.exception.InsufficientFundsException;
import com.unisono.card.payment.domain.exception.InvalidTransactionException;
import com.unisono.card.payment.domain.exception.MerchantValidationException;
import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.CardData;
import com.unisono.card.payment.domain.model.MerchantInfo;
import com.unisono.card.payment.domain.model.PaymentMethod;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.model.TransactionType;
import com.unisono.card.payment.domain.port.output.AuthorizationPort;
import com.unisono.card.payment.domain.port.output.CardValidationPort;
import com.unisono.card.payment.domain.port.output.CardValidationPort.CardStatusSnapshot;
import com.unisono.card.payment.domain.port.output.CreditLimitPort;
import com.unisono.card.payment.domain.port.output.CreditLimitPort.CreditCheck;
import com.unisono.card.payment.domain.port.output.FraudCheckPort;
import com.unisono.card.payment.domain.port.output.MerchantValidationPort;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentDomainServiceTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");
    private static final String MERCHANT_ID = "6f1c2d3e-0000-4000-8000-000000000001";
    private static final Money AMOUNT = Money.ofARS(1500);
    private static final CardStatusSnapshot ACTIVE_CARD = new CardStatusSnapshot("ACTIVE", false, false);

    @Mock
    private CardValidationPort cardValidationPort;
    @Mock
    private CreditLimitPort creditLimitPort;
    @Mock
    private MerchantValidationPort merchantValidationPort;
    @Mock
    private FraudCheckPort fraudCheckPort;
    @Mock
    private AuthorizationPort authorizationPort;

    private PaymentDomainService service;

    @BeforeEach
    void setUp() {
        service = new PaymentDomainService(cardValidationPort, creditLimitPort, merchantValidationPort,
                fraudCheckPort, authorizationPort);
    }

    // ========== Compras y adelantos ==========

    @Nested
    class ProcessPayment {

        @BeforeEach
        void happyPath() {
            lenient().when(cardValidationPort.findCardStatus(CARD_ID)).thenReturn(Optional.of(ACTIVE_CARD));
            lenient().when(merchantValidationPort.isMerchantActive(MERCHANT_ID)).thenReturn(true);
            lenient().when(merchantValidationPort.isValidMCC("5411")).thenReturn(true);
            lenient().when(creditLimitPort.checkCredit(eq(CARD_ID), eq(AMOUNT), any()))
                    .thenReturn(new CreditCheck(true, null, Money.ofARS(100000)));
            lenient().when(authorizationPort.requestAuthorization(any())).thenReturn(AuthorizationCode.approved("AUTH01"));
            lenient().when(fraudCheckPort.calculateRiskScore(any())).thenReturn(10);
        }

        @Test
        void shouldApprovePurchaseAndConsumePurchaseCredit() {
            Transaction transaction = pay(TransactionType.PURCHASE);

            assertThat(transaction.getType()).isEqualTo(TransactionType.PURCHASE);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.APPROVED);
            assertThat(transaction.getRiskScore()).isEqualTo(10);
            verify(creditLimitPort).checkCredit(CARD_ID, AMOUNT, TransactionType.PURCHASE);
            verify(creditLimitPort).consumeCredit(eq(CARD_ID), eq(AMOUNT), eq(TransactionType.PURCHASE),
                    contains("Supermercado"));
        }

        @Test
        void shouldApproveCashAdvanceAndConsumeCashAdvanceCredit() {
            Transaction transaction = pay(TransactionType.CASH_ADVANCE);

            assertThat(transaction.getType()).isEqualTo(TransactionType.CASH_ADVANCE);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.APPROVED);
            verify(creditLimitPort).checkCredit(CARD_ID, AMOUNT, TransactionType.CASH_ADVANCE);
            verify(creditLimitPort).consumeCredit(eq(CARD_ID), eq(AMOUNT), eq(TransactionType.CASH_ADVANCE), anyString());
        }

        @Test
        void shouldAuthorizeBeforeConsumingCredit() {
            pay(TransactionType.PURCHASE);

            InOrder order = inOrder(authorizationPort, creditLimitPort);
            order.verify(authorizationPort).requestAuthorization(any());
            order.verify(creditLimitPort).consumeCredit(any(), any(), any(), anyString());
        }

        @Test
        void shouldNotConsumeCreditWhenAuthorizationIsDeclined() {
            when(authorizationPort.requestAuthorization(any())).thenReturn(AuthorizationCode.declined("51"));

            assertThatThrownBy(() -> pay(TransactionType.PURCHASE))
                    .isInstanceOf(InvalidTransactionException.class);

            verify(creditLimitPort, never()).consumeCredit(any(), any(), any(), anyString());
        }

        @Test
        void shouldRejectUnknownCard() {
            when(cardValidationPort.findCardStatus(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pay(TransactionType.PURCHASE))
                    .isInstanceOf(CardValidationException.class)
                    .hasMessageContaining("not found");
            verifyNoInteractions(creditLimitPort, authorizationPort);
        }

        @Test
        void shouldRejectBlockedCard() {
            when(cardValidationPort.findCardStatus(CARD_ID))
                    .thenReturn(Optional.of(new CardStatusSnapshot("BLOCKED", true, false)));

            assertThatThrownBy(() -> pay(TransactionType.PURCHASE))
                    .isInstanceOf(CardValidationException.class)
                    .hasMessageContaining("blocked");
        }

        @Test
        void shouldRejectCardThatIsNotActive() {
            when(cardValidationPort.findCardStatus(CARD_ID))
                    .thenReturn(Optional.of(new CardStatusSnapshot("ISSUED", false, false)));

            assertThatThrownBy(() -> pay(TransactionType.PURCHASE))
                    .isInstanceOf(CardValidationException.class)
                    .hasMessageContaining("ISSUED");
        }

        @Test
        void shouldRejectCardExpiredInCardData() {
            when(cardValidationPort.findCardStatus(CARD_ID))
                    .thenReturn(Optional.of(new CardStatusSnapshot("ACTIVE", false, true)));

            assertThatThrownBy(() -> pay(TransactionType.PURCHASE))
                    .isInstanceOf(CardValidationException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        void shouldRejectInactiveMerchant() {
            when(merchantValidationPort.isMerchantActive(MERCHANT_ID)).thenReturn(false);

            assertThatThrownBy(() -> pay(TransactionType.PURCHASE))
                    .isInstanceOf(MerchantValidationException.class);
            verifyNoInteractions(creditLimitPort);
        }

        @Test
        void shouldRejectWhenCardLimitsDeniesCredit() {
            when(creditLimitPort.checkCredit(CARD_ID, AMOUNT, TransactionType.PURCHASE))
                    .thenReturn(new CreditCheck(false, "Se excede el límite diario", Money.ofARS(300)));

            assertThatThrownBy(() -> pay(TransactionType.PURCHASE))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Se excede el límite diario");
            verify(creditLimitPort, never()).consumeCredit(any(), any(), any(), anyString());
            verifyNoInteractions(authorizationPort);
        }

        private Transaction pay(TransactionType type) {
            return service.processPayment(CARD_ID, cardData(), merchant(), TransactionAmount.of(AMOUNT),
                    PaymentMethod.CHIP, type);
        }
    }

    // ========== Devoluciones, reversos y liquidación ==========

    @Test
    void shouldApproveRefundAndReleaseCredit() {
        when(cardValidationPort.findCardStatus(CARD_ID)).thenReturn(Optional.of(ACTIVE_CARD));
        TransactionId original = TransactionId.generate();

        Transaction refund = service.processRefund(CARD_ID, cardData(), merchant(), TransactionAmount.of(AMOUNT), original);

        assertThat(refund.getType()).isEqualTo(TransactionType.REFUND);
        assertThat(refund.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(refund.getDomainEvents()).anyMatch(RefundProcessed.class::isInstance);
        verify(creditLimitPort).releaseCredit(eq(CARD_ID), eq(AMOUNT),
                contains(original.toString()));
    }

    @Test
    void shouldReverseApprovedPurchaseAndReleaseCredit() {
        Transaction purchase = approvedPurchase();

        service.reverseTransaction(purchase, "Cliente desistió");

        assertThat(purchase.getStatus()).isEqualTo(TransactionStatus.REVERSED);
        verify(creditLimitPort).releaseCredit(eq(CARD_ID), eq(AMOUNT),
                contains("Cliente desistió"));
    }

    @Test
    void shouldSettleWithoutTouchingCredit() {
        Transaction purchase = approvedPurchase();

        service.settleTransaction(purchase);

        assertThat(purchase.getStatus()).isEqualTo(TransactionStatus.SETTLED);
        verifyNoInteractions(creditLimitPort);
    }

    @Test
    void shouldRejectSettlementOfTransactionThatIsNotApproved() {
        Transaction pending = Transaction.createPurchase(CARD_ID, cardData(), merchant(),
                TransactionAmount.of(AMOUNT), PaymentMethod.CHIP);

        assertThatThrownBy(() -> service.settleTransaction(pending))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void shouldResolveTerminalMerchant() {
        when(merchantValidationPort.findMerchantIdByTerminal("ATM-01")).thenReturn(Optional.of(MERCHANT_ID));

        assertThat(service.resolveTerminalMerchant("ATM-01")).isEqualTo(MERCHANT_ID);
    }

    @Test
    void shouldRejectUnknownTerminal() {
        when(merchantValidationPort.findMerchantIdByTerminal("ATM-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveTerminalMerchant("ATM-99"))
                .isInstanceOf(MerchantValidationException.class)
                .hasMessageContaining("ATM-99");
    }

    // ========== Helpers ==========

    private static Transaction approvedPurchase() {
        Transaction transaction = Transaction.createPurchase(CARD_ID, cardData(), merchant(),
                TransactionAmount.of(AMOUNT), PaymentMethod.CHIP);
        transaction.authorize(AuthorizationCode.approved("AUTH01"), 10);
        transaction.approve();
        return transaction;
    }

    private static CardData cardData() {
        YearMonth expiration = YearMonth.now().plusYears(3);
        return CardData.of("tok_123", "0366", "Juan Pérez",
                ExpirationDate.of(expiration.getMonthValue(), expiration.getYear()));
    }

    private static MerchantInfo merchant() {
        return MerchantInfo.of(MERCHANT_ID, "Supermercado Uno", "5411", "CABA", "AR");
    }
}
