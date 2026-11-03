package com.unisono.card.payment.infrastructure.adapter.output.card;

import com.unisono.card.payment.domain.exception.ExternalServiceException;
import com.unisono.card.payment.domain.port.output.CardValidationPort.CardStatusSnapshot;
import com.unisono.card.shared.domain.valueobject.CardId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CardValidationAdapterTest {

    private static final String BASE = "http://card-data";
    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");
    private static final String CARD_URL = BASE + "/api/v1/cards/" + CARD_ID.getValue();

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
}
