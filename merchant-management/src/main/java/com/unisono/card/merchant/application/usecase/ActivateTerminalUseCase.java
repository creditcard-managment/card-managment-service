package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.response.TerminalResponse;
import com.unisono.card.merchant.application.port.input.TerminalManagementUseCase;
import com.unisono.card.merchant.application.port.output.MerchantEventPublisherPort;
import com.unisono.card.merchant.application.port.output.TerminalPersistencePort;
import com.unisono.card.merchant.domain.event.TerminalActivated;
import com.unisono.card.merchant.domain.exception.TerminalNotFoundException;
import com.unisono.card.merchant.domain.model.terminal.PaymentTerminal;
import com.unisono.card.merchant.domain.model.terminal.TerminalId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Activar terminal
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivateTerminalUseCase implements TerminalManagementUseCase.Activate {

    private final TerminalPersistencePort persistencePort;
    private final MerchantEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public TerminalResponse execute(String terminalId) {
        log.info("Activating terminal: {}", terminalId);

        TerminalId id = TerminalId.of(terminalId);

        PaymentTerminal terminal = persistencePort.findById(id)
                .orElseThrow(() -> new TerminalNotFoundException(id));

        terminal.activate();

        PaymentTerminal savedTerminal = persistencePort.save(terminal);

        // Publicar evento
        TerminalActivated event = TerminalActivated.of(
                savedTerminal.getId(),
                savedTerminal.getMerchantId(),
                savedTerminal.getTerminalType(),
                savedTerminal.getSerialNumber()
        );
        eventPublisher.publish(event);

        log.info("Terminal activated successfully: {}", terminalId);

        return TerminalResponse.from(savedTerminal);
    }
}
