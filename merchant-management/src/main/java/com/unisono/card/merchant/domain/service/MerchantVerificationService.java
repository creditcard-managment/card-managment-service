package com.unisono.card.merchant.domain.service;

import com.unisono.card.merchant.domain.exception.InvalidBusinessInformationException;
import com.unisono.card.merchant.domain.exception.InvalidTaxInformationException;
import com.unisono.card.merchant.domain.model.BusinessInformation;
import com.unisono.card.merchant.domain.model.TaxInformation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio de dominio para verificación de merchants
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantVerificationService {

    /**
     * Verifica la información fiscal del merchant
     * En producción, esto se integraría con AFIP u otros servicios gubernamentales
     */
    public void verifyTaxInformation(TaxInformation taxInformation) {
        log.info("Verifying tax information for Tax ID: {}", taxInformation.getTaxId());

        // Validar formato CUIT
        if (!isValidCuitFormat(taxInformation.getTaxId())) {
            throw new InvalidTaxInformationException("Invalid CUIT format");
        }

        // Aquí iría la integración con servicios externos
        // Por ejemplo: AFIP, para verificar que el CUIT existe y está activo
        // boolean isValid = afipService.verifyCuit(taxInformation.getTaxId());

        log.info("Tax information verified successfully");
    }

    /**
     * Verifica la información del negocio
     */
    public void verifyBusinessInformation(BusinessInformation businessInfo) {
        log.info("Verifying business information for: {}", businessInfo.getLegalName());

        // Validar número de registro
        if (!isValidRegistrationNumber(businessInfo.getRegistrationNumber())) {
            throw new InvalidBusinessInformationException("Invalid registration number");
        }

        // Aquí podrían agregarse más validaciones:
        // - Verificar en registros públicos
        // - Validar que el negocio esté activo
        // - Verificar sanciones o listas negras

        log.info("Business information verified successfully");
    }

    /**
     * Valida el formato del CUIT argentino
     */
    private boolean isValidCuitFormat(String cuit) {
        // Limpiar el CUIT de guiones
        String cleaned = cuit.replaceAll("[^0-9]", "");

        if (cleaned.length() != 11) {
            return false;
        }

        // Validar dígito verificador usando algoritmo módulo 11
        return validateCuitCheckDigit(cleaned);
    }

    /**
     * Valida el dígito verificador del CUIT usando algoritmo módulo 11
     */
    private boolean validateCuitCheckDigit(String cuit) {
        int[] multipliers = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int sum = 0;

        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cuit.charAt(i)) * multipliers[i];
        }

        int remainder = sum % 11;
        int checkDigit = 11 - remainder;

        if (checkDigit == 11) checkDigit = 0;
        if (checkDigit == 10) checkDigit = 9;

        return checkDigit == Character.getNumericValue(cuit.charAt(10));
    }

    /**
     * Valida el número de registro del negocio
     */
    private boolean isValidRegistrationNumber(String registrationNumber) {
        String cleaned = registrationNumber.replaceAll("[^0-9]", "");
        return !cleaned.isEmpty() && cleaned.length() >= 8;
    }
}
