package com.unisono.card.payment.domain.exception;

/**
 * Exception thrown when a dependency (card-data, card-limits) cannot be reached
 * or answers with an unexpected error. The payment is not processed.
 */
public class ExternalServiceException extends PaymentException {

    private final String service;

    public ExternalServiceException(String service, String message, Throwable cause) {
        super(String.format("%s unavailable: %s", service, message), cause);
        this.service = service;
    }

    public String getService() {
        return service;
    }
}
