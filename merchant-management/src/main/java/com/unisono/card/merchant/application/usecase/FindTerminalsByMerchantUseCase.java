package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.response.TerminalResponse;
import com.unisono.card.merchant.application.port.input.TerminalManagementUseCase;
import com.unisono.card.merchant.application.port.output.TerminalPersistencePort;
import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.terminal.PaymentTerminal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Caso de uso: Buscar terminales por merchant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FindTerminalsByMerchantUseCase implements TerminalManagementUseCase.FindByMerchant {

    private final TerminalPersistencePort persistencePort;

    @Override
    @Transactional(readOnly = true)
    public List<TerminalResponse> execute(String merchantId) {
        log.info("Finding terminals by merchant ID: {}", merchantId);

        MerchantId id = MerchantId.of(merchantId);

        List<PaymentTerminal> terminals = persistencePort.findByMerchantId(id);

        return terminals.stream()
                .map(TerminalResponse::from)
                .collect(Collectors.toList());
    }
}
