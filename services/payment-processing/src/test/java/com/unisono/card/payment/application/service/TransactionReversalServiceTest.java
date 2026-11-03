package com.unisono.card.payment.application.service;

import com.unisono.card.payment.domain.exception.InvalidTransactionException;
import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.CardData;
import com.unisono.card.payment.domain.model.MerchantInfo;
import com.unisono.card.payment.domain.model.PaymentMethod;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.port.output.PaymentEventPublisher;
import com.unisono.card.payment.domain.port.output.TransactionRepository;
import com.unisono.card.payment.domain.service.PaymentDomainService;
import com.unisono.card.payment.domain.service.TransactionValidationService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionReversalServiceTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private PaymentDomainService paymentDomainService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PaymentEventPublisher eventPublisher;

    private TransactionReversalService service;

    @BeforeEach
    void setUp() {
        service = new TransactionReversalService(paymentDomainService, new TransactionValidationService(),
                transactionRepository, eventPublisher);
    }

    @Test
    void shouldReverseLockingTheTransaction() {
        Transaction purchase = approvedPurchase();
        when(transactionRepository.findByIdForUpdate(purchase.getTransactionId())).thenReturn(Optional.of(purchase));
        when(transactionRepository.findRefundsOf(purchase.getTransactionId())).thenReturn(List.of());
        when(transactionRepository.save(purchase)).thenReturn(purchase);

        assertThat(service.reverseTransaction(purchase.getTransactionId(), "Cliente desistió")).isSameAs(purchase);
        verify(paymentDomainService).reverseTransaction(purchase, "Cliente desistió");
    }

    @Test
    void shouldNotReverseTransactionWithRefunds() {
        Transaction purchase = approvedPurchase();
        Transaction refund = Transaction.createRefund(CARD_ID, purchase.getCardData(), purchase.getMerchantInfo(),
                TransactionAmount.of(Money.ofARS(500)), purchase.getTransactionId());
        when(transactionRepository.findByIdForUpdate(purchase.getTransactionId())).thenReturn(Optional.of(purchase));
        when(transactionRepository.findRefundsOf(purchase.getTransactionId())).thenReturn(List.of(refund));

        assertThatThrownBy(() -> service.reverseTransaction(purchase.getTransactionId(), "Cliente desistió"))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("refunds");
        verify(paymentDomainService, never()).reverseTransaction(any(), anyString());
    }

    private static Transaction approvedPurchase() {
        YearMonth expiration = YearMonth.now().plusYears(3);
        CardData cardData = CardData.of("tok_123", "0366", "Juan Pérez",
                ExpirationDate.of(expiration.getMonthValue(), expiration.getYear()));
        MerchantInfo merchant = MerchantInfo.of("6f1c2d3e-0000-4000-8000-000000000001",
                "Supermercado Uno", "5411", "CABA", "AR");
        Transaction transaction = Transaction.createPurchase(CARD_ID, cardData, merchant,
                TransactionAmount.of(Money.ofARS(1500)), PaymentMethod.CHIP);
        transaction.authorize(AuthorizationCode.approved("AUTH01"), 10);
        transaction.approve();
        return transaction;
    }
}
