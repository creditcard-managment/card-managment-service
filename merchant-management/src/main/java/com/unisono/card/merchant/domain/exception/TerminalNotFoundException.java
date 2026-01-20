package com.unisono.card.merchant.domain.exception;

import com.unisono.card.merchant.domain.model.terminal.TerminalId;

/**
 * Excepción lanzada cuando no se encuentra una terminal
 */
public class TerminalNotFoundException extends MerchantDomainException {

    public TerminalNotFoundException(TerminalId terminalId) {
        super("Terminal not found with ID: " + terminalId.getValue());
    }

    public TerminalNotFoundException(String serialNumber) {
        super("Terminal not found with serial number: " + serialNumber);
    }
}
