package com.unisono.card.lifecycle.infrastructure.adapter.output.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.unisono.card.lifecycle.domain.exception.CardDataIntegrationException;
import com.unisono.card.lifecycle.domain.port.output.CardDataClient;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.exception.ValidationException;
import com.unisono.card.shared.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Adapter para comunicarse con card-data vía REST.
 * Los errores HTTP se traducen a excepciones de dominio (ver {@link CardDataClient}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardDataRestClient implements CardDataClient {

    private final RestTemplate restTemplate;

    @Value("${card-data.base-url:http://localhost:8081}")
    private String cardDataBaseUrl;

    @Override
    public CardStatus getStatus(CardId cardId) {
        String url = cardUrl(cardId);

        CardStatusView card = call("consultar tarjeta", cardId,
                () -> restTemplate.getForObject(url, CardStatusView.class));

        if (card == null || card.status() == null) {
            throw new CardDataIntegrationException(
                    "card-data no informó el estado de la tarjeta " + cardId.getValue(), null);
        }
        return card.status();
    }

    @Override
    public void updateStatus(CardId cardId, String status, String reason, String performedBy) {
        Map<String, String> request = transitionBody(reason, performedBy);
        request.put("status", status);

        log.info("Actualizando estado de tarjeta vía REST: {} -> {}", cardId.getValue(), status);

        put(cardUrl(cardId) + "/status", request, "actualizar estado", cardId);
    }

    @Override
    public void block(CardId cardId, String reason, String performedBy) {
        log.info("Bloqueando tarjeta vía REST: {}", cardId.getValue());
        post(cardUrl(cardId) + "/block", transitionBody(reason, performedBy), "bloquear", cardId);
    }

    @Override
    public void blockForFraud(CardId cardId, String reason, String performedBy) {
        log.warn("Bloqueando tarjeta por fraude vía REST: {}", cardId.getValue());
        post(cardUrl(cardId) + "/block-fraud", transitionBody(reason, performedBy), "bloquear por fraude", cardId);
    }

    @Override
    public void cancel(CardId cardId, String reason, String performedBy) {
        log.info("Cancelando tarjeta vía REST: {}", cardId.getValue());
        post(cardUrl(cardId) + "/cancel", transitionBody(reason, performedBy), "cancelar", cardId);
    }

    @Override
    public void updateActivatedAt(CardId cardId, LocalDateTime activatedAt) {
        Map<String, String> request = new HashMap<>();
        request.put("activatedAt", activatedAt.toString());

        log.info("Actualizando fecha de activación vía REST: {}", cardId.getValue());

        put(cardUrl(cardId) + "/activated-at", request, "registrar fecha de activación", cardId);
    }

    @Override
    public void updateCardData(CardId cardId, CardNumber cardNumber, CVV cvv, PIN pin, ExpirationDate expirationDate) {
        Map<String, Object> request = new HashMap<>();
        request.put("cardNumber", cardNumber.getValue());
        request.put("cvv", cvv.getValue());
        request.put("pin", pin.getValue());
        request.put("expirationMonth", expirationDate.getMonth());
        request.put("expirationYear", expirationDate.getYear());

        log.info("Actualizando datos de tarjeta vía REST: {}", cardId.getValue());

        put(cardUrl(cardId) + "/card-data", request, "cargar datos de emisión", cardId);
    }

    @Override
    public boolean validatePin(CardId cardId, PIN pin) {
        String url = cardUrl(cardId) + "/validate-pin";

        log.debug("Validando PIN vía REST: {}", cardId.getValue());

        PinValidationView result = call("validar PIN", cardId,
                () -> restTemplate.postForObject(url, jsonEntity(Map.of("pin", pin.getValue())), PinValidationView.class));

        return result != null && Boolean.TRUE.equals(result.valid());
    }

    // =============================================
    // HELPERS
    // =============================================

    private static Map<String, String> transitionBody(String reason, String performedBy) {
        Map<String, String> body = new HashMap<>();
        body.put("reason", reason);
        body.put("performedBy", performedBy);
        return body;
    }

    private String cardUrl(CardId cardId) {
        return cardDataBaseUrl + "/api/v1/cards/" + cardId.getValue();
    }

    private void put(String url, Object body, String operation, CardId cardId) {
        call(operation, cardId, () -> {
            restTemplate.put(url, jsonEntity(body));
            return null;
        });
    }

    /** POST cuyo cuerpo de respuesta no interesa. */
    private void post(String url, Object body, String operation, CardId cardId) {
        call(operation, cardId, () -> {
            restTemplate.postForLocation(url, jsonEntity(body));
            return null;
        });
    }

    private static <T> HttpEntity<T> jsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private <T> T call(String operation, CardId cardId, Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpStatusCodeException e) {
            throw translate(e, operation, cardId);
        } catch (RestClientException e) {
            log.error("card-data no disponible al {} ({}): {}", operation, cardId.getValue(), e.getMessage());
            throw new CardDataIntegrationException(
                    String.format("card-data no disponible al %s la tarjeta %s", operation, cardId.getValue()), e);
        }
    }

    private RuntimeException translate(HttpStatusCodeException e, String operation, CardId cardId) {
        String message = remoteMessage(e);
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        log.warn("card-data respondió {} al {} ({}): {}", e.getStatusCode().value(), operation, cardId.getValue(), message);

        if (status == HttpStatus.NOT_FOUND) {
            return new NotFoundException(message);
        }
        if (status == HttpStatus.BAD_REQUEST) {
            return new ValidationException(message);
        }
        if (status == HttpStatus.CONFLICT || status == HttpStatus.UNPROCESSABLE_ENTITY) {
            return new BusinessRuleException(message);
        }
        return new CardDataIntegrationException(
                String.format("card-data respondió %d al %s la tarjeta %s: %s",
                        e.getStatusCode().value(), operation, cardId.getValue(), message), e);
    }

    /**
     * Extrae {@code message} del ErrorResponse de card-data; si no hay, usa el status.
     */
    private static String remoteMessage(HttpStatusCodeException e) {
        try {
            Map<?, ?> body = e.getResponseBodyAs(Map.class);
            if (body != null && body.get("message") != null) {
                return body.get("message").toString();
            }
        } catch (RuntimeException ignored) {
            // cuerpo vacío o no JSON
        }
        return e.getStatusText();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CardStatusView(CardStatus status) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PinValidationView(Boolean valid) {}
}
