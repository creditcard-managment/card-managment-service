package com.unisono.card.lifecycle.infrastructure.adapter.output.card;

import com.unisono.card.lifecycle.domain.port.output.CardDataClient;
import com.unisono.card.shared.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter para comunicarse con card-data vía REST
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardDataRestClient implements CardDataClient {

    private final RestTemplate restTemplate;

    @Value("${card-data.base-url:http://localhost:8081}")
    private String cardDataBaseUrl;

    @Override
    public void updateStatus(CardId cardId, String status) {
        String url = cardDataBaseUrl + "/api/v1/cards/" + cardId.getValue() + "/status";

        Map<String, String> request = new HashMap<>();
        request.put("status", status);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        log.info("Actualizando estado de tarjeta vía REST: {} -> {}", cardId.getValue(), status);

        restTemplate.put(url, entity);
    }

    @Override
    public void updateActivatedAt(CardId cardId, LocalDateTime activatedAt) {
        String url = cardDataBaseUrl + "/api/v1/cards/" + cardId.getValue() + "/activated-at";

        Map<String, String> request = new HashMap<>();
        request.put("activatedAt", activatedAt.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        log.info("Actualizando fecha de activación vía REST: {}", cardId.getValue());

        restTemplate.put(url, entity);
    }

    @Override
    public void updateCardData(CardId cardId, CardNumber cardNumber, CVV cvv, PIN pin, ExpirationDate expirationDate) {
        String url = cardDataBaseUrl + "/api/v1/cards/" + cardId.getValue() + "/card-data";

        Map<String, Object> request = new HashMap<>();
        request.put("cardNumber", cardNumber.getValue());
        request.put("cvv", cvv.getValue());
        request.put("pin", pin.getValue());
        request.put("expirationMonth", expirationDate.getMonth());
        request.put("expirationYear", expirationDate.getYear());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        log.info("Actualizando datos de tarjeta vía REST: {}", cardId.getValue());

        restTemplate.put(url, entity);
    }
}
