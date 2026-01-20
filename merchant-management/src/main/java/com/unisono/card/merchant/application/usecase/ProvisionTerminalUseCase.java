package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.request.ProvisionTerminalRequest;
import com.unisono.card.merchant.application.dto.response.TerminalResponse;
import com.unisono.card.merchant.application.port.input.TerminalManagementUseCase;
import com.unisono.card.merchant.application.port.output.MerchantEventPublisherPort;
import com.unisono.card.merchant.application.port.output.MerchantPersistencePort;
import com.unisono.card.merchant.application.port.output.TerminalPersistencePort;
import com.unisono.card.merchant.domain.event.TerminalActivated;
import com.unisono.card.merchant.domain.exception.MerchantNotFoundException;
import com.unisono.card.merchant.domain.exception.TerminalAlreadyExistsException;
import com.unisono.card.merchant.domain.model.Merchant;
import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.terminal.*;
import com.unisono.card.merchant.domain.service.TerminalProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Caso de uso: Provisionar una nueva terminal
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProvisionTerminalUseCase implements TerminalManagementUseCase.Provision {

    private final TerminalProvisioningService provisioningService;
    private final MerchantPersistencePort merchantPersistencePort;
    private final TerminalPersistencePort terminalPersistencePort;
    private final MerchantEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public TerminalResponse execute(ProvisionTerminalRequest request) {
        log.info("Provisioning terminal for merchant: {}", request.getMerchantId());

        MerchantId merchantId = MerchantId.of(request.getMerchantId());

        // Obtener el merchant
        Merchant merchant = merchantPersistencePort.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));

        // Validar que el serial number sea único
        if (terminalPersistencePort.existsBySerialNumber(request.getSerialNumber())) {
            throw new TerminalAlreadyExistsException(
                    "Terminal with serial number " + request.getSerialNumber() + " already exists"
            );
        }

        TerminalConfiguration configuration = buildConfiguration(request);

        // Crear la terminal usando el servicio de dominio
        PaymentTerminal terminal = switch (request.getTerminalType()) {
            case POS -> provisioningService.provisionPOSTerminal(
                    merchant,
                    request.getSerialNumber(),
                    request.getManufacturer(),
                    request.getModel(),
                    configuration
            );
            case VIRTUAL -> provisioningService.provisionVirtualTerminal(
                    merchant,
                    request.getApiEndpoint(),
                    configuration
            );
            case MOBILE -> provisioningService.provisionMobileTerminal(
                    merchant,
                    request.getDeviceId(),
                    request.getDeviceModel(),
                    request.getOperatingSystem(),
                    configuration
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported terminal type: " + request.getTerminalType()
            );
        };

        // Persistir la terminal
        PaymentTerminal savedTerminal = terminalPersistencePort.save(terminal);

        // Agregar la terminal al merchant
        merchant.addTerminal(savedTerminal);
        merchantPersistencePort.save(merchant);

        // Si se solicita activación inmediata
        if (request.isActivateImmediately()) {
            savedTerminal.activate();
            terminalPersistencePort.save(savedTerminal);

            // Publicar evento
            TerminalActivated event = TerminalActivated.of(
                    savedTerminal.getId(),
                    merchantId,
                    savedTerminal.getTerminalType(),
                    savedTerminal.getSerialNumber()
            );
            eventPublisher.publish(event);
        }

        log.info("Terminal provisioned successfully: {}", savedTerminal.getId());

        return TerminalResponse.from(savedTerminal);
    }

    private TerminalConfiguration buildConfiguration(ProvisionTerminalRequest request) {
        return TerminalConfiguration.of(
                Set.of("VISA", "MASTERCARD", "AMEX"),
                new BigDecimal("999999.99"),
                new BigDecimal("1.00"),
                true,
                true,
                false,
                true,
                new BigDecimal("1000.00"),
                true,
                30,
                "ARS"
        );
    }
}
