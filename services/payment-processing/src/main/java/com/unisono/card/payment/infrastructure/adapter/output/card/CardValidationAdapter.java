package com.unisono.card.payment.infrastructure.adapter.output.card;

import com.unisono.card.payment.domain.port.output.CardValidationPort;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardNumber;
import com.unisono.card.shared.domain.valueobject.CVV;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

/**
 * Adapter for validating card information via card-data service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardValidationAdapter implements CardValidationPort {

    private final RestTemplate restTemplate;

    @Value("${services.card-data.url:http://localhost:8080}")
    private String cardDataServiceUrl;

    @Override
    public boolean isCardActive(CardId cardId) {
        log.debug("Validating if card is active: {}", cardId);

        try {
            String url = String.format("%s/api/v1/cards/%s/status", cardDataServiceUrl, cardId.getValue());

            CardStatusResponse response = restTemplate.getForObject(url, CardStatusResponse.class);

            boolean isActive = response != null && "ACTIVE".equals(response.getStatus());
            log.debug("Card {} active status: {}", cardId, isActive);

            return isActive;

        } catch (Exception ex) {
            log.error("Error checking card active status for {}: {}", cardId, ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public boolean validateCardNumber(CardNumber cardNumber) {
        log.debug("Validating card number with Luhn algorithm");

        try {
            // Luhn algorithm validation
            String number = cardNumber.getValue().replaceAll("\\s+", "");

            int sum = 0;
            boolean alternate = false;

            for (int i = number.length() - 1; i >= 0; i--) {
                int digit = Character.getNumericValue(number.charAt(i));

                if (alternate) {
                    digit *= 2;
                    if (digit > 9) {
                        digit = (digit % 10) + 1;
                    }
                }

                sum += digit;
                alternate = !alternate;
            }

            boolean isValid = (sum % 10 == 0);
            log.debug("Card number Luhn validation: {}", isValid);

            return isValid;

        } catch (Exception ex) {
            log.error("Error validating card number: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean validateCVV(CardId cardId, CVV cvv) {
        log.debug("Validating CVV for card: {}", cardId);

        try {
            // In a real implementation, this would call the card-data service
            // For now, we just validate the CVV format
            String cvvValue = cvv.getValue();
            boolean isValid = cvvValue != null && cvvValue.matches("\\d{3,4}");

            log.debug("CVV validation for card {}: {}", cardId, isValid);
            return isValid;

        } catch (Exception ex) {
            log.error("Error validating CVV for {}: {}", cardId, ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean isCardExpired(CardId cardId) {
        log.debug("Checking if card is expired: {}", cardId);

        try {
            String url = String.format("%s/api/v1/cards/%s", cardDataServiceUrl, cardId.getValue());

            CardDetailsResponse response = restTemplate.getForObject(url, CardDetailsResponse.class);

            boolean isExpired = response != null && response.isExpired();
            log.debug("Card {} expired status: {}", cardId, isExpired);

            return isExpired;

        } catch (Exception ex) {
            log.error("Error checking card expiration for {}: {}", cardId, ex.getMessage(), ex);
            return true; // Fail-safe: assume expired if can't verify
        }
    }

    @Override
    public boolean isCardBlocked(CardId cardId) {
        log.debug("Checking if card is blocked: {}", cardId);

        try {
            String url = String.format("%s/api/v1/cards/%s/status", cardDataServiceUrl, cardId.getValue());

            CardStatusResponse response = restTemplate.getForObject(url, CardStatusResponse.class);

            boolean isBlocked = response != null &&
                    ("BLOCKED".equals(response.getStatus()) || "SUSPENDED".equals(response.getStatus()));

            log.debug("Card {} blocked status: {}", cardId, isBlocked);
            return isBlocked;

        } catch (Exception ex) {
            log.error("Error checking card blocked status for {}: {}", cardId, ex.getMessage(), ex);
            return true; // Fail-safe: assume blocked if can't verify
        }
    }

    @Override
    public String getCardStatus(CardId cardId) {
        log.debug("Getting card status: {}", cardId);

        try {
            String url = String.format("%s/api/v1/cards/%s/status", cardDataServiceUrl, cardId.getValue());

            CardStatusResponse response = restTemplate.getForObject(url, CardStatusResponse.class);

            String status = response != null ? response.getStatus() : "UNKNOWN";
            log.debug("Card {} status: {}", cardId, status);

            return status;

        } catch (Exception ex) {
            log.error("Error getting card status for {}: {}", cardId, ex.getMessage(), ex);
            return "UNKNOWN";
        }
    }

    // ==================== Internal DTOs ====================

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class CardStatusResponse {
        private String status;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class CardDetailsResponse {
        private String cardId;
        private String status;
        private String expirationDate;

        public boolean isExpired() {
            // Parse expiration date and check
            // Simplified for now
            return false;
        }
    }
}
