package com.unisono.card.payment.domain.exception;

/**
 * Exception thrown when merchant validation fails
 */
public class MerchantValidationException extends PaymentException {

    private final String merchantId;
    private final String validationType;

    public MerchantValidationException(String merchantId, String validationType, String message) {
        super(String.format("Merchant validation failed for %s - %s: %s", merchantId, validationType, message));
        this.merchantId = merchantId;
        this.validationType = validationType;
    }

    public static MerchantValidationException notFound(String merchantId) {
        return new MerchantValidationException(merchantId, "EXISTENCE", "Merchant not found");
    }

    public static MerchantValidationException unknownTerminal(String terminalId) {
        return new MerchantValidationException(terminalId, "TERMINAL", "Terminal not found or inactive");
    }

    public static MerchantValidationException inactive(String merchantId) {
        return new MerchantValidationException(merchantId, "STATUS", "Merchant is inactive");
    }

    public static MerchantValidationException suspended(String merchantId) {
        return new MerchantValidationException(merchantId, "STATUS", "Merchant is suspended");
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getValidationType() {
        return validationType;
    }
}
