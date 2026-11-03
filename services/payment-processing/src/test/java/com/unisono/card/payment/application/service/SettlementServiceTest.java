package com.unisono.card.payment.application.service;

import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.CardData;
import com.unisono.card.payment.domain.model.MerchantInfo;
import com.unisono.card.payment.domain.model.PaymentMethod;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.port.input.SettleTransactionsUseCase.BatchSettlementResult;
import com.unisono.card.payment.domain.port.output.PaymentEventPublisher;
import com.unisono.card.payment.domain.port.output.TransactionRepository;
import com.unisono.card.payment.domain.service.PaymentDomainService;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentEventPublisher eventPublisher;

    private SettlementService service;

    @BeforeEach
    void setUp() {
        // La liquidación no usa puertos externos: el crédito se consumió al aprobar
        PaymentDomainService domainService = new PaymentDomainService(null, null, null, null, null);
        service = new SettlementService(domainService, transactionRepository, eventPublisher);
    }

    @Test
    void shouldReportFailedTransactionsInBatch() {
        Transaction approved = purchase(true);
        Transaction pending = purchase(false);
        TransactionId missing = TransactionId.generate();
        when(transactionRepository.findById(approved.getTransactionId())).thenReturn(Optional.of(approved));
        when(transactionRepository.findById(pending.getTransactionId())).thenReturn(Optional.of(pending));
        when(transactionRepository.findById(missing)).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BatchSettlementResult result = service.settleBatch(
                List.of(approved.getTransactionId(), pending.getTransactionId(), missing));

        assertThat(result.settled()).containsExactly(approved);
        assertThat(approved.getStatus()).isEqualTo(TransactionStatus.SETTLED);
        assertThat(result.failed()).containsExactly(pending.getTransactionId(), missing);
    }

    @Test
    void shouldSettleAllPending() {
        Transaction approved = purchase(true);
        when(transactionRepository.findPendingSettlement()).thenReturn(List.of(approved));
        when(transactionRepository.findById(approved.getTransactionId())).thenReturn(Optional.of(approved));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BatchSettlementResult result = service.settleAllPending();

        assertThat(result.settled()).containsExactly(approved);
        assertThat(result.failed()).isEmpty();
    }

    private static Transaction purchase(boolean approved) {
        YearMonth expiration = YearMonth.now().plusYears(3);
        Transaction transaction = Transaction.createPurchase(CARD_ID,
                CardData.of("tok_123", "0366", "Juan Pérez", ExpirationDate.of(expiration.getMonthValue(), expiration.getYear())),
                MerchantInfo.of("6f1c2d3e-0000-4000-8000-000000000001", "Supermercado Uno", "5411", "CABA", "AR"),
                TransactionAmount.of(Money.ofARS(1500)), PaymentMethod.CHIP);
        if (approved) {
            transaction.authorize(AuthorizationCode.approved("AUTH01"), 10);
            transaction.approve();
        }
        return transaction;
    }
}
