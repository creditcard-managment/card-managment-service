package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.response.FeeStructureResponse;
import com.unisono.card.merchant.application.port.input.FeeManagementUseCase;
import com.unisono.card.merchant.application.port.output.MerchantPersistencePort;
import com.unisono.card.merchant.domain.exception.MerchantNotFoundException;
import com.unisono.card.merchant.domain.model.Merchant;
import com.unisono.card.merchant.domain.model.MerchantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Obtener estructura de fees de un merchant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetFeeStructureUseCase implements FeeManagementUseCase.GetFeeStructure {

    private final MerchantPersistencePort persistencePort;

    @Override
    @Transactional(readOnly = true)
    public FeeStructureResponse execute(String merchantId) {
        log.info("Getting fee structure for merchant: {}", merchantId);

        MerchantId id = MerchantId.of(merchantId);

        Merchant merchant = persistencePort.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id));

        if (merchant.getContract() == null || merchant.getContract().getFeeStructure() == null) {
            throw new IllegalStateException("Merchant does not have a fee structure configured");
        }

        return FeeStructureResponse.from(merchant.getContract().getFeeStructure());
    }
}
