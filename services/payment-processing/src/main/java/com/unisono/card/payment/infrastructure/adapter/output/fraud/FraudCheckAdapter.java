package com.unisono.card.payment.infrastructure.adapter.output.fraud;

import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.port.output.FraudCheckPort;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Adapter for fraud detection and risk scoring
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudCheckAdapter implements FraudCheckPort {

    private final RestTemplate restTemplate;

    @Value("${services.fraud-detection.url:http://localhost:8081}")
    private String fraudDetectionServiceUrl;

    @Value("${fraud.risk-threshold.high:70}")
    private Integer highRiskThreshold;

    @Value("${fraud.risk-threshold.medium:40}")
    private Integer mediumRiskThreshold;

    @Value("${fraud.velocity.max-transactions-per-hour:10}")
    private Integer maxTransactionsPerHour;

    @Value("${fraud.amount.suspicious-threshold:5000.00}")
    private BigDecimal suspiciousAmountThreshold;

    @Override
    public Integer calculateRiskScore(Transaction transaction) {
        log.debug("Calculating risk score for transaction: {}", transaction.getTransactionId());

        try {
            String url = String.format("%s/api/v1/fraud/risk-score", fraudDetectionServiceUrl);

            RiskScoreRequest request = RiskScoreRequest.builder()
                    .transactionId(transaction.getTransactionId().toString())
                    .cardId(transaction.getCardId().getValue())
                    .amount(transaction.getAmount().getEffectiveAmount().getAmount())
                    .currency(transaction.getAmount().getEffectiveAmount().getCurrency().toString())
                    .merchantId(transaction.getMerchantInfo().getMerchantId())
                    .merchantCategoryCode(transaction.getMerchantInfo().getMerchantCategoryCode())
                    .merchantCountry(transaction.getMerchantInfo().getCountry())
                    .paymentMethod(transaction.getPaymentMethod().name())
                    .transactionDate(transaction.getTransactionDate())
                    .build();

            RiskScoreResponse response = restTemplate.postForObject(url, request, RiskScoreResponse.class);

            Integer riskScore = response != null ? response.getRiskScore() : calculateBasicRiskScore(transaction);

            log.info("Risk score calculated for transaction {}: {}",
                    transaction.getTransactionId(), riskScore);

            return riskScore;

        } catch (Exception ex) {
            log.error("Error calculating risk score for transaction {}, using fallback: {}",
                    transaction.getTransactionId(), ex.getMessage());

            // Fallback to basic risk calculation
            return calculateBasicRiskScore(transaction);
        }
    }

    @Override
    public boolean isSuspicious(Transaction transaction) {
        log.debug("Checking if transaction is suspicious: {}", transaction.getTransactionId());

        try {
            Integer riskScore = calculateRiskScore(transaction);

            boolean isSuspicious = riskScore >= highRiskThreshold;

            // Additional checks
            boolean hasHighAmount = transaction.getAmount().getEffectiveAmount()
                    .getAmount().compareTo(suspiciousAmountThreshold) >= 0;

            boolean isOnlineTransaction = transaction.getPaymentMethod() ==
                    com.unisono.card.payment.domain.model.PaymentMethod.ONLINE;

            boolean isSuspiciousCombination = hasHighAmount && isOnlineTransaction && riskScore >= mediumRiskThreshold;

            isSuspicious = isSuspicious || isSuspiciousCombination;

            log.debug("Transaction {} suspicious status: {} (risk score: {})",
                    transaction.getTransactionId(), isSuspicious, riskScore);

            return isSuspicious;

        } catch (Exception ex) {
            log.error("Error checking suspicious transaction {}: {}",
                    transaction.getTransactionId(), ex.getMessage(), ex);
            return false; // Fail-safe: don't block transaction on error
        }
    }

    @Override
    public boolean violatesVelocityRules(CardId cardId) {
        log.debug("Checking velocity rules for card: {}", cardId);

        try {
            String url = String.format("%s/api/v1/fraud/velocity-check/%s",
                    fraudDetectionServiceUrl, cardId.getValue());

            VelocityCheckResponse response = restTemplate.getForObject(url, VelocityCheckResponse.class);

            boolean violates = response != null && response.isViolation();

            if (violates) {
                log.warn("Velocity rule violation detected for card {}: {} transactions in last hour",
                        cardId, response.getTransactionCount());
            }

            return violates;

        } catch (Exception ex) {
            log.error("Error checking velocity rules for card {}: {}", cardId, ex.getMessage(), ex);
            return false; // Fail-safe: don't block on error
        }
    }

    @Override
    public boolean hasGeolocationAnomaly(CardId cardId, String merchantCountry) {
        log.debug("Checking geolocation anomaly for card: {}, merchant country: {}",
                cardId, merchantCountry);

        try {
            String url = String.format("%s/api/v1/fraud/geolocation-check", fraudDetectionServiceUrl);

            GeolocationCheckRequest request = new GeolocationCheckRequest(
                    cardId.getValue(),
                    merchantCountry
            );

            GeolocationCheckResponse response = restTemplate.postForObject(
                    url, request, GeolocationCheckResponse.class);

            boolean hasAnomaly = response != null && response.isAnomaly();

            if (hasAnomaly) {
                log.warn("Geolocation anomaly detected for card {}: transaction in {}, expected in {}",
                        cardId, merchantCountry, response.getExpectedCountry());
            }

            return hasAnomaly;

        } catch (Exception ex) {
            log.error("Error checking geolocation for card {}: {}", cardId, ex.getMessage(), ex);
            return false; // Fail-safe
        }
    }

    @Override
    public boolean hasAmountAnomaly(Transaction transaction) {
        log.debug("Checking amount anomaly for transaction: {}", transaction.getTransactionId());

        try {
            String url = String.format("%s/api/v1/fraud/amount-anomaly", fraudDetectionServiceUrl);

            AmountAnomalyRequest request = AmountAnomalyRequest.builder()
                    .cardId(transaction.getCardId().getValue())
                    .amount(transaction.getAmount().getEffectiveAmount().getAmount())
                    .currency(transaction.getAmount().getEffectiveAmount().getCurrency().toString())
                    .merchantCategoryCode(transaction.getMerchantInfo().getMerchantCategoryCode())
                    .build();

            AmountAnomalyResponse response = restTemplate.postForObject(
                    url, request, AmountAnomalyResponse.class);

            boolean hasAnomaly = response != null && response.isAnomaly();

            if (hasAnomaly) {
                log.warn("Amount anomaly detected for transaction {}: {} vs avg {}",
                        transaction.getTransactionId(),
                        transaction.getAmount().getEffectiveAmount().getAmount(),
                        response.getAverageAmount());
            }

            return hasAnomaly;

        } catch (Exception ex) {
            log.error("Error checking amount anomaly for transaction {}: {}",
                    transaction.getTransactionId(), ex.getMessage(), ex);
            return false; // Fail-safe
        }
    }

    @Override
    public void reportFraud(TransactionId transactionId, String reason) {
        log.warn("Reporting fraud for transaction: {}, reason: {}", transactionId, reason);

        try {
            String url = String.format("%s/api/v1/fraud/report", fraudDetectionServiceUrl);

            FraudReportRequest request = new FraudReportRequest(
                    transactionId.toString(),
                    reason,
                    LocalDateTime.now()
            );

            restTemplate.postForObject(url, request, Void.class);

            log.info("Fraud reported successfully for transaction: {}", transactionId);

        } catch (Exception ex) {
            log.error("Error reporting fraud for transaction {}: {}", transactionId, ex.getMessage(), ex);
        }
    }

    /**
     * Calculate basic risk score as fallback
     */
    private Integer calculateBasicRiskScore(Transaction transaction) {
        int score = 0;

        // Amount-based scoring
        BigDecimal amount = transaction.getAmount().getEffectiveAmount().getAmount();
        if (amount.compareTo(new BigDecimal("10000")) >= 0) {
            score += 30;
        } else if (amount.compareTo(new BigDecimal("5000")) >= 0) {
            score += 20;
        } else if (amount.compareTo(new BigDecimal("1000")) >= 0) {
            score += 10;
        }

        // Payment method scoring
        if (transaction.getPaymentMethod() == com.unisono.card.payment.domain.model.PaymentMethod.ONLINE) {
            score += 20;
        } else if (transaction.getPaymentMethod() == com.unisono.card.payment.domain.model.PaymentMethod.MANUAL) {
            score += 15;
        }

        // International transaction
        if (transaction.getMerchantInfo().getCountry() != null &&
                !transaction.getMerchantInfo().getCountry().equals("AR")) {
            score += 15;
        }

        // Currency conversion
        if (transaction.getAmount().hasCurrencyConversion()) {
            score += 10;
        }

        log.debug("Basic risk score calculated: {}", score);
        return Math.min(score, 100); // Cap at 100
    }

    // ==================== Internal DTOs ====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class RiskScoreRequest {
        private String transactionId;
        private String cardId;
        private BigDecimal amount;
        private String currency;
        private String merchantId;
        private String merchantCategoryCode;
        private String merchantCountry;
        private String paymentMethod;
        private LocalDateTime transactionDate;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class RiskScoreResponse {
        private Integer riskScore;
        private String riskLevel;
        private String[] flags;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class VelocityCheckResponse {
        private boolean violation;
        private Integer transactionCount;
        private Integer allowedCount;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class GeolocationCheckRequest {
        private String cardId;
        private String merchantCountry;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class GeolocationCheckResponse {
        private boolean anomaly;
        private String expectedCountry;
        private String actualCountry;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class AmountAnomalyRequest {
        private String cardId;
        private BigDecimal amount;
        private String currency;
        private String merchantCategoryCode;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class AmountAnomalyResponse {
        private boolean anomaly;
        private BigDecimal averageAmount;
        private BigDecimal standardDeviation;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class FraudReportRequest {
        private String transactionId;
        private String reason;
        private LocalDateTime reportedAt;
    }
}
