package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.port.input.ContractManagementUseCase;
import com.unisono.card.merchant.application.port.output.MerchantEventPublisherPort;
import com.unisono.card.merchant.application.port.output.MerchantPersistencePort;
import com.unisono.card.merchant.domain.event.ContractTerminated;
import com.unisono.card.merchant.domain.exception.MerchantNotFoundException;
import com.unisono.card.merchant.domain.model.Merchant;
import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.contract.MerchantContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Terminar contrato de un merchant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TerminateContractUseCase implements ContractManagementUseCase.TerminateContract {

    private final MerchantPersistencePort persistencePort;
    private final MerchantEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public void execute(String merchantId, String reason) {
        log.info("Terminating contract for merchant: {} - Reason: {}", merchantId, reason);

        MerchantId id = MerchantId.of(merchantId);

        Merchant merchant = persistencePort.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id));

        MerchantContract contract = merchant.getContract();

        if (contract == null) {
            throw new IllegalStateException("Merchant does not have a contract to terminate: " + merchantId);
        }

        if (!contract.isActive()) {
            throw new IllegalStateException("Contract is not active and cannot be terminated: " + merchantId);
        }

        // Terminar el contrato
        contract.terminate();

        // También suspender el merchant
        merchant.suspend("Contract terminated: " + reason);

        // Guardar cambios
        persistencePort.save(merchant);

        // Publicar evento
        ContractTerminated event = ContractTerminated.of(
                merchant.getId(),
                contract.getId(),
                reason
        );
        eventPublisher.publish(event);

        log.info("Contract terminated successfully for merchant: {}", merchantId);
    }
}
