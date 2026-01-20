package com.unisono.card.merchant.domain.exception;

/**
 * Excepción lanzada cuando se intenta registrar una terminal que ya existe
 */
public class TerminalAlreadyExistsException extends MerchantDomainException {

    public TerminalAlreadyExistsException(String message) {
        super(message);
    }
}
