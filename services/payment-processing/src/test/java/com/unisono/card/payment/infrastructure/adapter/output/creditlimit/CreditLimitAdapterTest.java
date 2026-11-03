package com.unisono.card.payment.infrastructure.adapter.output.creditlimit;

import com.unisono.card.payment.domain.exception.ExternalServiceException;
import com.unisono.card.payment.domain.exception.InsufficientFundsException;
import com.unisono.card.payment.domain.model.TransactionType;
import com.unisono.card.payment.domain.port.output.CreditLimitPort.CreditCheck;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CreditLimitAdapterTest {

    private static final java.time.LocalDate ORIGINAL_DATE = java.time.LocalDate.of(2026, 11, 2);

    private static final String BASE = "http://card-limits";
    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");
    private static final String LIMIT_ID = "11111111-2222-4333-8444-555555555555";
    private static final Money AMOUNT = Money.ofARS(1500);

    private MockRestServiceServer server;
    private CreditLimitAdapter adapter;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        adapter = new CreditLimitAdapter(restTemplate);
        ReflectionTestUtils.setField(adapter, "cardLimitsServiceUrl", BASE);
    }

    @Test
    void shouldValidatePurchaseAgainstCardLimits() {
        server.expect(requestTo(BASE + "/api/v1/credit-limits/validate/purchase"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.cardId").value(CARD_ID.getValue()))
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.currency").value("ARS"))
                .andRespond(withSuccess("""
                        {"approved":true,"reason":null,"availableCredit":98500.00,"requestedAmount":1500.00,"currency":"ARS"}
                        """, MediaType.APPLICATION_JSON));

        CreditCheck check = adapter.checkCredit(CARD_ID, AMOUNT, TransactionType.PURCHASE);

        assertThat(check.approved()).isTrue();
        assertThat(check.available()).isEqualTo(Money.ofARS(98500));
        server.verify();
    }

    @Test
    void shouldValidateCashAdvanceWithItsOwnEndpoint() {
        server.expect(requestTo(BASE + "/api/v1/credit-limits/validate/cash-advance"))
                .andRespond(withSuccess("""
                        {"approved":false,"reason":"Se excede el límite de adelantos","availableCredit":500.00,"currency":"ARS"}
                        """, MediaType.APPLICATION_JSON));

        CreditCheck check = adapter.checkCredit(CARD_ID, AMOUNT, TransactionType.CASH_ADVANCE);

        assertThat(check.approved()).isFalse();
        assertThat(check.reason()).isEqualTo("Se excede el límite de adelantos");
        server.verify();
    }

    @Test
    void shouldRejectCardWithoutCreditLimit() {
        server.expect(requestTo(BASE + "/api/v1/credit-limits/validate/purchase"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        CreditCheck check = adapter.checkCredit(CARD_ID, AMOUNT, TransactionType.PURCHASE);

        assertThat(check.approved()).isFalse();
        assertThat(check.available()).isEqualTo(Money.zero("ARS"));
    }

    @Test
    void shouldFailWhenCardLimitsIsDown() {
        server.expect(requestTo(BASE + "/api/v1/credit-limits/validate/purchase"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> adapter.checkCredit(CARD_ID, AMOUNT, TransactionType.PURCHASE))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void shouldConsumePurchaseOnResolvedCreditLimit() {
        expectCreditLimitLookup();
        server.expect(requestTo(BASE + "/api/v1/credit-limits/" + LIMIT_ID + "/consume-purchase"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.description").value("Compra X"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        adapter.consumeCredit(CARD_ID, AMOUNT, TransactionType.PURCHASE, "Compra X");

        server.verify();
    }

    @Test
    void shouldConsumeCashAdvanceOnResolvedCreditLimit() {
        expectCreditLimitLookup();
        server.expect(requestTo(BASE + "/api/v1/credit-limits/" + LIMIT_ID + "/consume-cash-advance"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        adapter.consumeCredit(CARD_ID, AMOUNT, TransactionType.CASH_ADVANCE, "Adelanto");

        server.verify();
    }

    @Test
    void shouldTranslateRejectedConsumptionToInsufficientFunds() {
        expectCreditLimitLookup();
        server.expect(requestTo(BASE + "/api/v1/credit-limits/" + LIMIT_ID + "/consume-purchase"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> adapter.consumeCredit(CARD_ID, AMOUNT, TransactionType.PURCHASE, "Compra"))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void shouldRestoreCreditWithOriginalTransactionDate() {
        expectCreditLimitLookup();
        server.expect(requestTo(BASE + "/api/v1/credit-limits/" + LIMIT_ID + "/restore-credit"))
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.currency").value("ARS"))
                .andExpect(jsonPath("$.originalTransactionDate").value("2026-11-02"))
                .andExpect(jsonPath("$.description").value("Reversal"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        adapter.restoreCredit(CARD_ID, AMOUNT, ORIGINAL_DATE, "Reversal");

        server.verify();
    }

    @Test
    void shouldTruncateLongDescriptions() {
        expectCreditLimitLookup();
        server.expect(requestTo(BASE + "/api/v1/credit-limits/" + LIMIT_ID + "/restore-credit"))
                .andExpect(jsonPath("$.description").value("x".repeat(255)))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        adapter.restoreCredit(CARD_ID, AMOUNT, ORIGINAL_DATE, "x".repeat(300));

        server.verify();
    }

    @Test
    void shouldFailWhenCreditLimitCannotBeResolved() {
        server.expect(requestTo(BASE + "/api/v1/credit-limits/card/" + CARD_ID.getValue()))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adapter.restoreCredit(CARD_ID, AMOUNT, ORIGINAL_DATE, "Reversal"))
                .isInstanceOf(ExternalServiceException.class);
    }

    private void expectCreditLimitLookup() {
        server.expect(requestTo(BASE + "/api/v1/credit-limits/card/" + CARD_ID.getValue()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"%s","cardId":"%s","availableCredit":100000.00,"currency":"ARS"}
                        """.formatted(LIMIT_ID, CARD_ID.getValue()), MediaType.APPLICATION_JSON));
    }
}
