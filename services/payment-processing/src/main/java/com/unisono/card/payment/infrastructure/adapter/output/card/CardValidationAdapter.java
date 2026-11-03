package com.unisono.card.payment.infrastructure.adapter.output.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.unisono.card.payment.domain.exception.CardValidationException;
import com.unisono.card.payment.domain.exception.ExternalServiceException;
import com.unisono.card.payment.domain.port.output.CardValidationPort;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Adapter for validating card information via card-data service (GET /api/v1/cards/{cardId})
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardValidationAdapter implements CardValidationPort {

    private static final String SERVICE = "card-data";

    private final RestTemplate restTemplate;

    @Value("${services.card-data.url:http://localhost:8081}")
    private String cardDataServiceUrl;

    @Override
    public Optional<CardStatusSnapshot> findCardStatus(CardId cardId) {
        log.debug("Getting card status from card-data: {}", cardId);

        String url = String.format("%s/api/v1/cards/%s", cardDataServiceUrl, cardId.getValue());
        try {
            CardDetailsResponse response = restTemplate.getForObject(url, CardDetailsResponse.class);
            if (response == null) {
                throw new ExternalServiceException(SERVICE, "empty response for card " + cardId, null);
            }

            CardStatusSnapshot snapshot = new CardStatusSnapshot(
                    response.getStatus(),
                    Boolean.TRUE.equals(response.getIsBlocked()),
                    Boolean.TRUE.equals(response.getIsExpired())
            );
            log.debug("Card {} status: {}", cardId, snapshot);
            return Optional.of(snapshot);

        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Card not found in card-data: {}", cardId);
            return Optional.empty();
        } catch (RestClientException ex) {
            log.error("Error getting card status for {}: {}", cardId, ex.getMessage());
            throw new ExternalServiceException(SERVICE, ex.getMessage(), ex);
        }
    }

    @Override
    public boolean validatePin(CardId cardId, String pin) {
        return verify(cardId, "validate-pin", Map.of("pin", pin), "PIN");
    }

    @Override
    public boolean validateCvv(CardId cardId, String cvv) {
        return verify(cardId, "validate-cvv", Map.of("cvv", cvv), "CVV");
    }

    /**
     * POST /api/v1/cards/{cardId}/{operation} → { "valid": true | false }
     */
    private boolean verify(CardId cardId, String operation, Map<String, String> body, String what) {
        log.debug("Validating {} in card-data for card: {}", what, cardId);

        String url = String.format("%s/api/v1/cards/%s/%s", cardDataServiceUrl, cardId.getValue(), operation);
        try {
            VerificationResponse response = restTemplate.postForObject(url, body, VerificationResponse.class);
            if (response == null || response.getValid() == null) {
                throw new ExternalServiceException(SERVICE,
                        "empty " + what + " validation response for card " + cardId, null);
            }
            if (!response.getValid()) {
                log.warn("Incorrect {} for card: {}", what, cardId);
            }
            return response.getValid();

        } catch (HttpClientErrorException.NotFound ex) {
            throw CardValidationException.notFound(cardId);
        } catch (HttpClientErrorException.UnprocessableEntity ex) {
            // card-data rechaza la validación sobre una tarjeta bloqueada
            throw CardValidationException.blocked(cardId);
        } catch (RestClientException ex) {
            log.error("Error validating {} for {}: {}", what, cardId, ex.getMessage());
            throw new ExternalServiceException(SERVICE, ex.getMessage(), ex);
        }
    }

    // ==================== Internal DTOs ====================

    /**
     * Subset of card-data's CardDetailResponse
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CardDetailsResponse {
        private String id;
        private String status;
        private Boolean isBlocked;
        private Boolean isExpired;
    }

    /**
     * Response of card-data's POST /api/v1/cards/{cardId}/validate-pin and /validate-cvv
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class VerificationResponse {
        private Boolean valid;
    }
}
