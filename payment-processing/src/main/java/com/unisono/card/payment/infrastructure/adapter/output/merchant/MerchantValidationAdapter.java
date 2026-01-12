package com.unisono.card.payment.infrastructure.adapter.output.merchant;

import com.unisono.card.payment.domain.model.MerchantInfo;
import com.unisono.card.payment.domain.port.output.MerchantValidationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Adapter for validating merchant information
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantValidationAdapter implements MerchantValidationPort {

    private final RestTemplate restTemplate;

    @Value("${services.merchant.url:http://localhost:8080}")
    private String merchantServiceUrl;

    @Override
    public boolean isMerchantActive(String merchantId) {
        log.debug("Validating if merchant is active: {}", merchantId);

        try {
            String url = String.format("%s/api/v1/merchants/%s/status", merchantServiceUrl, merchantId);

            MerchantStatusResponse response = restTemplate.getForObject(url, MerchantStatusResponse.class);

            boolean isActive = response != null && "ACTIVE".equals(response.getStatus());
            log.debug("Merchant {} active status: {}", merchantId, isActive);

            return isActive;

        } catch (Exception ex) {
            log.error("Error checking merchant active status for {}: {}", merchantId, ex.getMessage(), ex);
            return false; // Fail-safe: assume inactive if can't verify
        }
    }

    @Override
    public MerchantInfo getMerchantInfo(String merchantId) {
        log.debug("Getting merchant information: {}", merchantId);

        try {
            String url = String.format("%s/api/v1/merchants/%s", merchantServiceUrl, merchantId);

            MerchantDetailsResponse response = restTemplate.getForObject(url, MerchantDetailsResponse.class);

            if (response != null) {
                MerchantInfo merchantInfo = MerchantInfo.of(
                        response.getMerchantId(),
                        response.getMerchantName(),
                        response.getMerchantCategoryCode(),
                        response.getCity(),
                        response.getCountry()
                );

                log.debug("Retrieved merchant info for {}: {}", merchantId, merchantInfo);
                return merchantInfo;
            }

            log.warn("No merchant information found for: {}", merchantId);
            return MerchantInfo.simple(merchantId, "Unknown Merchant");

        } catch (Exception ex) {
            log.error("Error getting merchant info for {}: {}", merchantId, ex.getMessage(), ex);
            return MerchantInfo.simple(merchantId, "Unknown Merchant");
        }
    }

    @Override
    public boolean isValidMCC(String mcc) {
        log.debug("Validating MCC: {}", mcc);

        try {
            // Basic MCC validation (4 digits)
            boolean isValid = mcc != null && mcc.matches("\\d{4}");

            // In a real implementation, validate against known MCC codes
            log.debug("MCC {} validation: {}", mcc, isValid);
            return isValid;

        } catch (Exception ex) {
            log.error("Error validating MCC {}: {}", mcc, ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean isCardRestrictedForMerchant(String cardId, String merchantId) {
        log.debug("Checking if card {} is restricted for merchant {}", cardId, merchantId);

        try {
            String url = String.format("%s/api/v1/merchants/%s/restrictions/%s",
                    merchantServiceUrl, merchantId, cardId);

            RestrictionResponse response = restTemplate.getForObject(url, RestrictionResponse.class);

            boolean isRestricted = response != null && response.isRestricted();
            log.debug("Card {} restriction for merchant {}: {}", cardId, merchantId, isRestricted);

            return isRestricted;

        } catch (Exception ex) {
            log.error("Error checking merchant restriction for card {} merchant {}: {}",
                    cardId, merchantId, ex.getMessage());
            return false; // Fail-safe: assume not restricted
        }
    }

    // ==================== Internal DTOs ====================

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class MerchantStatusResponse {
        private String status;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class MerchantDetailsResponse {
        private String merchantId;
        private String merchantName;
        private String merchantCategoryCode;
        private String city;
        private String country;
        private String status;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class RestrictionResponse {
        private boolean restricted;
        private String reason;
    }
}
