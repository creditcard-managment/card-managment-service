package com.unisono.card.payment.domain.port.output;

import com.unisono.card.payment.domain.model.MerchantInfo;

/**
 * Port for validating merchant information
 */
public interface MerchantValidationPort {

    /**
     * Validate if merchant exists and is active
     */
    boolean isMerchantActive(String merchantId);

    /**
     * Get merchant information
     */
    MerchantInfo getMerchantInfo(String merchantId);

    /**
     * Validate merchant category code (MCC)
     */
    boolean isValidMCC(String mcc);

    /**
     * Check if card is restricted for this merchant
     */
    boolean isCardRestrictedForMerchant(String cardId, String merchantId);
}
