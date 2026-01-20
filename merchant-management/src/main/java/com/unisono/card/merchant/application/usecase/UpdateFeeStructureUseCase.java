package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.request.UpdateFeeStructureRequest;
import com.unisono.card.merchant.application.dto.response.FeeStructureResponse;
import com.unisono.card.merchant.application.port.input.FeeManagementUseCase;
import com.unisono.card.merchant.application.port.output.MerchantEventPublisherPort;
import com.unisono.card.merchant.application.port.output.MerchantPersistencePort;
import com.unisono.card.merchant.domain.event.FeeStructureUpdated;
import com.unisono.card.merchant.domain.exception.MerchantNotFoundException;
import com.unisono.card.merchant.domain.model.Merchant;
import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.contract.MerchantContract;
import com.unisono.card.merchant.domain.model.fees.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Caso de uso: Actualizar estructura de fees de un merchant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateFeeStructureUseCase implements FeeManagementUseCase.UpdateFeeStructure {

    private final MerchantPersistencePort persistencePort;
    private final MerchantEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public FeeStructureResponse execute(String merchantId, UpdateFeeStructureRequest request) {
        log.info("Updating fee structure for merchant: {}", merchantId);

        MerchantId id = MerchantId.of(merchantId);

        Merchant merchant = persistencePort.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id));

        MerchantContract contract = merchant.getContract();
        if (contract == null) {
            throw new IllegalStateException("Merchant does not have a contract");
        }

        // Obtener estructura de fees actual
        MerchantFeeStructure oldFeeStructure = contract.getFeeStructure();
        FeeStructureId oldFeeStructureId = oldFeeStructure != null ? oldFeeStructure.getId() : null;
        var oldMonthlyFee = oldFeeStructure != null ? oldFeeStructure.getMonthlyFee() : null;

        // Crear nueva estructura de fees
        MerchantFeeStructure newFeeStructure = buildFeeStructure(request);

        // Actualizar el contrato con la nueva estructura (usando el nuevo método)
        contract.updateFeeStructure(newFeeStructure);

        // Guardar cambios
        Merchant savedMerchant = persistencePort.save(merchant);

        // Publicar evento
        FeeStructureUpdated event = FeeStructureUpdated.of(
                savedMerchant.getId(),
                oldFeeStructureId,
                newFeeStructure.getId(),
                oldMonthlyFee,
                newFeeStructure.getMonthlyFee(),
                "SYSTEM" // TODO: Obtener usuario actual del contexto de seguridad
        );
        eventPublisher.publish(event);

        log.info("Fee structure updated successfully for merchant: {}", merchantId);

        return FeeStructureResponse.from(newFeeStructure);
    }

    private MerchantFeeStructure buildFeeStructure(UpdateFeeStructureRequest request) {
        // Convertir DTOs de transaction fees a domain objects
        List<TransactionFee> transactionFees = request.getTransactionFees().stream()
                .map(this::buildTransactionFee)
                .collect(Collectors.toList());

        // Crear interchange fee (usando valores estándar por ahora)
        InterchangeFee interchangeFee = InterchangeFee.standard("VISA");

        // Crear acquirer fee
        AcquirerFee acquirerFee = AcquirerFee.standard();

        return MerchantFeeStructure.builder()
                .id(FeeStructureId.generate())
                .transactionFees(transactionFees)
                .interchangeFee(interchangeFee)
                .acquirerFee(acquirerFee)
                .monthlyFee(request.getMonthlyFee())
                .setupFee(request.getSetupFee())
                .chargebackFee(request.getChargebackFee())
                .effectiveFrom(request.getEffectiveFrom() != null
                        ? request.getEffectiveFrom()
                        : java.time.LocalDateTime.now())
                .build();
    }

    private TransactionFee buildTransactionFee(UpdateFeeStructureRequest.TransactionFeeDto dto) {
        FeeType feeType = FeeType.valueOf(dto.getFeeType().toUpperCase());

        return TransactionFee.of(
                feeType,
                dto.getPercentageFee(),
                dto.getFixedFee(),
                new HashSet<>(dto.getApplicableCardBrands()),
                new HashSet<>(dto.getApplicableTransactionTypes()),
                dto.getMinimumFee(),
                dto.getMaximumFee()
        );
    }
}
