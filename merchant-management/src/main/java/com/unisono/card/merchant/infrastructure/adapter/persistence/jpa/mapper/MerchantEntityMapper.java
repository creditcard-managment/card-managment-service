package com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.mapper;

import com.unisono.card.merchant.domain.model.*;
import com.unisono.card.merchant.domain.model.contract.*;
import com.unisono.card.merchant.domain.model.fees.*;
import com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.entity.MerchantContractEntity;
import com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.entity.MerchantEntity;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * MapStruct Mapper para convertir entre MerchantEntity y Merchant
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@Component
public interface MerchantEntityMapper {

    // ==========================================
    // Entity -> Domain
    // ==========================================

    @Mapping(target = "id", expression = "java(mapMerchantId(entity.getId()))")
    @Mapping(target = "profile", expression = "java(mapProfile(entity))")
    @Mapping(target = "businessInformation", expression = "java(mapBusinessInformation(entity))")
    @Mapping(target = "taxInformation", expression = "java(mapTaxInformation(entity))")
    @Mapping(target = "contract", expression = "java(mapContract(entity.getContract()))")
    //@Mapping(target = "terminals", ignore = true)
    Merchant toDomain(MerchantEntity entity);

    // ==========================================
    // Domain -> Entity
    // ==========================================

    @Mapping(target = "id", expression = "java(merchant.getId().getValue())")
    @Mapping(target = "legalName", expression = "java(merchant.getBusinessInformation().getLegalName())")
    @Mapping(target = "tradeName", expression = "java(merchant.getBusinessInformation().getTradeName())")
    @Mapping(target = "registrationNumber", expression = "java(merchant.getBusinessInformation().getRegistrationNumber())")
    @Mapping(target = "businessDescription", expression = "java(merchant.getBusinessInformation().getBusinessDescription())")
    @Mapping(target = "yearsInBusiness", expression = "java(merchant.getBusinessInformation().getYearsInBusiness())")
    @Mapping(target = "taxId", expression = "java(merchant.getTaxInformation().getTaxId())")
    @Mapping(target = "taxCategory", expression = "java(merchant.getTaxInformation().getTaxCategory())")
    @Mapping(target = "vatResponsible", expression = "java(merchant.getTaxInformation().isVatResponsible())")
    @Mapping(target = "taxAddress", expression = "java(merchant.getTaxInformation().getTaxAddress())")
    @Mapping(target = "contactName", expression = "java(merchant.getProfile().getContactName())")
    @Mapping(target = "email", expression = "java(merchant.getProfile().getEmail())")
    @Mapping(target = "phone", expression = "java(merchant.getProfile().getPhone())")
    @Mapping(target = "website", expression = "java(merchant.getProfile().getWebsite())")
    @Mapping(target = "streetAddress", expression = "java(merchant.getProfile().getAddress().getStreetAddress())")
    @Mapping(target = "streetNumber", expression = "java(merchant.getProfile().getAddress().getStreetNumber())")
    @Mapping(target = "floor", expression = "java(merchant.getProfile().getAddress().getFloor())")
    @Mapping(target = "apartment", expression = "java(merchant.getProfile().getAddress().getApartment())")
    @Mapping(target = "city", expression = "java(merchant.getProfile().getAddress().getCity())")
    @Mapping(target = "state", expression = "java(merchant.getProfile().getAddress().getState())")
    @Mapping(target = "postalCode", expression = "java(merchant.getProfile().getAddress().getPostalCode())")
    @Mapping(target = "country", expression = "java(merchant.getProfile().getAddress().getCountry())")
    @Mapping(target = "latitude", expression = "java(merchant.getProfile().getAddress().getLatitude())")
    @Mapping(target = "longitude", expression = "java(merchant.getProfile().getAddress().getLongitude())")
    @Mapping(target = "contract", ignore = true)  // Se establece en @AfterMapping
    @Mapping(target = "terminals", ignore = true)
    MerchantEntity toEntity(Merchant merchant);

    // ==========================================
    // CRÍTICO: Post-mapping para relaciones
    // ==========================================

    @AfterMapping
    default void setRelationships(Merchant merchant, @MappingTarget MerchantEntity entity) {
        // Establecer contrato bidireccional CORRECTAMENTE
        if (merchant.getContract() != null) {
            MerchantContractEntity contractEntity = mapContractEntity(merchant.getContract());

            // ⚠️ IMPORTANTE: NO establecer merchant en contractEntity aquí
            // Dejar que el método setContract() de MerchantEntity lo haga
            entity.setContract(contractEntity);
        }
    }

    // ==========================================
    // Helper Methods - Entity to Domain
    // ==========================================

    default MerchantId mapMerchantId(String id) {
        return id != null ? MerchantId.of(id) : null;
    }

    default MerchantProfile mapProfile(MerchantEntity entity) {
        if (entity == null) return null;

        Address address = Address.fullAddress(
                entity.getStreetAddress(),
                entity.getStreetNumber(),
                entity.getFloor(),
                entity.getApartment(),
                entity.getCity(),
                entity.getState(),
                entity.getPostalCode(),
                entity.getCountry(),
                entity.getLatitude(),
                entity.getLongitude()
        );

        return MerchantProfile.of(
                entity.getContactName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getWebsite(),
                address
        );
    }

    default BusinessInformation mapBusinessInformation(MerchantEntity entity) {
        if (entity == null) return null;

        return BusinessInformation.of(
                entity.getLegalName(),
                entity.getTradeName(),
                entity.getRegistrationNumber(),
                entity.getBusinessDescription(),
                entity.getYearsInBusiness()
        );
    }

