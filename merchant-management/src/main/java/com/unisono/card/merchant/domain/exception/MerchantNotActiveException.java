package com.unisono.card.merchant.domain.exception;

/**
 * Excepción lanzada cuando se intenta operar con un merchant inactivo
 */
public class MerchantNotActiveException extends MerchantDomainException {

    public MerchantNotActiveException(String message) {
        super(message);
    }
}
