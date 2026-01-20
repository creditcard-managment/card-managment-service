package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.request.RegisterMerchantRequest;
import com.unisono.card.merchant.application.dto.response.MerchantResponse;
import com.unisono.card.merchant.application.port.input.MerchantManagementUseCase;
import com.unisono.card.merchant.application.port.output.MerchantEventPublisherPort;
import com.unisono.card.merchant.application.port.output.MerchantPersistencePort;
import com.unisono.card.merchant.domain.event.MerchantRegistered;
import com.unisono.card.merchant.domain.exception.MerchantAlreadyExistsException;
import com.unisono.card.merchant.domain.model.*;
import com.unisono.card.merchant.domain.model.contract.*;
import com.unisono.card.merchant.domain.model.fees.*;
import com.unisono.card.merchant.domain.service.MerchantRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Caso de uso: Registrar un nuevo merchant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterMerchantUseCase implements MerchantManagementUseCase.Register {

    private final MerchantRegistrationService registrationService;
    private final MerchantPersistencePort persistencePort;
    private final MerchantEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public MerchantResponse execute(RegisterMerchantRequest request) {
        log.info("Registering new merchant: {}", request.getLegalName());

        // Validar unicidad de Tax ID
        if (persistencePort.existsByTaxId(request.getTaxId())) {
            throw new MerchantAlreadyExistsException(
                    "Merchant with tax ID " + request.getTaxId() + " already exists"
            );
        }

        // Validar unicidad de Email
        if (persistencePort.existsByEmail(request.getContactEmail())) {
            throw new MerchantAlreadyExistsException(
                    "Merchant with email " + request.getContactEmail() + " already exists"
            );
        }

        // Construir Value Objects del dominio
        MerchantProfile profile = buildProfile(request);
        BusinessInformation businessInfo = buildBusinessInformation(request);
        TaxInformation taxInfo = buildTaxInformation(request);
        MerchantContract contract = buildContract(request);

        // Registrar el merchant usando el servicio de dominio
        Merchant merchant = registrationService.registerMerchant(
                profile,
                businessInfo,
                taxInfo,
                request.getCategory(),
                request.getType(),
                contract
        );

        // Persistir
        Merchant savedMerchant = persistencePort.save(merchant);

        // Publicar evento de dominio
        MerchantRegistered event = MerchantRegistered.of(
                savedMerchant.getId(),
                savedMerchant.getBusinessInformation().getLegalName(),
                savedMerchant.getTaxInformation().getTaxId(),
                savedMerchant.getCategory(),
                savedMerchant.getType(),
                savedMerchant.getProfile().getEmail()
        );
        eventPublisher.publish(event);

        log.info("Merchant registered successfully with ID: {}", savedMerchant.getId());

        return MerchantResponse.from(savedMerchant);
    }

    private MerchantProfile buildProfile(RegisterMerchantRequest request) {
        Address address = Address.of(
                request.getStreetAddress(),
                request.getStreetNumber(),
                request.getCity(),
                request.getState(),
                request.getPostalCode(),
                request.getCountry()
        );

        return MerchantProfile.of(
                request.getContactName(),
                request.getContactEmail(),
                request.getContactPhone(),
                request.getWebsite(),
                address
        );
    }

    private BusinessInformation buildBusinessInformation(RegisterMerchantRequest request) {
        return BusinessInformation.of(
                request.getLegalName(),
                request.getTradeName(),
                request.getRegistrationNumber(),
                request.getBusinessDescription(),
                request.getYearsInBusiness()
        );
    }

    private TaxInformation buildTaxInformation(RegisterMerchantRequest request) {
        return TaxInformation.of(
                request.getTaxId(),
                request.getTaxCategory(),
                request.isVatResponsible(),
                request.getTaxAddress()
        );
    }

    private MerchantContract buildContract(RegisterMerchantRequest request) {
        // Crear estructura de fees estándar
        MerchantFeeStructure feeStructure = buildFeeStructure();

        // Crear términos del contrato
        ContractTerms terms = ContractTerms.standard();

        // Crear política de chargebacks
        ChargebackPolicy chargebackPolicy = ChargebackPolicy.standard();

        // Crear nivel de servicio
        ServiceLevel serviceLevel = ServiceLevel.basic();

        return MerchantContract.builder()
                .id(ContractId.generate())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .status(ContractStatus.ACTIVE)
                .terms(terms)
                .feeStructure(feeStructure)
                .chargebackPolicy(chargebackPolicy)
                .serviceLevel(serviceLevel)
                .autoRenew(true)
                .renewalPeriodMonths(12)
                .build();
    }

    private MerchantFeeStructure buildFeeStructure() {
        // Transaction fee estándar: 2.5% + $0.30
        TransactionFee standardFee = TransactionFee.combined(
                new BigDecimal("2.5"),
                new BigDecimal("0.30"),
                Set.of("VISA", "MASTERCARD"),
                Set.of("PURCHASE")
        );

        // Interchange fee
        InterchangeFee interchangeFee = InterchangeFee.standard("VISA");

        // Acquirer fee
        AcquirerFee acquirerFee = AcquirerFee.standard();

        return MerchantFeeStructure.builder()
                .id(FeeStructureId.generate())
                .transactionFees(java.util.List.of(standardFee))
                .interchangeFee(interchangeFee)
                .acquirerFee(acquirerFee)
                .monthlyFee(new BigDecimal("25.00"))
                .setupFee(new BigDecimal("100.00"))
                .chargebackFee(new BigDecimal("15.00"))
                .effectiveFrom(java.time.LocalDateTime.now())
                .build();
    }
}
