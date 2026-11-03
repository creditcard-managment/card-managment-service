package com.unisono.card.payment.application.service;

import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.CardData;
import com.unisono.card.payment.domain.model.MerchantInfo;
import com.unisono.card.payment.domain.model.PaymentMethod;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.port.input.ReverseTransactionUseCase;
import com.unisono.card.payment.domain.port.output.TransactionRepository;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreAuthorizationExpiryServiceTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ReverseTransactionUseCase reverseTransactionUseCase;

    private PreAuthorizationExpiryService service;

    @BeforeEach
    void setUp() {
        service = new PreAuthorizationExpiryService(transactionRepository, reverseTransactionUseCase);
        ReflectionTestUtils.setField(service, "expirationHours", 24L);
    }

    @Test
    void shouldReverseEachOverduePreAuthorization() {
        Transaction first = preAuthorization();
        Transaction second = preAuthorization();
        when(transactionRepository.findPreAuthorizationsAuthorizedBefore(any())).thenReturn(List.of(first, second));

        int expired = service.expireOverdue();

        assertThat(expired).isEqualTo(2);
        verify(reverseTransactionUseCase).reverseTransaction(eq(first.getTransactionId()), contains("expired"));
        verify(reverseTransactionUseCase).reverseTransaction(eq(second.getTransactionId()), contains("expired"));
    }

    @Test
    void shouldUseTheConfiguredExpiration() {
        when(transactionRepository.findPreAuthorizationsAuthorizedBefore(any())).thenReturn(List.of());

        service.expireOverdue();

        ArgumentCaptor<LocalDateTime> threshold = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(transactionRepository).findPreAuthorizationsAuthorizedBefore(threshold.capture());
        Duration age = Duration.between(threshold.getValue(), LocalDateTime.now());
        assertThat(age.toHours()).isEqualTo(24);
        verifyNoInteractions(reverseTransactionUseCase);
    }

    @Test
    void shouldContinueWhenOneReversalFails() {
        Transaction failing = preAuthorization();
        Transaction ok = preAuthorization();
        when(transactionRepository.findPreAuthorizationsAuthorizedBefore(any())).thenReturn(List.of(failing, ok));
        when(reverseTransactionUseCase.reverseTransaction(eq(failing.getTransactionId()), any()))
                .thenThrow(new RuntimeException("card-limits down"));

        int expired = service.expireOverdue();

        assertThat(expired).isEqualTo(1);
        verify(reverseTransactionUseCase).reverseTransaction(eq(ok.getTransactionId()), any());
    }

    private static Transaction preAuthorization() {
        YearMonth expiration = YearMonth.now().plusYears(3);
        CardData cardData = CardData.of("tok_123", "0366", "Juan Pérez",
                ExpirationDate.of(expiration.getMonthValue(), expiration.getYear()));
        MerchantInfo merchant = MerchantInfo.of("6f1c2d3e-0000-4000-8000-000000000001",
                "Hotel Uno", "7011", "CABA", "AR");
        Transaction transaction = Transaction.createPurchase(CARD_ID, cardData, merchant,
                TransactionAmount.of(Money.ofARS(1500)), PaymentMethod.CHIP);
        transaction.authorize(AuthorizationCode.approved("AUTH01"), 10);
        return transaction;
    }
}
