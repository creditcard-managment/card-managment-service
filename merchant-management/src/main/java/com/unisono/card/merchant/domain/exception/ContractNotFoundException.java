package com.unisono.card.merchant.domain.exception;

import com.unisono.card.merchant.domain.model.contract.ContractId;

/**
 * Excepción lanzada cuando no se encuentra un contrato
 */
public class ContractNotFoundException extends MerchantDomainException {

    public ContractNotFoundException(ContractId contractId) {
        super("Contract not found with ID: " + contractId.getValue());
    }
}
