package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.response.MerchantResponse;
import com.unisono.card.merchant.application.port.input.MerchantManagementUseCase;
import com.unisono.card.merchant.application.port.output.MerchantEventPublisherPort;
import com.unisono.card.merchant.application.port.output.MerchantPersistencePort;
import com.unisono.card.merchant.domain.event.MerchantSuspended;
import com.unisono.card.merchant.domain.exception.MerchantNotFoundException;
import com.unisono.card.merchant.domain.model.Merchant;
import com.unisono.card.merchant.domain.model.MerchantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Caso de uso: Suspender un merchant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuspendMerchantUseCase implements MerchantManagementUseCase.Suspend {

    private final MerchantPersistencePort persistencePort;
    private final MerchantEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public MerchantResponse execute(String merchantId, String reason, String suspendedBy) {
        log.info("Suspending merchant: {} - Reason: {}", merchantId, reason);

        MerchantId id = MerchantId.of(merchantId);

        Merchant merchant = persistencePort.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id));

        merchant.suspend(reason);

        Merchant savedMerchant = persistencePort.save(merchant);

        // Publicar evento - CORREGIDO
        MerchantSuspended event = MerchantSuspended.of(
                savedMerchant.getId(),
                reason,
                suspendedBy
        );
        eventPublisher.publish(event);

        log.info("Merchant suspended successfully: {}", merchantId);

        return MerchantResponse.from(savedMerchant);
    }
}
