package com.unisono.card.payment.infrastructure.adapter.output.creditlimit;

import com.unisono.card.payment.infrastructure.observability.ConsistencyMonitor;
import com.unisono.card.payment.domain.exception.ExternalServiceException;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentCreditOperationEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentCreditOperationEntity.Status;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentCreditOperationRepository;
import com.unisono.card.shared.domain.valueobject.CardId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditOperationReconcilerTest {

    private static final String CARD = "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private JpaPaymentCreditOperationRepository repository;
    @Mock
    private CreditLimitAdapter adapter;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private ConsistencyMonitor monitor;

    private CreditOperationReconciler reconciler;

    @BeforeEach
    void setUp() {
        reconciler = new CreditOperationReconciler(repository, adapter, monitor, transactionManager);
    }

    @Test
    void shouldCancelStalePendingOperationsInCardLimits() {
        PaymentCreditOperationEntity first = pending("op-1");
        PaymentCreditOperationEntity second = pending("op-2");
        when(repository.lockStalePending(any(), eq(20))).thenReturn(List.of(first, second));
        when(adapter.cancelOperation(any(), eq(CardId.of(CARD)))).thenReturn("CANCELLED");

        int compensated = reconciler.reconcileBatch();

        assertThat(compensated).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(Status.COMPENSATED);
        assertThat(second.getStatus()).isEqualTo(Status.COMPENSATED);
        assertThat(first.getAttempts()).isEqualTo(1);
        verify(repository).saveAll(List.of(first, second));
        verify(monitor, org.mockito.Mockito.times(2)).compensationSucceeded(ConsistencyMonitor.SOURCE_RECONCILER);
    }

    @Test
    void shouldKeepTheOperationPendingAndContinueWhenACancellationFails() {
        PaymentCreditOperationEntity failing = pending("op-1");
        PaymentCreditOperationEntity next = pending("op-2");
        when(repository.lockStalePending(any(), eq(20))).thenReturn(List.of(failing, next));
        when(adapter.cancelOperation(eq("op-1"), any()))
                .thenThrow(new ExternalServiceException("card-limits", "Connection refused", null));
        when(adapter.cancelOperation(eq("op-2"), any())).thenReturn("VOIDED");

        int compensated = reconciler.reconcileBatch();

        assertThat(compensated).isEqualTo(1);
        assertThat(failing.getStatus()).isEqualTo(Status.PENDING);
        assertThat(failing.getAttempts()).isEqualTo(1);
        assertThat(failing.getLastError()).contains("Connection refused");
        assertThat(next.getStatus()).isEqualTo(Status.COMPENSATED);
        verify(monitor).compensationFailed(ConsistencyMonitor.SOURCE_RECONCILER, "op-1", 1);
    }

    @Test
    void shouldOnlyLookAtOperationsOlderThanTheThreshold() {
        when(repository.lockStalePending(any(), eq(20))).thenReturn(List.of());

        reconciler.reconcileBatch();

        verify(repository).lockStalePending(org.mockito.ArgumentMatchers.argThat(
                before -> before.isBefore(LocalDateTime.now().minusSeconds(59))), eq(20));
    }

    private static PaymentCreditOperationEntity pending(String operationId) {
        LocalDateTime created = LocalDateTime.now().minusMinutes(5);
        return PaymentCreditOperationEntity.builder()
                .operationId(operationId)
                .transactionId(UUID.randomUUID())
                .cardId(CARD)
                .operationType(PaymentCreditOperationEntity.Type.CONSUME_PURCHASE)
                .amount(new BigDecimal("1500.00"))
                .currency("ARS")
                .status(Status.PENDING)
                .createdAt(created)
                .updatedAt(created)
                .build();
    }
}
