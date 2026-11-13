package com.unisono.card.payment.application.service;

import com.unisono.card.payment.application.dto.request.ProcessRefundCommand;
import com.unisono.card.payment.application.mapper.RefundMapper;
import com.unisono.card.payment.domain.exception.InvalidTransactionException;
import com.unisono.card.payment.domain.exception.TransactionNotFoundException;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.port.output.PaymentEventPublisher;
import com.unisono.card.payment.domain.port.output.TransactionRepository;
import com.unisono.card.payment.domain.service.PaymentDomainService;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.unisono.card.payment.support.TestTransactions.CARD_ID;
import static com.unisono.card.payment.support.TestTransactions.MERCHANT_ID;
import static com.unisono.card.payment.support.TestTransactions.approvedPurchase;
import static com.unisono.card.payment.support.TestTransactions.approvedRefund;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundApplicationServiceTest {

    @Mock
    private PaymentDomainService paymentDomainService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PaymentEventPublisher eventPublisher;

    private final RefundMapper refundMapper = Mockito.mock(RefundMapper.class, Mockito.CALLS_REAL_METHODS);
    private RefundApplicationService service;
    private Transaction original;

    @BeforeEach
    void setUp() {
        service = new RefundApplicationService(paymentDomainService, transactionRepository, eventPublisher, refundMapper);
        original = approvedPurchase(10_000);
    }

    @Test
    void shouldRefundLockingTheOriginalAndPassingItsPreviousRefunds() {
        Transaction previous = approvedRefund(2_000, original.getTransactionId());
        Transaction refund = approvedRefund(3_000, original.getTransactionId());
        when(transactionRepository.findByIdForUpdate(original.getTransactionId())).thenReturn(Optional.of(original));
        when(transactionRepository.findRefundsOf(original.getTransactionId())).thenReturn(List.of(previous));
        when(paymentDomainService.processRefund(eq(original), eq(CARD_ID), any(), eq(List.of(previous)), eq(false)))
                .thenReturn(refund);
        when(transactionRepository.save(refund)).thenReturn(refund);

        Transaction result = service.processPartialRefund(command(3_000));

        assertThat(result).isSameAs(refund);
        ArgumentCaptor<TransactionAmount> amount = ArgumentCaptor.forClass(TransactionAmount.class);
        verify(paymentDomainService).processRefund(eq(original), eq(CARD_ID), amount.capture(), any(), eq(false));
        assertThat(amount.getValue().getEffectiveAmount()).isEqualTo(Money.ofARS(3_000));

        InOrder order = inOrder(transactionRepository, eventPublisher);
        order.verify(transactionRepository).findByIdForUpdate(original.getTransactionId());
        order.verify(transactionRepository).save(refund);
        order.verify(eventPublisher).publishAll(any());
        assertThat(refund.getDomainEvents()).isEmpty();
    }

    @Test
    void shouldAskForAFullRefund() {
        Transaction refund = approvedRefund(10_000, original.getTransactionId());
        when(transactionRepository.findByIdForUpdate(original.getTransactionId())).thenReturn(Optional.of(original));
        when(transactionRepository.findRefundsOf(original.getTransactionId())).thenReturn(List.of());
        when(paymentDomainService.processRefund(any(), any(), any(), any(), eq(true))).thenReturn(refund);
        when(transactionRepository.save(refund)).thenReturn(refund);

        service.processFullRefund(command(10_000));

        verify(paymentDomainService).processRefund(eq(original), eq(CARD_ID), any(), eq(List.of()), eq(true));
    }

    @Test
    void shouldPublishTheRefundEventsBeforeClearingThem() {
        Transaction refund = approvedRefund(3_000, original.getTransactionId());
        when(transactionRepository.findByIdForUpdate(any())).thenReturn(Optional.of(original));
        when(transactionRepository.findRefundsOf(any())).thenReturn(List.of());
        when(paymentDomainService.processRefund(any(), any(), any(), any(), anyBoolean())).thenReturn(refund);
        when(transactionRepository.save(refund)).thenReturn(refund);
        List<String> published = new java.util.ArrayList<>();
        Mockito.doAnswer(inv -> {
            ((Iterable<?>) inv.getArgument(0)).forEach(e -> published.add(e.getClass().getSimpleName()));
            return null;
        }).when(eventPublisher).publishAll(any());

        service.processPartialRefund(command(3_000));

        assertThat(published).containsExactly("RefundInitiated", "RefundProcessed");
    }

    @Test
    void shouldFailWhenTheOriginalDoesNotExist() {
        when(transactionRepository.findByIdForUpdate(original.getTransactionId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processPartialRefund(command(3_000)))
                .isInstanceOf(TransactionNotFoundException.class);
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishAll(any());
    }

    @Test
    void shouldNotSaveNorPublishWhenTheDomainRejectsTheRefund() {
        when(transactionRepository.findByIdForUpdate(any())).thenReturn(Optional.of(original));
        when(transactionRepository.findRefundsOf(any())).thenReturn(List.of());
        when(paymentDomainService.processRefund(any(), any(), any(), any(), anyBoolean()))
                .thenThrow(new InvalidTransactionException(original.getTransactionId(), "exceeds"));

        assertThatThrownBy(() -> service.processPartialRefund(command(50_000)))
                .isInstanceOf(InvalidTransactionException.class);
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishAll(any());
    }

    private ProcessRefundCommand command(long amount) {
        ProcessRefundCommand command = new ProcessRefundCommand();
        command.setOriginalTransactionId(original.getTransactionId().toString());
        command.setCardId(CARD_ID.getValue());
        command.setRefundAmount(BigDecimal.valueOf(amount));
        command.setCurrency("ARS");
        command.setMerchantId(MERCHANT_ID);
        command.setReason("Devolución de prueba");
        return command;
    }
}
