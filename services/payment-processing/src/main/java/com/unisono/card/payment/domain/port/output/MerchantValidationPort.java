package com.unisono.card.payment.domain.port.output;

import java.util.Optional;

/**
 * Port for validating merchant information
 */
public interface MerchantValidationPort {

    /**
     * Validate if merchant exists and is active
     */
    boolean isMerchantActive(String merchantId);

    /**
     * Validate merchant category code (MCC)
     */
    boolean isValidMCC(String mcc);

    /**
     * Merchant that owns an active terminal (ATM or POS), or empty if the terminal is unknown or inactive
     */
    Optional<String> findMerchantIdByTerminal(String terminalId);
}
