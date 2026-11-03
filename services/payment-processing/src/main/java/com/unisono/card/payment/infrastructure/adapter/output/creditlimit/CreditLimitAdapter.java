package com.unisono.card.payment.infrastructure.adapter.output.creditlimit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.unisono.card.payment.domain.exception.ExternalServiceException;
import com.unisono.card.payment.domain.exception.InsufficientFundsException;
import com.unisono.card.payment.domain.model.TransactionType;
import com.unisono.card.payment.domain.port.output.CreditLimitPort;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Adapter for managing credit limits via card-limits service
 *
 * - checkCredit: POST /api/v1/credit-limits/validate/purchase | cash-advance (disponible, diario y mensual)
 * - consumeCredit: POST /api/v1/credit-limits/{creditLimitId}/consume-purchase | consume-cash-advance
 * - restoreCredit: POST /api/v1/credit-limits/{creditLimitId}/restore-credit
 *
 * The creditLimitId is resolved with GET /api/v1/credit-limits/card/{cardId}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditLimitAdapter implements CreditLimitPort {

    private static final String SERVICE = "card-limits";
    private static final String BASE_PATH = "/api/v1/credit-limits";
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final RestTemplate restTemplate;

    @Value("${services.card-limits.url:http://localhost:8082}")
    private String cardLimitsServiceUrl;

    @Override
    public CreditCheck checkCredit(CardId cardId, Money amount, TransactionType type) {
        log.debug("Checking credit for card: {}, amount: {}, type: {}", cardId, amount, type);

        String operation = type == TransactionType.CASH_ADVANCE ? "cash-advance" : "purchase";
        String url = String.format("%s%s/validate/%s", cardLimitsServiceUrl, BASE_PATH, operation);
        try {
            ValidationResultResponse response = restTemplate.postForObject(url,
                    new ValidatePurchaseRequest(cardId.getValue(), amount.getAmount(), amount.getCurrencyCode()),
                    ValidationResultResponse.class);
            if (response == null) {
                throw new ExternalServiceException(SERVICE, "empty validation response for card " + cardId, null);
            }

            Money available = Money.of(
                    response.getAvailableCredit() != null ? response.getAvailableCredit() : BigDecimal.ZERO,
                    response.getCurrency() != null ? response.getCurrency() : amount.getCurrencyCode()
            );
            CreditCheck check = new CreditCheck(Boolean.TRUE.equals(response.getApproved()),
                    response.getReason(), available);
            log.debug("Credit check for card {}: {}", cardId, check);
            return check;

        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Card {} has no credit limit in card-limits", cardId);
            return new CreditCheck(false, "Card has no credit limit", Money.zero(amount.getCurrencyCode()));
        } catch (RestClientException ex) {
            log.error("Error checking credit for {}: {}", cardId, ex.getMessage());
            throw new ExternalServiceException(SERVICE, ex.getMessage(), ex);
        }
    }

    @Override
    public void consumeCredit(CardId cardId, Money amount, TransactionType type, String description) {
        log.debug("Consuming credit for card: {}, amount: {}, type: {}", cardId, amount, type);

        String creditLimitId = findCreditLimitId(cardId);
        String operation = type == TransactionType.CASH_ADVANCE ? "consume-cash-advance" : "consume-purchase";
        String url = String.format("%s%s/%s/%s", cardLimitsServiceUrl, BASE_PATH, creditLimitId, operation);
        try {
            restTemplate.postForObject(url,
                    new ConsumeCreditRequest(amount.getAmount(), amount.getCurrencyCode(), truncate(description)),
                    Void.class);
            log.info("Consumed credit for card {}: {}", cardId, amount);

        } catch (HttpClientErrorException ex) {
            // Otra operación consumió el disponible entre la validación y el consumo
            if (ex.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY || ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.warn("card-limits rejected consumption for {}: {}", cardId, ex.getResponseBodyAsString());
                throw new InsufficientFundsException(cardId, amount, Money.zero(amount.getCurrencyCode()),
                        "rejected by card-limits");
            }
            throw new ExternalServiceException(SERVICE, ex.getMessage(), ex);
        } catch (RestClientException ex) {
            log.error("Error consuming credit for {}: {}", cardId, ex.getMessage());
            throw new ExternalServiceException(SERVICE, ex.getMessage(), ex);
        }
    }

    @Override
    public void restoreCredit(CardId cardId, Money amount, LocalDate originalTransactionDate, String description) {
        log.debug("Restoring credit for card: {}, amount: {}, original date: {}", cardId, amount, originalTransactionDate);

        String creditLimitId = findCreditLimitId(cardId);
        String url = String.format("%s%s/%s/restore-credit", cardLimitsServiceUrl, BASE_PATH, creditLimitId);
        try {
            restTemplate.postForObject(url,
                    new RestoreCreditRequest(amount.getAmount(), amount.getCurrencyCode(),
                            originalTransactionDate.toString(), truncate(description)),
                    Void.class);
            log.info("Restored credit for card {}: {}", cardId, amount);

        } catch (RestClientException ex) {
            log.error("Error restoring credit for {}: {}", cardId, ex.getMessage());
            throw new ExternalServiceException(SERVICE, ex.getMessage(), ex);
        }
    }

    private String findCreditLimitId(CardId cardId) {
        String url = String.format("%s%s/card/%s", cardLimitsServiceUrl, BASE_PATH, cardId.getValue());
        try {
            CreditLimitResponse response = restTemplate.getForObject(url, CreditLimitResponse.class);
            if (response == null || response.getId() == null) {
                throw new ExternalServiceException(SERVICE, "empty credit limit for card " + cardId, null);
            }
            return response.getId();

        } catch (RestClientException ex) {
            log.error("Error getting credit limit for {}: {}", cardId, ex.getMessage());
            throw new ExternalServiceException(SERVICE, ex.getMessage(), ex);
        }
    }

    private static String truncate(String description) {
        return description.length() <= MAX_DESCRIPTION_LENGTH
                ? description
                : description.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    // ==================== Internal DTOs (contratos de card-limits) ====================

    @lombok.Data
    @lombok.NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ValidationResultResponse {
        private Boolean approved;
        private String reason;
        private BigDecimal availableCredit;
        private BigDecimal requestedAmount;
        private String currency;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CreditLimitResponse {
        private String id;
        private String cardId;
    }

    @lombok.Data
    @AllArgsConstructor
    static class ValidatePurchaseRequest {
        private String cardId;
        private BigDecimal amount;
        private String currency;
    }

    @lombok.Data
    @AllArgsConstructor
    static class ConsumeCreditRequest {
        private BigDecimal amount;
        private String currency;
        private String description;
    }

    @lombok.Data
    @AllArgsConstructor
    static class RestoreCreditRequest {
        private BigDecimal amount;
        private String currency;
        private String originalTransactionDate; // ISO yyyy-MM-dd
        private String description;
    }
}
