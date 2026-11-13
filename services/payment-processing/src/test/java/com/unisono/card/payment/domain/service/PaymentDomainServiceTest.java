package com.unisono.card.payment.domain.service;

import com.unisono.card.payment.domain.model.PinBlock;
import com.unisono.card.payment.domain.event.PaymentDeclined;
import com.unisono.card.payment.domain.event.PaymentProcessed;
import com.unisono.card.payment.domain.event.RefundProcessed;
import com.unisono.card.payment.domain.exception.CardValidationException;
import com.unisono.card.payment.domain.exception.ExternalServiceException;
import com.unisono.card.payment.domain.exception.InsufficientFundsException;
import com.unisono.card.payment.domain.exception.InvalidTransactionException;
import com.unisono.card.payment.domain.exception.MerchantValidationException;
import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.CardData;
import com.unisono.card.payment.domain.model.CardVerification;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentDomainServiceTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");
    private static final String MERCHANT_ID = "6f1c2d3e-0000-4000-8000-000000000001";
    private static final Money AMOUNT = Money.ofARS(1500);
    private static final PinBlock PIN = new PinBlock("8E2C1F0A5B3D4E6F7A8B9C0D1E2F3A4B", "pek-v1");
    private static final String CVV = "123";
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
            lenient().when(cardValidationPort.validatePin(CARD_ID, PIN)).thenReturn(true);
        }

        @Test
        void shouldApprovePurchaseAndConsumePurchaseCredit() {
            Transaction transaction = pay(TransactionType.PURCHASE);

            assertThat(transaction.getType()).isEqualTo(TransactionType.PURCHASE);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.APPROVED);
            assertThat(transaction.getRiskScore()).isEqualTo(10);
            verify(creditLimitPort).checkCredit(CARD_ID, AMOUNT, TransactionType.PURCHASE);
            verify(creditLimitPort).consumeCredit(any(), eq(CARD_ID), eq(AMOUNT), eq(TransactionType.PURCHASE),
                    contains("Supermercado"));
        }

        @Test
        void shouldApproveCashAdvanceAndConsumeCashAdvanceCredit() {
            Transaction transaction = pay(TransactionType.CASH_ADVANCE);

            assertThat(transaction.getType()).isEqualTo(TransactionType.CASH_ADVANCE);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.APPROVED);
            verify(creditLimitPort).checkCredit(CARD_ID, AMOUNT, TransactionType.CASH_ADVANCE);
            verify(creditLimitPort).consumeCredit(any(), eq(CARD_ID), eq(AMOUNT), eq(TransactionType.CASH_ADVANCE), anyString());
        }

        @Test
        void shouldAuthorizeBeforeConsumingCredit() {
            pay(TransactionType.PURCHASE);

            InOrder order = inOrder(authorizationPort, creditLimitPort);
            order.verify(authorizationPort).requestAuthorization(any());
            order.verify(creditLimitPort).consumeCredit(any(), any(), any(), any(), anyString());
        }

        @Test
        void shouldDeclineWithNetworkCodeAndNotConsumeCreditWhenAuthorizationIsDeclined() {
            when(authorizationPort.requestAuthorization(any())).thenReturn(AuthorizationCode.declined("61"));

            Transaction transaction = pay(TransactionType.PURCHASE);

            assertDeclined(transaction, "61");
            assertThat(transaction.getDeclineReason()).contains("61");
            verify(creditLimitPort, never()).consumeCredit(any(), any(), any(), any(), anyString());
        }

        @Test
        void shouldRejectUnknownCardWithoutCreatingTransaction() {
            when(cardValidationPort.findCardStatus(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pay(TransactionType.PURCHASE))
                    .isInstanceOf(CardValidationException.class)
                    .hasMessageContaining("not found");
            verifyNoInteractions(creditLimitPort, authorizationPort);
        }

        @Test
        void shouldDeclineBlockedCard() {
            when(cardValidationPort.findCardStatus(CARD_ID))
                    .thenReturn(Optional.of(new CardStatusSnapshot("BLOCKED", true, false)));

            Transaction transaction = pay(TransactionType.PURCHASE);

            assertDeclined(transaction, "62");
            assertThat(transaction.getDeclineReason()).contains("blocked");
            verifyNoInteractions(creditLimitPort, authorizationPort);
        }

        @Test
        void shouldDeclineCardThatIsNotActive() {
            when(cardValidationPort.findCardStatus(CARD_ID))
                    .thenReturn(Optional.of(new CardStatusSnapshot("ISSUED", false, false)));

            Transaction transaction = pay(TransactionType.PURCHASE);

            assertDeclined(transaction, "62");
            assertThat(transaction.getDeclineReason()).contains("ISSUED");
        }

        @Test
        void shouldDeclineCardExpiredInCardData() {
            when(cardValidationPort.findCardStatus(CARD_ID))
                    .thenReturn(Optional.of(new CardStatusSnapshot("ACTIVE", false, true)));

            Transaction transaction = pay(TransactionType.PURCHASE);

            assertDeclined(transaction, "54");
            assertThat(transaction.getDeclineReason()).contains("expired");
        }

        @Test
        void shouldRejectInactiveMerchantWithoutCreatingTransaction() {
            when(merchantValidationPort.isMerchantActive(MERCHANT_ID)).thenReturn(false);

            assertThatThrownBy(() -> pay(TransactionType.PURCHASE))
                    .isInstanceOf(MerchantValidationException.class);
            verifyNoInteractions(creditLimitPort);
        }

        @Test
        void shouldDeclineWhenCardLimitsDeniesCredit() {
            when(creditLimitPort.checkCredit(CARD_ID, AMOUNT, TransactionType.PURCHASE))
                    .thenReturn(new CreditCheck(false, "Se excede el límite diario", Money.ofARS(300)));

            Transaction transaction = pay(TransactionType.PURCHASE);

            assertDeclined(transaction, "51");
            assertThat(transaction.getDeclineReason()).contains("Se excede el límite diario");
            verify(creditLimitPort, never()).consumeCredit(any(), any(), any(), any(), anyString());
            verifyNoInteractions(authorizationPort);
        }

        @Test
        void shouldDeclineSuspiciousTransaction() {
            when(fraudCheckPort.calculateRiskScore(any())).thenReturn(85);
            when(fraudCheckPort.isSuspicious(any(), eq(85))).thenReturn(true);

            Transaction transaction = pay(TransactionType.PURCHASE);

            assertDeclined(transaction, "59");
            assertThat(transaction.getRiskScore()).isEqualTo(85);
            verifyNoInteractions(authorizationPort);
        }

        @Test
        void shouldCalculateRiskScoreOnceAndEvaluateTheStoredValue() {
            Transaction transaction = pay(TransactionType.PURCHASE);

            verify(fraudCheckPort, times(1)).calculateRiskScore(any());
            verify(fraudCheckPort).isSuspicious(transaction, 10);
            assertThat(transaction.getRiskScore()).isEqualTo(10);
        }

        // ---------- Preautorización y captura ----------

        @Test
        void shouldPreAuthorizeHoldingCreditWithoutApproving() {
            Transaction transaction = service.preAuthorize(CARD_ID, cardData(), merchant(),
                    TransactionAmount.of(AMOUNT), PaymentMethod.CHIP, CardVerification.none());

            assertThat(transaction.getType()).isEqualTo(TransactionType.PURCHASE);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);
            assertThat(transaction.getDomainEvents()).noneMatch(PaymentProcessed.class::isInstance);
            verify(creditLimitPort).consumeCredit(any(), eq(CARD_ID), eq(AMOUNT), eq(TransactionType.PURCHASE), anyString());
        }

        @Test
        void shouldDeclinePreAuthorizationLikeAPurchase() {
            when(creditLimitPort.checkCredit(CARD_ID, AMOUNT, TransactionType.PURCHASE))
                    .thenReturn(new CreditCheck(false, "Sin disponible", Money.ofARS(0)));

            Transaction transaction = service.preAuthorize(CARD_ID, cardData(), merchant(),
                    TransactionAmount.of(AMOUNT), PaymentMethod.CHIP, CardVerification.none());

            assertDeclined(transaction, "51");
        }

        @Test
        void shouldDeclineAndVoidAuthorizationWhenCreditConsumptionIsRejected() {
            doThrow(new InsufficientFundsException(CARD_ID, AMOUNT, Money.ofARS(0), "Sin disponible"))
                    .when(creditLimitPort).consumeCredit(any(), any(), any(), any(), anyString());

            Transaction transaction = pay(TransactionType.PURCHASE);

            assertDeclined(transaction, "51");
            verify(authorizationPort).voidAuthorization(transaction);
        }

        @Test
        void shouldVerifyPinOfCashAdvanceBeforeCheckingCredit() {
            pay(TransactionType.CASH_ADVANCE);

            InOrder order = inOrder(cardValidationPort, creditLimitPort);
            order.verify(cardValidationPort).validatePin(CARD_ID, PIN);
            order.verify(creditLimitPort).checkCredit(any(), any(), any());
        }

        @Test
        void shouldDeclineCashAdvanceWithIncorrectPin() {
            when(cardValidationPort.validatePin(CARD_ID, PIN)).thenReturn(false);

            Transaction transaction = pay(TransactionType.CASH_ADVANCE);

            assertDeclined(transaction, "55");
            verifyNoInteractions(creditLimitPort, authorizationPort);
        }

        @Test
        void shouldDeclineCashAdvanceWhenCardDataCannotDecryptThePinBlock() {
            when(cardValidationPort.validatePin(CARD_ID, PIN)).thenThrow(CardValidationException.invalidPinBlock(CARD_ID));

            Transaction transaction = pay(TransactionType.CASH_ADVANCE);

            assertDeclined(transaction, "55");
            assertThat(transaction.getDeclineReason()).contains("Invalid PIN block");
            verifyNoInteractions(creditLimitPort, authorizationPort);
        }

        @Test
        void shouldDeclineCashAdvanceWithoutPin() {
            Transaction transaction = service.processPayment(CARD_ID, cardData(), merchant(),
                    TransactionAmount.of(AMOUNT), PaymentMethod.CHIP, TransactionType.CASH_ADVANCE, CardVerification.none());

            assertDeclined(transaction, "55");
            verify(cardValidationPort, never()).validatePin(any(), any());
        }

        @Test
        void shouldNotAskForPinOrCvvOnCardPresentPurchase() {
            pay(TransactionType.PURCHASE);

            verify(cardValidationPort, never()).validatePin(any(), any());
            verify(cardValidationPort, never()).validateCvv(any(), any());
        }

        // ---------- CVV ----------

        @Test
        void shouldVerifyCvvOfOnlinePurchase() {
            when(cardValidationPort.validateCvv(CARD_ID, CVV)).thenReturn(true);

            Transaction transaction = payWith(PaymentMethod.ONLINE, CardVerification.ofCvv(CVV));

            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.APPROVED);
            verify(cardValidationPort).validateCvv(CARD_ID, CVV);
        }

        @Test
        void shouldDeclineOnlinePurchaseWithIncorrectCvv() {
            when(cardValidationPort.validateCvv(CARD_ID, CVV)).thenReturn(false);

            Transaction transaction = payWith(PaymentMethod.ONLINE, CardVerification.ofCvv(CVV));

            assertDeclined(transaction, "N7");
            verifyNoInteractions(creditLimitPort, authorizationPort);
        }

        @Test
        void shouldDeclineManualPurchaseWithoutCvv() {
            Transaction transaction = payWith(PaymentMethod.MANUAL, CardVerification.none());

            assertDeclined(transaction, "N7");
            verify(cardValidationPort, never()).validateCvv(any(), any());
        }

        @Test
        void shouldVerifyCvvWhenSentOnCardPresentPurchase() {
            when(cardValidationPort.validateCvv(CARD_ID, CVV)).thenReturn(false);

            Transaction transaction = payWith(PaymentMethod.CHIP, CardVerification.ofCvv(CVV));

            assertDeclined(transaction, "N7");
        }

        @Test
        void shouldVerifyCvvOfPreAuthorization() {
            when(cardValidationPort.validateCvv(CARD_ID, CVV)).thenReturn(false);

            Transaction transaction = service.preAuthorize(CARD_ID, cardData(), merchant(),
                    TransactionAmount.of(AMOUNT), PaymentMethod.ONLINE, CardVerification.ofCvv(CVV));

            assertDeclined(transaction, "N7");
        }

        private Transaction payWith(PaymentMethod method, CardVerification verification) {
            return service.processPayment(CARD_ID, cardData(), merchant(), TransactionAmount.of(AMOUNT),
                    method, TransactionType.PURCHASE, verification);
        }

        private void assertDeclined(Transaction transaction, String responseCode) {
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.DECLINED);
            assertThat(transaction.getAuthorizationCode().getResponseCode()).isEqualTo(responseCode);
            assertThat(transaction.getDomainEvents()).anyMatch(PaymentDeclined.class::isInstance);
        }

        private Transaction pay(TransactionType type) {
            return service.processPayment(CARD_ID, cardData(), merchant(), TransactionAmount.of(AMOUNT),
                    PaymentMethod.CHIP, type, CardVerification.ofPin(PIN));
        }
    }

    // ========== Devoluciones, reversos y liquidación ==========

    // ---------- Devoluciones ----------

    @Nested
    class ProcessRefund {

        @BeforeEach
        void activeCard() {
            lenient().when(cardValidationPort.findCardStatus(CARD_ID)).thenReturn(Optional.of(ACTIVE_CARD));
        }

        @Test
        void shouldApproveFullRefundLinkedToOriginalAndRestoreCredit() {
            Transaction original = approvedPurchase();

            Transaction refund = service.processRefund(original, CARD_ID, TransactionAmount.of(AMOUNT), List.of(), true);

            assertThat(refund.getType()).isEqualTo(TransactionType.REFUND);
            assertThat(refund.getStatus()).isEqualTo(TransactionStatus.APPROVED);
            assertThat(refund.getOriginalTransactionId()).isEqualTo(original.getTransactionId());
            assertThat(refund.getDomainEvents()).anyMatch(RefundProcessed.class::isInstance);
            verify(creditLimitPort).restoreCredit(any(), eq(CARD_ID), eq(AMOUNT),
                    eq(original.getTransactionDate().toLocalDate()), contains(original.getTransactionId().toString()));
        }

        @Test
        void shouldAllowPartialRefundsUpToTheOriginalAmount() {
            Transaction original = approvedPurchase();
            Transaction first = service.processRefund(original, CARD_ID, TransactionAmount.of(Money.ofARS(1000)),
                    List.of(), false);

            Transaction second = service.processRefund(original, CARD_ID, TransactionAmount.of(Money.ofARS(500)),
                    List.of(first), false);

            assertThat(second.getStatus()).isEqualTo(TransactionStatus.APPROVED);
            assertThat(service.remainingToRefund(original, List.of(first, second)).isZero()).isTrue();
        }

        @Test
        void shouldRejectPartialRefundAboveWhatIsLeft() {
            Transaction original = approvedPurchase();
            Transaction first = service.processRefund(original, CARD_ID, TransactionAmount.of(Money.ofARS(1000)),
                    List.of(), false);

            assertThatThrownBy(() -> service.processRefund(original, CARD_ID,
                    TransactionAmount.of(Money.ofARS(600)), List.of(first), false))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("not yet refunded");
        }

        @Test
        void shouldRequireFullRefundToMatchWhatIsLeft() {
            Transaction original = approvedPurchase();
            Transaction first = service.processRefund(original, CARD_ID, TransactionAmount.of(Money.ofARS(1000)),
                    List.of(), false);

            assertThatThrownBy(() -> service.processRefund(original, CARD_ID,
                    TransactionAmount.of(AMOUNT), List.of(first), true))
                    .isInstanceOf(InvalidTransactionException.class);

            Transaction rest = service.processRefund(original, CARD_ID,
                    TransactionAmount.of(Money.ofARS(500)), List.of(first), true);
            assertThat(rest.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        }

        @Test
        void shouldRejectRefundOfFullyRefundedTransaction() {
            Transaction original = approvedPurchase();
            Transaction full = service.processRefund(original, CARD_ID, TransactionAmount.of(AMOUNT), List.of(), true);

            assertThatThrownBy(() -> service.processRefund(original, CARD_ID,
                    TransactionAmount.of(Money.ofARS(1)), List.of(full), false))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("already fully refunded");
        }

        @Test
        void shouldRejectRefundToAnotherCard() {
            Transaction original = approvedPurchase();
            CardId otherCard = CardId.of("660e8400-e29b-41d4-a716-446655440000");

            assertThatThrownBy(() -> service.processRefund(original, otherCard,
                    TransactionAmount.of(AMOUNT), List.of(), true))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("card");
            verifyNoInteractions(creditLimitPort);
        }

        @Test
        void shouldRejectRefundOfTransactionThatIsNotApproved() {
            Transaction preAuthorized = authorizedPurchase();

            assertThatThrownBy(() -> service.processRefund(preAuthorized, CARD_ID,
                    TransactionAmount.of(AMOUNT), List.of(), true))
                    .isInstanceOf(InvalidTransactionException.class);
        }
    }

    @Test
    void shouldReverseApprovedPurchaseAndReleaseCredit() {
        Transaction purchase = approvedPurchase();

        service.reverseTransaction(purchase, "Cliente desistió");

        assertThat(purchase.getStatus()).isEqualTo(TransactionStatus.REVERSED);
        verify(creditLimitPort).restoreCredit(any(), eq(CARD_ID), eq(AMOUNT),
                eq(purchase.getTransactionDate().toLocalDate()), contains("Cliente desistió"));
    }

    @Test
    void shouldReversePreAuthorizationVoidingItAndReleasingTheHold() {
        Transaction preAuthorized = authorizedPurchase();

        service.reverseTransaction(preAuthorized, "Check-out sin cargo");

        assertThat(preAuthorized.getStatus()).isEqualTo(TransactionStatus.REVERSED);
        verify(authorizationPort).voidAuthorization(preAuthorized);
        verify(creditLimitPort).restoreCredit(any(), eq(CARD_ID), eq(AMOUNT), any(), anyString());
    }

    @Test
    void shouldNotVoidAuthorizationWhenReversingApprovedPurchase() {
        service.reverseTransaction(approvedPurchase(), "Cliente desistió");

        verify(authorizationPort, never()).voidAuthorization(any());
    }

    @Test
    void shouldCaptureAuthorizedTransactionWithoutConsumingCreditAgain() {
        Transaction preAuthorized = authorizedPurchase();

        service.captureTransaction(preAuthorized);

        assertThat(preAuthorized.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(preAuthorized.getDomainEvents()).anyMatch(PaymentProcessed.class::isInstance);
        verify(authorizationPort).captureAuthorization(preAuthorized);
        verifyNoInteractions(creditLimitPort);
    }

    @Test
    void shouldRejectCaptureOfTransactionThatIsNotAuthorized() {
        Transaction approved = approvedPurchase();

        assertThatThrownBy(() -> service.captureTransaction(approved))
                .isInstanceOf(InvalidTransactionException.class);
        verifyNoInteractions(authorizationPort);
    }

    @Test
    void shouldKeepTransactionAuthorizedWhenNetworkCaptureFails() {
        Transaction preAuthorized = authorizedPurchase();
        doThrow(new RuntimeException("timeout")).when(authorizationPort).captureAuthorization(preAuthorized);

        assertThatThrownBy(() -> service.captureTransaction(preAuthorized))
                .isInstanceOf(ExternalServiceException.class);
        assertThat(preAuthorized.getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);
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
        Transaction transaction = authorizedPurchase();
        transaction.approve();
        return transaction;
    }

    private static Transaction authorizedPurchase() {
        Transaction transaction = Transaction.createPurchase(CARD_ID, cardData(), merchant(),
                TransactionAmount.of(AMOUNT), PaymentMethod.CHIP);
        transaction.authorize(AuthorizationCode.approved("AUTH01"), 10);
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
