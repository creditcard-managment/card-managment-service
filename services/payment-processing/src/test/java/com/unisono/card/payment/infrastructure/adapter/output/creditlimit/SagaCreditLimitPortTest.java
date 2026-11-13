package com.unisono.card.payment.infrastructure.adapter.output.creditlimit;

import com.unisono.card.payment.domain.exception.ExternalServiceException;
import com.unisono.card.payment.domain.exception.InsufficientFundsException;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionType;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentCreditOperationEntity.Type;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Saga de crédito: registro previo, confirmación con el pago y compensación cuando el pago no confirma
 */
@ExtendWith(MockitoExtension.class)
class SagaCreditLimitPortTest {

    private static final TransactionId TRANSACTION_ID = TransactionId.generate();
    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");
    private static final Money AMOUNT = Money.ofARS(1_500);
    private static final String OPERATION_ID = "op-1";

    @Mock
    private CreditLimitAdapter adapter;
    @Mock
    private CreditOperationJournal journal;
    @Mock
    private CreditOperationCompensator compensator;

    private SagaCreditLimitPort port;

    @BeforeEach
    void setUp() {
        port = new SagaCreditLimitPort(adapter, journal, compensator);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void shouldRegisterLockCallAndConfirmAConsumption() {
        when(journal.begin(TRANSACTION_ID, CARD_ID, Type.CONSUME_PURCHASE, AMOUNT)).thenReturn(OPERATION_ID);

        port.consumeCredit(TRANSACTION_ID, CARD_ID, AMOUNT, TransactionType.PURCHASE, "Compra");

        InOrder order = inOrder(journal, adapter);
        order.verify(journal).begin(TRANSACTION_ID, CARD_ID, Type.CONSUME_PURCHASE, AMOUNT);
        order.verify(journal).lock(OPERATION_ID);
        order.verify(adapter).consumeCredit(OPERATION_ID, CARD_ID, AMOUNT, TransactionType.PURCHASE, "Compra");
        order.verify(journal).confirm(OPERATION_ID);
    }

    @Test
    void shouldRegisterCashAdvancesWithTheirType() {
        when(journal.begin(TRANSACTION_ID, CARD_ID, Type.CONSUME_CASH_ADVANCE, AMOUNT)).thenReturn(OPERATION_ID);

        port.consumeCredit(TRANSACTION_ID, CARD_ID, AMOUNT, TransactionType.CASH_ADVANCE, "Adelanto");

        verify(adapter).consumeCredit(OPERATION_ID, CARD_ID, AMOUNT, TransactionType.CASH_ADVANCE, "Adelanto");
        verify(journal).confirm(OPERATION_ID);
    }

    @Test
    void shouldMarkRejectedConsumptionsWithoutConfirmingThem() {
        when(journal.begin(any(), any(), any(), any())).thenReturn(OPERATION_ID);
        InsufficientFundsException rejected = new InsufficientFundsException(CARD_ID, AMOUNT, Money.ofARS(0), "x");
        doThrow(rejected).when(adapter).consumeCredit(eq(OPERATION_ID), any(), any(), any(), anyString());

        assertThatThrownBy(() -> port.consumeCredit(TRANSACTION_ID, CARD_ID, AMOUNT, TransactionType.PURCHASE, "Compra"))
                .isSameAs(rejected);

        verify(journal).reject(OPERATION_ID);
        verify(journal, never()).confirm(anyString());
    }

    @Test
    void shouldNotConfirmWhenCardLimitsFails() {
        when(journal.begin(any(), any(), any(), any())).thenReturn(OPERATION_ID);
        doThrow(new ExternalServiceException("card-limits", "timeout", null))
                .when(adapter).restoreCredit(eq(OPERATION_ID), any(), any(), any(), anyString());

        assertThatThrownBy(() -> port.restoreCredit(TRANSACTION_ID, CARD_ID, AMOUNT, LocalDate.now(), "Reverso"))
                .isInstanceOf(ExternalServiceException.class);

        verify(journal, never()).confirm(anyString());
        verify(journal, never()).reject(anyString());
    }

    @Test
    void shouldCompensateWhenThePaymentTransactionRollsBack() {
        when(journal.begin(TRANSACTION_ID, CARD_ID, Type.RESTORE, AMOUNT)).thenReturn(OPERATION_ID);
        port.restoreCredit(TRANSACTION_ID, CARD_ID, AMOUNT, LocalDate.now(), "Reverso");

        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(compensator).compensate(OPERATION_ID, CARD_ID);
    }

    @Test
    void shouldNotCompensateWhenThePaymentTransactionCommits() {
        when(journal.begin(any(), any(), any(), any())).thenReturn(OPERATION_ID);
        port.consumeCredit(TRANSACTION_ID, CARD_ID, AMOUNT, TransactionType.PURCHASE, "Compra");

        complete(TransactionSynchronization.STATUS_COMMITTED);

        verifyNoInteractions(compensator);
    }

    @Test
    void shouldLeaveUnknownOutcomesToTheReconciler() {
        when(journal.begin(any(), any(), any(), any())).thenReturn(OPERATION_ID);
        port.consumeCredit(TRANSACTION_ID, CARD_ID, AMOUNT, TransactionType.PURCHASE, "Compra");

        complete(TransactionSynchronization.STATUS_UNKNOWN);

        verifyNoInteractions(compensator);
    }

    @Test
    void shouldRefuseCreditOperationsOutsideATransaction() {
        TransactionSynchronizationManager.clearSynchronization();
        try {
            assertThatThrownBy(() -> port.consumeCredit(TRANSACTION_ID, CARD_ID, AMOUNT, TransactionType.PURCHASE, "x"))
                    .isInstanceOf(IllegalStateException.class);
            verifyNoInteractions(adapter, journal);
        } finally {
            TransactionSynchronizationManager.initSynchronization();
        }
    }

    private static void complete(int status) {
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(status));
    }
}
