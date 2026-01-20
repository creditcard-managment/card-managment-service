package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.response.MerchantResponse;
import com.unisono.card.merchant.application.port.input.MerchantManagementUseCase;
import com.unisono.card.merchant.application.port.output.MerchantPersistencePort;
import com.unisono.card.merchant.domain.model.Merchant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Caso de uso: Listar todos los merchants con paginación
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FindAllMerchantsUseCase implements MerchantManagementUseCase.FindAll {

    private final MerchantPersistencePort persistencePort;

    @Override
    @Transactional(readOnly = true)
    public List<MerchantResponse> execute(int page, int size) {
        log.info("Finding all merchants - page: {}, size: {}", page, size);

        List<Merchant> merchants = persistencePort.findAll(page, size);

        return merchants.stream()
                .map(MerchantResponse::from)
                .collect(Collectors.toList());
    }
}
