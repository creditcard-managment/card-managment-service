package com.unisono.card.payment.infrastructure.adapter.output.creditlimit;


import com.unisono.card.payment.domain.port.output.CreditLimitPort;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * Adapter for managing credit limits via card-limits service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditLimitAdapter implements CreditLimitPort {

    private final RestTemplate restTemplate;

    @Value("${services.card-limits.url:http://localhost:8082}")
    private String cardLimitsServiceUrl;

    @Override
    public Money getAvailableCredit(CardId cardId) {
        log.debug("Getting available credit for card: {}", cardId);

        try {
            String url = String.format("%s/api/v1/credit-limits/%s/available",
                    cardLimitsServiceUrl, cardId.getValue());

            AvailableCreditResponse response = restTemplate.getForObject(url, AvailableCreditResponse.class);

            if (response != null) {
                Money availableCredit = Money.of(response.getAvailableAmount(), response.getCurrency());
                log.debug("Available credit for card {}: {}", cardId, availableCredit);
                return availableCredit;
            }

            log.warn("No credit limit information found for card: {}", cardId);
            return Money.of(BigDecimal.ZERO, "USD");

        } catch (Exception ex) {
            log.error("Error getting available credit for {}: {}", cardId, ex.getMessage(), ex);
            return Money.of(BigDecimal.ZERO, "USD");
        }
    }

    @Override
    public boolean hasSufficientFunds(CardId cardId, Money amount) {
        log.debug("Checking sufficient funds for card: {}, amount: {}", cardId, amount);

        try {
            Money availableCredit = getAvailableCredit(cardId);

            boolean hasFunds = availableCredit.getAmount().compareTo(amount.getAmount()) >= 0;
            log.debug("Card {} has sufficient funds for {}: {}", cardId, amount, hasFunds);

            return hasFunds;

        } catch (Exception ex) {
            log.error("Error checking sufficient funds for {}: {}", cardId, ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public void reserveCredit(CardId cardId, Money amount) {
        log.debug("Reserving credit for card: {}, amount: {}", cardId, amount);

        try {
            String url = String.format("%s/api/v1/credit-limits/%s/reserve",
                    cardLimitsServiceUrl, cardId.getValue());

            ReserveCreditRequest request = new ReserveCreditRequest(
                    amount.getAmount(),
                    amount.getCurrency().toString()
            );

            restTemplate.postForObject(url, request, Void.class);

            log.info("Successfully reserved credit for card {}: {}", cardId, amount);

        } catch (Exception ex) {
            log.error("Error reserving credit for {}: {}", cardId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to reserve credit", ex);
        }
    }

    @Override
    public void releaseCredit(CardId cardId, Money amount) {
        log.debug("Releasing credit for card: {}, amount: {}", cardId, amount);

        try {
            String url = String.format("%s/api/v1/credit-limits/%s/release",
                    cardLimitsServiceUrl, cardId.getValue());

            ReleaseCreditRequest request = new ReleaseCreditRequest(
                    amount.getAmount(),
                    amount.getCurrency().toString()
            );

            restTemplate.postForObject(url, request, Void.class);

            log.info("Successfully released credit for card {}: {}", cardId, amount);

        } catch (Exception ex) {
            log.error("Error releasing credit for {}: {}", cardId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to release credit", ex);
        }
    }

    @Override
    public void consumeCredit(CardId cardId, Money amount) {
        log.debug("Consuming credit for card: {}, amount: {}", cardId, amount);

        try {
            String url = String.format("%s/api/v1/credit-limits/%s/consume",
                    cardLimitsServiceUrl, cardId.getValue());

            ConsumeCreditRequest request = new ConsumeCreditRequest(
                    amount.getAmount(),
                    amount.getCurrency().toString()
            );

            restTemplate.postForObject(url, request, Void.class);

            log.info("Successfully consumed credit for card {}: {}", cardId, amount);

        } catch (Exception ex) {
            log.error("Error consuming credit for {}: {}", cardId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to consume credit", ex);
        }
    }

    @Override
    public void restoreCredit(CardId cardId, Money amount) {
        log.debug("Restoring credit for card: {}, amount: {}", cardId, amount);

        try {
            String url = String.format("%s/api/v1/credit-limits/%s/restore",
                    cardLimitsServiceUrl, cardId.getValue());

            RestoreCreditRequest request = new RestoreCreditRequest(
                    amount.getAmount(),
                    amount.getCurrency().toString()
            );

            restTemplate.postForObject(url, request, Void.class);

            log.info("Successfully restored credit for card {}: {}", cardId, amount);

        } catch (Exception ex) {
            log.error("Error restoring credit for {}: {}", cardId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to restore credit", ex);
        }
    }

    // ==================== Internal DTOs ====================

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class AvailableCreditResponse {
        private BigDecimal availableAmount;
        private String currency;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ReserveCreditRequest {
        private BigDecimal amount;
        private String currency;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ReleaseCreditRequest {
        private BigDecimal amount;
        private String currency;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ConsumeCreditRequest {
        private BigDecimal amount;
        private String currency;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class RestoreCreditRequest {
        private BigDecimal amount;
        private String currency;
    }
}
