package com.unisono.card.payment.infrastructure.adapter.output.card;

import com.unisono.card.payment.domain.model.PinBlock;
import com.unisono.card.payment.domain.exception.CardValidationException;
import com.unisono.card.payment.domain.exception.ExternalServiceException;
import com.unisono.card.payment.domain.port.output.CardValidationPort.CardStatusSnapshot;
import com.unisono.card.shared.domain.valueobject.CardId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CardValidationAdapterTest {

    private static final String BASE = "http://card-data";
    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");
    private static final String CARD_URL = BASE + "/api/v1/cards/" + CARD_ID.getValue();
    private static final String PIN_URL = CARD_URL + "/validate-pin";
    private static final PinBlock PIN_BLOCK = new PinBlock("8E2C1F0A5B3D4E6F7A8B9C0D1E2F3A4B", "pek-v1");

    private MockRestServiceServer server;
    private CardValidationAdapter adapter;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        adapter = new CardValidationAdapter(restTemplate);
        ReflectionTestUtils.setField(adapter, "cardDataServiceUrl", BASE);
    }

    @Test
    void shouldMapCardDetailFromCardData() {
        server.expect(requestTo(CARD_URL)).andRespond(withSuccess("""
                {"id":"%s","status":"ACTIVE","isBlocked":false,"isExpired":false,"maskedCardNumber":"**** **** **** 0366"}
                """.formatted(CARD_ID.getValue()), MediaType.APPLICATION_JSON));

        Optional<CardStatusSnapshot> card = adapter.findCardStatus(CARD_ID);

        assertThat(card).contains(new CardStatusSnapshot("ACTIVE", false, false));
        assertThat(card.get().isActive()).isTrue();
    }

    @Test
    void shouldMapBlockedAndExpiredFlags() {
        server.expect(requestTo(CARD_URL)).andRespond(withSuccess("""
                {"status":"BLOCKED","isBlocked":true,"isExpired":true}
                """, MediaType.APPLICATION_JSON));

        CardStatusSnapshot card = adapter.findCardStatus(CARD_ID).orElseThrow();

        assertThat(card.blocked()).isTrue();
        assertThat(card.expired()).isTrue();
        assertThat(card.isActive()).isFalse();
    }

    @Test
    void shouldTreatMissingFlagsAsFalse() {
        server.expect(requestTo(CARD_URL)).andRespond(withSuccess("""
                {"status":"REQUESTED"}
                """, MediaType.APPLICATION_JSON));

        assertThat(adapter.findCardStatus(CARD_ID)).contains(new CardStatusSnapshot("REQUESTED", false, false));
    }

    @Test
    void shouldReturnEmptyWhenCardDoesNotExist() {
        server.expect(requestTo(CARD_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(adapter.findCardStatus(CARD_ID)).isEmpty();
    }

    @Test
    void shouldFailWhenCardDataIsDown() {
        server.expect(requestTo(CARD_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> adapter.findCardStatus(CARD_ID))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("card-data");
    }

    // ========== PIN ==========

    @Test
    void shouldPostPinToCardDataAndReturnResult() {
        server.expect(requestTo(PIN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.pinBlock").value(PIN_BLOCK.block()))
                .andExpect(jsonPath("$.pinKeyId").value("pek-v1"))
                .andExpect(jsonPath("$.pin").doesNotExist())
                .andRespond(withSuccess("""
                        {"valid":true}
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapter.validatePin(CARD_ID, PIN_BLOCK)).isTrue();
        server.verify();
    }

    @Test
    void shouldReturnFalseForIncorrectPin() {
        server.expect(requestTo(PIN_URL)).andRespond(withSuccess("""
                {"valid":false}
                """, MediaType.APPLICATION_JSON));

        assertThat(adapter.validatePin(CARD_ID, PIN_BLOCK)).isFalse();
    }

    @Test
    void shouldReportBlockedCardWhenCardDataRejectsPinValidation() {
        server.expect(requestTo(PIN_URL)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> adapter.validatePin(CARD_ID, PIN_BLOCK))
                .isInstanceOf(CardValidationException.class)
                .hasMessageContaining("blocked");
    }

    @Test
    void shouldFailPinValidationWhenCardDataIsDown() {
        server.expect(requestTo(PIN_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> adapter.validatePin(CARD_ID, PIN_BLOCK))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void shouldDeclineWithInvalidPinBlockWhenCardDataCannotDecryptIt() {
        server.expect(requestTo(PIN_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> adapter.validatePin(CARD_ID, PIN_BLOCK))
                .isInstanceOf(CardValidationException.class)
                .hasMessageContaining("Invalid PIN block");
    }

    // ========== CVV ==========

    @Test
    void shouldPostCvvToCardDataAndReturnResult() {
        server.expect(requestTo(CARD_URL + "/validate-cvv"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.cvv").value("123"))
                .andRespond(withSuccess("""
                        {"valid":false}
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapter.validateCvv(CARD_ID, "123")).isFalse();
        server.verify();
    }

    @Test
    void shouldReportBlockedCardWhenCardDataRejectsCvvValidation() {
        server.expect(requestTo(CARD_URL + "/validate-cvv")).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> adapter.validateCvv(CARD_ID, "123"))
                .isInstanceOf(CardValidationException.class)
                .hasMessageContaining("blocked");
    }
}
