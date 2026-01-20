package com.unisono.card.merchant.domain.exception;

/**
 * Excepción lanzada cuando la información fiscal es inválida
 */
public class InvalidTaxInformationException extends MerchantDomainException {

    public InvalidTaxInformationException(String message) {
        super(message);
    }
}
