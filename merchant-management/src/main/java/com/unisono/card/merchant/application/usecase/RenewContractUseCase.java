package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.request.RenewContractRequest;
import com.unisono.card.merchant.application.dto.response.ContractResponse;
import com.unisono.card.merchant.application.port.input.ContractManagementUseCase;
import com.unisono.card.merchant.application.port.output.MerchantEventPublisherPort;
import com.unisono.card.merchant.application.port.output.MerchantPersistencePort;
import com.unisono.card.merchant.domain.event.ContractRenewed;
import com.unisono.card.merchant.domain.exception.MerchantNotFoundException;
import com.unisono.card.merchant.domain.model.Merchant;
import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.contract.MerchantContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Caso de uso: Renovar contrato de un merchant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RenewContractUseCase implements ContractManagementUseCase.RenewContract {

    private final MerchantPersistencePort persistencePort;
    private final MerchantEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public ContractResponse execute(String merchantId, RenewContractRequest request) {
        log.info("Renewing contract for merchant: {}", merchantId);

        MerchantId id = MerchantId.of(merchantId);

        Merchant merchant = persistencePort.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id));

        MerchantContract currentContract = merchant.getContract();

        if (currentContract == null) {
            throw new IllegalStateException("Merchant does not have a contract to renew: " + merchantId);
        }

        // Validar que el contrato pueda ser renovado
        if (!currentContract.isAutoRenew() && !currentContract.isExpiringSoon(30)) {
            throw new IllegalStateException(
                    "Contract cannot be renewed at this time. Auto-renew is disabled or contract is not expiring soon."
            );
        }

        // Guardar información del contrato anterior
        LocalDate oldEndDate = currentContract.getEndDate();

        // Renovar el contrato
        currentContract.renew(request.getNewEndDate());

        // Actualizar configuración de auto-renovación
        currentContract.updateAutoRenew(request.isAutoRenew(), request.getRenewalPeriodMonths());

        // Guardar cambios
        Merchant savedMerchant = persistencePort.save(merchant);
        MerchantContract renewedContract = savedMerchant.getContract();

        // Publicar evento
        ContractRenewed event = ContractRenewed.of(
                savedMerchant.getId(),
                renewedContract.getId(),
                renewedContract.getId(),
                oldEndDate,
                renewedContract.getEndDate()
        );
        eventPublisher.publish(event);

        log.info("Contract renewed successfully for merchant: {} until {}",
                merchantId, request.getNewEndDate());

        return ContractResponse.from(renewedContract);
    }
}
