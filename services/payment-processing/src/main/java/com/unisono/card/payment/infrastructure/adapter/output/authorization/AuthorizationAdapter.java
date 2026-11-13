package com.unisono.card.payment.infrastructure.adapter.output.authorization;

import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.port.output.AuthorizationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Adapter for external authorization service (card network processor)
 * Simulates integration with Visa/Mastercard authorization networks
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationAdapter implements AuthorizationPort {

    private final RestTemplate restTemplate;

    @Value("${services.authorization.url:http://localhost:8087}")
    private String authorizationServiceUrl;

    @Value("${authorization.timeout-ms:3000}")
    private Integer authorizationTimeout;

    @Value("${authorization.simulate:true}")
    private Boolean simulateAuthorization;

    /**
     * The simulator declines (code 61) amounts strictly above this value. The default is the maximum
     * amount a payment accepts, so every valid amount is approved; lower it to test declines.
     */
    @Value("${authorization.simulate-decline-above:50000.00}")
    private BigDecimal simulateDeclineAbove = new BigDecimal("50000.00");

    @Override
    public AuthorizationCode requestAuthorization(Transaction transaction) {
        log.info("Requesting authorization for transaction: {}, amount: {}",
                transaction.getTransactionId(), transaction.getAmount().getEffectiveAmount());

        try {
            if (simulateAuthorization) {
                return simulateAuthorizationRequest(transaction);
            }

            String url = String.format("%s/api/v1/authorization/request", authorizationServiceUrl);

            AuthorizationRequest request = AuthorizationRequest.builder()
                    .transactionId(transaction.getTransactionId().toString())
                    .cardId(transaction.getCardId().getValue())
                    .cardNumber(transaction.getCardData().getMaskedCardNumber())
                    .expirationDate(transaction.getCardData().getExpirationDate().getYearMonth().toString())
                    .amount(transaction.getAmount().getEffectiveAmount().getAmount())
                    .currency(transaction.getAmount().getEffectiveAmount().getCurrency().getCurrencyCode())
                    .merchantId(transaction.getMerchantInfo().getMerchantId())
                    .merchantName(transaction.getMerchantInfo().getMerchantName())
                    .merchantCategoryCode(transaction.getMerchantInfo().getMerchantCategoryCode())
                    .merchantCountry(transaction.getMerchantInfo().getCountry())
                    .transactionType(transaction.getType().name())
                    .paymentMethod(transaction.getPaymentMethod().name())
                    .terminalId(transaction.getTerminalId())
                    .posEntryMode(transaction.getPosEntryMode())
                    .build();

            AuthorizationResponse response = restTemplate.postForObject(
                    url, request, AuthorizationResponse.class);

            if (response != null && response.isApproved()) {
                log.info("Authorization approved for transaction {}: code {}",
                        transaction.getTransactionId(), response.getAuthorizationCode());

                return AuthorizationCode.of(
                        response.getAuthorizationCode(),
                        LocalDateTime.now(),
                        response.getResponseCode()
                );
            } else {
                log.warn("Authorization declined for transaction {}: {}",
                        transaction.getTransactionId(),
                        response != null ? response.getDeclineReason() : "Unknown");

                String responseCode = response != null ? response.getResponseCode() : "05";
                return AuthorizationCode.declined(responseCode);
            }

        } catch (Exception ex) {
            log.error("Error requesting authorization for transaction {}: {}",
                    transaction.getTransactionId(), ex.getMessage(), ex);

            // Fallback to declined authorization
            return AuthorizationCode.declined("96"); // System error
        }
    }

    @Override
    public void captureAuthorization(Transaction transaction) {
        log.info("Capturing authorization for transaction: {}", transaction.getTransactionId());

        try {
            if (simulateAuthorization) {
                log.info("Simulated capture for transaction: {}", transaction.getTransactionId());
                return;
            }

            String url = String.format("%s/api/v1/authorization/capture", authorizationServiceUrl);

            CaptureRequest request = new CaptureRequest(
                    transaction.getTransactionId().toString(),
                    transaction.getAuthorizationCode().getCode(),
                    transaction.getAmount().getEffectiveAmount().getAmount(),
                    transaction.getAmount().getEffectiveAmount().getCurrency().getCurrencyCode()
            );

            restTemplate.postForObject(url, request, Void.class);

            log.info("Authorization captured successfully for transaction: {}",
                    transaction.getTransactionId());

        } catch (Exception ex) {
            log.error("Error capturing authorization for transaction {}: {}",
                    transaction.getTransactionId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to capture authorization", ex);
        }
    }

    @Override
    public void voidAuthorization(Transaction transaction) {
        log.info("Voiding authorization for transaction: {}", transaction.getTransactionId());

        try {
            if (simulateAuthorization) {
                log.info("Simulated void for transaction: {}", transaction.getTransactionId());
                return;
            }

            String url = String.format("%s/api/v1/authorization/void", authorizationServiceUrl);

            VoidRequest request = new VoidRequest(
                    transaction.getTransactionId().toString(),
                    transaction.getAuthorizationCode().getCode(),
                    "Transaction reversal"
            );

            restTemplate.postForObject(url, request, Void.class);

            log.info("Authorization voided successfully for transaction: {}",
                    transaction.getTransactionId());

        } catch (Exception ex) {
            log.error("Error voiding authorization for transaction {}: {}",
                    transaction.getTransactionId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to void authorization", ex);
        }
    }

    /**
     * Simulate authorization request (for testing/development)
     */
    private AuthorizationCode simulateAuthorizationRequest(Transaction transaction) {
        log.debug("Simulating authorization request for transaction: {}",
                transaction.getTransactionId());

        // Simulate processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate approval logic
        BigDecimal amount = transaction.getAmount().getEffectiveAmount().getAmount();

        // Decline if amount is above the configured threshold (for testing)
        if (amount.compareTo(simulateDeclineAbove) > 0) {
            log.info("Simulated authorization declined: amount {} above {}", amount, simulateDeclineAbove);
            return AuthorizationCode.declined("61"); // Exceeds withdrawal limit
        }

        // Approve otherwise
        String authCode = generateAuthorizationCode();
        log.info("Simulated authorization approved: {}", authCode);

        return AuthorizationCode.approved(authCode);
    }

    /**
     * Generate a random authorization code
     */
    private String generateAuthorizationCode() {
        return "AUTH-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // ==================== Internal DTOs ====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class AuthorizationRequest {
        private String transactionId;
        private String cardId;
        private String cardNumber; // Masked
        private String expirationDate;
        private BigDecimal amount;
        private String currency;
        private String merchantId;
        private String merchantName;
        private String merchantCategoryCode;
        private String merchantCountry;
        private String transactionType;
        private String paymentMethod;
        private String terminalId;
        private String posEntryMode;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class AuthorizationResponse {
        private boolean approved;
        private String authorizationCode;
        private String responseCode;
        private String declineReason;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class CaptureRequest {
        private String transactionId;
        private String authorizationCode;
        private BigDecimal amount;
        private String currency;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class VoidRequest {
        private String transactionId;
        private String authorizationCode;
        private String reason;
    }
}