    default TaxInformation mapTaxInformation(MerchantEntity entity) {
        if (entity == null) return null;

        return TaxInformation.of(
                entity.getTaxId(),
                entity.getTaxCategory(),
                entity.isVatResponsible(),
                entity.getTaxAddress()
        );
    }

    default MerchantContract mapContract(MerchantContractEntity entity) {
        if (entity == null) return null;

        ContractTerms terms = ContractTerms.of(
                entity.getSettlementDays(),
                entity.getMinimumMonthlyVolume(),
                entity.getMaximumMonthlyVolume(),
                entity.getReservePercentage(),
                entity.getEarlyTerminationNoticeDays(),
                entity.getLegalJurisdiction(),
                entity.getDisputeResolutionMethod()
        );

        ChargebackPolicy chargebackPolicy = ChargebackPolicy.of(
                entity.getChargebackFee(),
                entity.getMaxChargebackRatio(),
                entity.getWarningThresholdCount(),
                entity.getWarningThresholdRatio(),
                entity.getReviewPeriodDays(),
                entity.isAutomaticSuspensionEnabled(),
                "Standard penalty structure"
        );

        ServiceLevel serviceLevel = ServiceLevel.of(
                entity.getServiceTier(),
                entity.getUptimeGuarantee(),
                entity.getSupportResponseTimeMinutes(),
                entity.getDisputeResolutionDays(),
                entity.isDedicatedAccountManager(),
                entity.isPrioritySupport(),
                entity.getMaxTransactionsPerDay(),
                entity.getMaxTransactionAmount()
        );

        MerchantFeeStructure feeStructure = buildDefaultFeeStructure();

        return MerchantContract.builder()
                .id(ContractId.of(entity.getId()))
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .terms(terms)
                .feeStructure(feeStructure)
                .chargebackPolicy(chargebackPolicy)
                .serviceLevel(serviceLevel)
                .autoRenew(entity.isAutoRenew())
                .renewalPeriodMonths(entity.getRenewalPeriodMonths())
                .build();
    }

    default MerchantFeeStructure buildDefaultFeeStructure() {
        TransactionFee standardFee = TransactionFee.combined(
                new BigDecimal("2.5"),
                new BigDecimal("0.30"),
                Set.of("VISA", "MASTERCARD"),
                Set.of("PURCHASE")
        );

        return MerchantFeeStructure.builder()
                .id(FeeStructureId.generate())
                .transactionFees(java.util.List.of(standardFee))
                .interchangeFee(InterchangeFee.standard("VISA"))
                .acquirerFee(AcquirerFee.standard())
                .monthlyFee(new BigDecimal("25.00"))
                .setupFee(new BigDecimal("100.00"))
                .chargebackFee(new BigDecimal("15.00"))
                .effectiveFrom(java.time.LocalDateTime.now())
                .build();
    }

    // ==========================================
    // Helper Methods - Domain to Entity
    // ==========================================

    // ⚠️ CRÍTICO: Este método NO debe recibir merchantId
    default MerchantContractEntity mapContractEntity(MerchantContract contract) {
        if (contract == null) return null;

        MerchantContractEntity entity = new MerchantContractEntity();
        entity.setId(contract.getId().getValue());
        entity.setStartDate(contract.getStartDate());
        entity.setEndDate(contract.getEndDate());
        entity.setStatus(contract.getStatus());
        entity.setAutoRenew(contract.isAutoRenew());
        entity.setRenewalPeriodMonths(contract.getRenewalPeriodMonths());

        // Contract Terms
        ContractTerms terms = contract.getTerms();
        entity.setSettlementDays(terms.getSettlementDays());
        entity.setMinimumMonthlyVolume(terms.getMinimumMonthlyVolume());
        entity.setMaximumMonthlyVolume(terms.getMaximumMonthlyVolume());
        entity.setReservePercentage(terms.getReservePercentage());
        entity.setEarlyTerminationNoticeDays(terms.getEarlyTerminationNoticeDays());
        entity.setLegalJurisdiction(terms.getLegalJurisdiction());
        entity.setDisputeResolutionMethod(terms.getDisputeResolutionMethod());

        // Chargeback Policy
        ChargebackPolicy policy = contract.getChargebackPolicy();
        entity.setChargebackFee(policy.getChargebackFee());
        entity.setMaxChargebackRatio(policy.getMaxChargebackRatio());
        entity.setWarningThresholdCount(policy.getWarningThresholdCount());
        entity.setWarningThresholdRatio(policy.getWarningThresholdRatio());
        entity.setReviewPeriodDays(policy.getReviewPeriodDays());
        entity.setAutomaticSuspensionEnabled(policy.isAutomaticSuspensionEnabled());

        // Service Level
        ServiceLevel serviceLevel = contract.getServiceLevel();
        entity.setServiceTier(serviceLevel.getTier());
        entity.setUptimeGuarantee(serviceLevel.getUptimeGuarantee());
        entity.setSupportResponseTimeMinutes(serviceLevel.getSupportResponseTimeMinutes());
        entity.setDisputeResolutionDays(serviceLevel.getDisputeResolutionDays());
        entity.setDedicatedAccountManager(serviceLevel.isDedicatedAccountManager());
        entity.setPrioritySupport(serviceLevel.isPrioritySupport());
        entity.setMaxTransactionsPerDay(serviceLevel.getMaxTransactionsPerDay());
        entity.setMaxTransactionAmount(serviceLevel.getMaxTransactionAmount());

        // ⚠️ IMPORTANTE: NO establecer merchant aquí
        // Dejar que MerchantEntity.setContract() lo haga

        return entity;
    }
}
