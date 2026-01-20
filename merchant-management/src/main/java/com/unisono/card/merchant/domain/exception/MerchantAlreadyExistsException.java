package com.unisono.card.merchant.domain.exception;

/**
 * Excepción lanzada cuando se intenta registrar un merchant que ya existe
 */
public class MerchantAlreadyExistsException extends MerchantDomainException {

    public MerchantAlreadyExistsException(String message) {
        super(message);
    }
}
