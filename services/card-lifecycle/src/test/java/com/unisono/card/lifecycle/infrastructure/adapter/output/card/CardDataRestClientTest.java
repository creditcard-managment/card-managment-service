package com.unisono.card.lifecycle.infrastructure.adapter.output.card;

import com.unisono.card.lifecycle.domain.exception.CardDataIntegrationException;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.domain.valueobject.PIN;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CardDataRestClientTest {

    private static final String BASE = "http://card-data";
    private static final CardId CARD_ID = CardId.of("be546553-f910-4891-8881-e83e1e7f488f");
    private static final String CARD_URL = BASE + "/api/v1/cards/" + CARD_ID.getValue();

    private MockRestServiceServer server;
    private CardDataRestClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new CardDataRestClient(restTemplate);
        ReflectionTestUtils.setField(client, "cardDataBaseUrl", BASE);
    }

    @Test
    void readsStatusIgnoringOtherFields() {
        server.expect(requestTo(CARD_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"x\",\"status\":\"ISSUED\",\"holderName\":\"Juan\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.getStatus(CARD_ID)).isEqualTo(CardStatus.ISSUED);
    }

    @Test
    void translates422ToBusinessRuleWithRemoteMessage() {
        server.expect(requestTo(CARD_URL + "/status")).andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":422,\"message\":\"Transición inválida: REQUESTED → ACTIVE\"}"));

        assertThatThrownBy(() -> client.updateStatus(CARD_ID, "ACTIVE"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Transición inválida: REQUESTED → ACTIVE");
    }

    @Test
    void translates404ToNotFound() {
        server.expect(requestTo(CARD_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getStatus(CARD_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void translates5xxToIntegrationError() {
        server.expect(requestTo(CARD_URL + "/activated-at")).andRespond(withServerError());

        assertThatThrownBy(() -> client.updateActivatedAt(CARD_ID, java.time.LocalDateTime.now()))
                .isInstanceOf(CardDataIntegrationException.class);
    }

    @Test
    void blocksAndCancelsWithReason() {
        server.expect(requestTo(CARD_URL + "/block")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.reason").value("Pérdida"))
                .andRespond(withSuccess("{\"status\":\"BLOCKED\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(CARD_URL + "/block-fraud")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());
        server.expect(requestTo(CARD_URL + "/cancel")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.reason").value("Baja"))
                .andRespond(withSuccess());

        client.block(CARD_ID, "Pérdida");
        client.blockForFraud(CARD_ID, "Fraude");
        client.cancel(CARD_ID, "Baja");

        server.verify();
    }

    @Test
    void validatesPin() {
        server.expect(requestTo(CARD_URL + "/validate-pin")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.pin").value("4821"))
                .andRespond(withSuccess("{\"valid\":false}", MediaType.APPLICATION_JSON));

        assertThat(client.validatePin(CARD_ID, PIN.of("4821"))).isFalse();
    }
}
