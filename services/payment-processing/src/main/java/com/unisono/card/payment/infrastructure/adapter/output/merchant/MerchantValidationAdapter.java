package com.unisono.card.payment.infrastructure.adapter.output.merchant;

import com.unisono.card.payment.domain.port.output.MerchantValidationPort;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentTerminalEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaMerchantRepository;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentTerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adapter for validating merchants against the shared tables merchants and payment_terminals
 *
 * No hay un servicio de comercios: las tablas viven en la base compartida (migración V4)
 * y transactions.merchant_id tiene FK a merchants(id).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantValidationAdapter implements MerchantValidationPort {

    static final String ACTIVE = "ACTIVE";

    private final JpaMerchantRepository merchantRepository;
    private final JpaPaymentTerminalRepository terminalRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isMerchantActive(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return false;
        }

        boolean active = merchantRepository.existsByIdAndStatus(merchantId, ACTIVE);
        log.debug("Merchant {} active: {}", merchantId, active);
        return active;
    }

    @Override
    public boolean isValidMCC(String mcc) {
        // Basic MCC validation (4 digits)
        return mcc != null && mcc.matches("\\d{4}");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findMerchantIdByTerminal(String terminalId) {
        if (terminalId == null || terminalId.isBlank()) {
            return Optional.empty();
        }

        Optional<String> merchantId = terminalRepository.findByIdAndStatus(terminalId, ACTIVE)
                .map(PaymentTerminalEntity::getMerchantId);
        log.debug("Terminal {} belongs to merchant {}", terminalId, merchantId.orElse("-"));
        return merchantId;
    }
}
