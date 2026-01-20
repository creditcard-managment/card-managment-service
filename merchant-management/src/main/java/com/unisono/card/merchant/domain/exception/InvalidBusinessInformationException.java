package com.unisono.card.merchant.domain.exception;

/**
 * Excepción lanzada cuando la información del negocio es inválida
 */
public class InvalidBusinessInformationException extends MerchantDomainException {

    public InvalidBusinessInformationException(String message) {
        super(message);
    }
}
