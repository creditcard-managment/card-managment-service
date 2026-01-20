package com.unisono.card.merchant.domain.exception;

import com.unisono.card.merchant.domain.model.MerchantId;

/**
 * Excepción lanzada cuando no se encuentra un merchant
 */
public class MerchantNotFoundException extends MerchantDomainException {

    public MerchantNotFoundException(MerchantId merchantId) {
        super("Merchant not found with ID: " + merchantId.getValue());
    }

    public MerchantNotFoundException(String taxId) {
        super("Merchant not found with Tax ID: " + taxId);
    }
}
