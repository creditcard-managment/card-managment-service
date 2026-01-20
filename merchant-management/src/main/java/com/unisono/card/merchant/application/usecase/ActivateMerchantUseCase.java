package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.response.MerchantResponse;
import com.unisono.card.merchant.application.port.input.MerchantManagementUseCase;
import com.unisono.card.merchant.application.port.output.MerchantEventPublisherPort;
import com.unisono.card.merchant.application.port.output.MerchantPersistencePort;
import com.unisono.card.merchant.domain.event.MerchantActivated;
import com.unisono.card.merchant.domain.exception.MerchantNotFoundException;
import com.unisono.card.merchant.domain.model.Merchant;
import com.unisono.card.merchant.domain.model.MerchantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Activar un merchant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivateMerchantUseCase implements MerchantManagementUseCase.Activate {

    private final MerchantPersistencePort persistencePort;
    private final MerchantEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public MerchantResponse execute(String merchantId, String activatedBy) {
        log.info("Activating merchant: {}", merchantId);

        MerchantId id = MerchantId.of(merchantId);

        Merchant merchant = persistencePort.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id));

        merchant.activate();

        Merchant savedMerchant = persistencePort.save(merchant);

        // Publicar evento - CORREGIDO: agregamos el tercer parámetro
        MerchantActivated event = MerchantActivated.of(
                savedMerchant.getId(),
                savedMerchant.getActivatedAt(), // Segundo parámetro
                activatedBy                      // Tercer parámetro
        );
        eventPublisher.publish(event);

        log.info("Merchant activated successfully: {}", merchantId);

        return MerchantResponse.from(savedMerchant);
    }
}
