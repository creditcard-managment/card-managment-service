package com.unisono.card.payment.infrastructure.adapter.output.authorization;

import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.CardData;
import com.unisono.card.payment.domain.model.MerchantInfo;
import com.unisono.card.payment.domain.model.PaymentMethod;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simulated authorization (authorization.simulate=true)
 */
class AuthorizationAdapterTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");

    private AuthorizationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AuthorizationAdapter(new RestTemplate());
        ReflectionTestUtils.setField(adapter, "simulateAuthorization", true);
    }

    @Test
    void shouldApproveTheMaximumAmountAPaymentAccepts() {
        AuthorizationCode code = adapter.requestAuthorization(purchase("50000.00"));

        assertThat(code.isApproved()).isTrue();
    }

    @Test
    void shouldDeclineAmountsAboveTheConfiguredThreshold() {
        ReflectionTestUtils.setField(adapter, "simulateDeclineAbove", new BigDecimal("1000.00"));

        assertThat(adapter.requestAuthorization(purchase("1000.00")).isApproved()).isTrue();

        AuthorizationCode declined = adapter.requestAuthorization(purchase("1000.01"));
        assertThat(declined.isApproved()).isFalse();
        assertThat(declined.getResponseCode()).isEqualTo("61");
    }

    private static Transaction purchase(String amount) {
        YearMonth expiration = YearMonth.now().plusYears(3);
        CardData cardData = CardData.of("tok_123", "0366", "Juan Pérez",
                ExpirationDate.of(expiration.getMonthValue(), expiration.getYear()));
        MerchantInfo merchant = MerchantInfo.of("6f1c2d3e-0000-4000-8000-000000000001",
                "Supermercado Uno", "5411", "CABA", "AR");
        return Transaction.createPurchase(CARD_ID, cardData, merchant,
                TransactionAmount.of(Money.of(new BigDecimal(amount), "ARS")), PaymentMethod.CHIP);
    }
}
