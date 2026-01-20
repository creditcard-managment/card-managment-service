package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.response.ContractResponse;
import com.unisono.card.merchant.application.port.input.ContractManagementUseCase;
import com.unisono.card.merchant.application.port.output.MerchantPersistencePort;
import com.unisono.card.merchant.domain.exception.MerchantNotFoundException;
import com.unisono.card.merchant.domain.model.Merchant;
import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.contract.MerchantContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Obtener contrato de un merchant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetContractUseCase implements ContractManagementUseCase.GetContract {

    private final MerchantPersistencePort persistencePort;

    @Override
    @Transactional(readOnly = true)
    public ContractResponse execute(String merchantId) {
        log.info("Getting contract for merchant: {}", merchantId);

        MerchantId id = MerchantId.of(merchantId);

        Merchant merchant = persistencePort.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id));

        MerchantContract contract = merchant.getContract();

        if (contract == null) {
            throw new IllegalStateException("Merchant does not have a contract: " + merchantId);
        }

        log.debug("Contract found for merchant: {}", merchantId);

        return ContractResponse.from(contract);
    }
}
