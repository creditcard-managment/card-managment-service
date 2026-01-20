package com.unisono.card.merchant.domain.service;

import com.unisono.card.merchant.domain.exception.InvalidBusinessInformationException;
import com.unisono.card.merchant.domain.exception.InvalidTaxInformationException;
import com.unisono.card.merchant.domain.model.*;
import com.unisono.card.merchant.domain.model.contract.MerchantContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Servicio de dominio para registro de merchants
 * Solo contiene lógica de negocio pura, sin dependencias de persistencia
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantRegistrationService {

    private final MerchantVerificationService verificationService;

    /**
     * Registra un nuevo merchant
     * NOTA: Las validaciones de unicidad se manejan en la capa de aplicación
     */
    public Merchant registerMerchant(
            MerchantProfile profile,
            BusinessInformation businessInformation,
            TaxInformation taxInformation,
            MerchantCategory category,
            MerchantType type,
            MerchantContract contract
    ) {
        Objects.requireNonNull(profile, "Profile cannot be null");
        Objects.requireNonNull(businessInformation, "Business information cannot be null");
        Objects.requireNonNull(taxInformation, "Tax information cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");

        // Verificar información fiscal
        verificationService.verifyTaxInformation(taxInformation);

        // Verificar información del negocio
        verificationService.verifyBusinessInformation(businessInformation);

        // Validar requisitos por categoría
        validateCategoryRequirements(category, businessInformation);

        // Crear el merchant
        Merchant merchant = Merchant.builder()
                .id(MerchantId.generate())
                .profile(profile)
                .businessInformation(businessInformation)
                .taxInformation(taxInformation)
                .category(category)
                .type(type)
                .contract(contract)
                .build();

        log.info("Merchant created: {}", merchant.getId());

        return merchant;
    }

    /**
     * Actualiza la información de un merchant
     */
    public void updateMerchantInformation(
            Merchant merchant,
            MerchantProfile newProfile,
            BusinessInformation newBusinessInformation
    ) {
        Objects.requireNonNull(merchant, "Merchant cannot be null");
        Objects.requireNonNull(newProfile, "Profile cannot be null");
        Objects.requireNonNull(newBusinessInformation, "Business information cannot be null");

        // Verificar la nueva información del negocio
        verificationService.verifyBusinessInformation(newBusinessInformation);

        // Actualizar el merchant
        merchant.updateProfile(newProfile);

        log.info("Merchant information updated: {}", merchant.getId());
    }

    /**
     * Valida requisitos específicos por categoría
     */
    private void validateCategoryRequirements(
            MerchantCategory category,
            BusinessInformation businessInformation
    ) {
        // Categorías de alto riesgo requieren más años en el negocio
        if (category.isHighRisk()) {
            if (businessInformation.getYearsInBusiness() == null ||
                    businessInformation.getYearsInBusiness() < 2) {
                throw new InvalidBusinessInformationException(
                        "High-risk category requires at least 2 years in business"
                );
            }
        }

        // Algunas categorías requieren aprobación especial
        if (category.requiresSpecialApproval()) {
            log.warn("Category {} requires special approval", category);
        }
    }
}
