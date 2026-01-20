package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.response.MerchantResponse;
import com.unisono.card.merchant.application.port.input.MerchantManagementUseCase;
import com.unisono.card.merchant.application.port.output.MerchantPersistencePort;
import com.unisono.card.merchant.domain.exception.MerchantNotFoundException;
import com.unisono.card.merchant.domain.model.Merchant;
import com.unisono.card.merchant.domain.model.MerchantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Buscar merchant por ID
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FindMerchantByIdUseCase implements MerchantManagementUseCase.FindById {

    private final MerchantPersistencePort persistencePort;

    @Override
    @Transactional(readOnly = true)
    public MerchantResponse execute(String merchantId) {
        log.info("Finding merchant by ID: {}", merchantId);

        MerchantId id = MerchantId.of(merchantId);

        Merchant merchant = persistencePort.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id));

        return MerchantResponse.from(merchant);
    }
}
