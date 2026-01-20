package com.unisono.card.merchant.domain.exception;

/**
 * Excepción base para todas las excepciones de dominio de Merchant
 */
public abstract class MerchantDomainException extends RuntimeException {

    protected MerchantDomainException(String message) {
        super(message);
    }

    protected MerchantDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
