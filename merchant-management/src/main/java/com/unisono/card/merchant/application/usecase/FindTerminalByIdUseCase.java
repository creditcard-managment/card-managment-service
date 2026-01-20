package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.response.TerminalResponse;
import com.unisono.card.merchant.application.port.input.TerminalManagementUseCase;
import com.unisono.card.merchant.application.port.output.TerminalPersistencePort;
import com.unisono.card.merchant.domain.exception.TerminalNotFoundException;
import com.unisono.card.merchant.domain.model.terminal.PaymentTerminal;
import com.unisono.card.merchant.domain.model.terminal.TerminalId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Buscar terminal por ID
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FindTerminalByIdUseCase implements TerminalManagementUseCase.FindById {

    private final TerminalPersistencePort persistencePort;

    @Override
    @Transactional(readOnly = true)
    public TerminalResponse execute(String terminalId) {
        log.info("Finding terminal by ID: {}", terminalId);

        TerminalId id = TerminalId.of(terminalId);

        PaymentTerminal terminal = persistencePort.findById(id)
                .orElseThrow(() -> new TerminalNotFoundException(id));

        return TerminalResponse.from(terminal);
    }
}
