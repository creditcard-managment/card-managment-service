package com.unisono.card.merchant.domain.service;

import com.unisono.card.merchant.domain.exception.MerchantNotActiveException;
import com.unisono.card.merchant.domain.model.Merchant;
import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.terminal.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Servicio de dominio para aprovisionamiento de terminales
 * NOTA: Este servicio NO debe tener dependencias de infraestructura
 * Solo contiene lógica de negocio pura
 */
@Slf4j
@Service
public class TerminalProvisioningService {

    /**
     * Aprovisiona una nueva terminal POS para un merchant
     * NOTA: La validación y persistencia se maneja en la capa de aplicación
     */
    public POSTerminal provisionPOSTerminal(
            Merchant merchant,
            String serialNumber,
            String manufacturer,
            String model,
            TerminalConfiguration configuration
    ) {
        Objects.requireNonNull(merchant, "Merchant cannot be null");
        Objects.requireNonNull(serialNumber, "Serial number cannot be null");
        Objects.requireNonNull(configuration, "Configuration cannot be null");

        // Validar que el merchant está activo
        if (!merchant.isActive()) {
            throw new MerchantNotActiveException(
                    "Cannot provision terminal for non-active merchant: " + merchant.getId()
            );
        }

        // Crear la terminal POS
        POSTerminal terminal = POSTerminal.builder()
                .id(TerminalId.generate())
                .merchantId(merchant.getId())
                .serialNumber(serialNumber)
                .configuration(configuration)
                .manufacturer(manufacturer)
                .model(model)
                .hasContactlessReader(true)
                .hasChipReader(true)
                .hasMagneticStripeReader(true)
                .hasPinPad(true)
                .hasPrinter(true)
                .build();

        log.info("POS Terminal {} provisioned for merchant {}", serialNumber, merchant.getId());

        return terminal;
    }

    /**
     * Aprovisiona una terminal virtual para un merchant
     */
    public VirtualTerminal provisionVirtualTerminal(
            Merchant merchant,
            String apiEndpoint,
            TerminalConfiguration configuration
    ) {
        Objects.requireNonNull(merchant, "Merchant cannot be null");
        Objects.requireNonNull(apiEndpoint, "API endpoint cannot be null");
        Objects.requireNonNull(configuration, "Configuration cannot be null");

        // Validar que el merchant está activo
        if (!merchant.isActive()) {
            throw new MerchantNotActiveException(
                    "Cannot provision terminal for non-active merchant: " + merchant.getId()
            );
        }

        // Generar serial number único para terminal virtual
        String serialNumber = generateVirtualTerminalSerial(merchant.getId());

        // Crear la terminal virtual
        VirtualTerminal terminal = VirtualTerminal.builder()
                .id(TerminalId.generate())
                .merchantId(merchant.getId())
                .serialNumber(serialNumber)
                .configuration(configuration)
                .apiEndpoint(apiEndpoint)
                .apiVersion("v1")
                .supports3DSecure(true)
                .supportsTokenization(true)
                .supportsRecurringPayments(false)
                .maxConcurrentTransactions(100)
                .build();

        log.info("Virtual Terminal {} provisioned for merchant {}", serialNumber, merchant.getId());

        return terminal;
    }

    /**
     * Aprovisiona una terminal móvil para un merchant
     */
    public MobileTerminal provisionMobileTerminal(
            Merchant merchant,
            String deviceId,
            String deviceModel,
            String operatingSystem,
            TerminalConfiguration configuration
    ) {
        Objects.requireNonNull(merchant, "Merchant cannot be null");
        Objects.requireNonNull(deviceId, "Device ID cannot be null");
        Objects.requireNonNull(configuration, "Configuration cannot be null");

        // Validar que el merchant está activo
        if (!merchant.isActive()) {
            throw new MerchantNotActiveException(
                    "Cannot provision terminal for non-active merchant: " + merchant.getId()
            );
        }

        // Generar serial number único
        String serialNumber = generateMobileTerminalSerial(merchant.getId(), deviceId);

        // Crear la terminal móvil
        MobileTerminal terminal = MobileTerminal.builder()
                .id(TerminalId.generate())
                .merchantId(merchant.getId())
                .serialNumber(serialNumber)
                .configuration(configuration)
                .deviceId(deviceId)
                .deviceModel(deviceModel)
                .operatingSystem(operatingSystem)
                .appVersion("1.0.0")
                .hasExternalReader(true)
                .supportsBluetooth(true)
                .supportsNFC(true)
                .build();

        log.info("Mobile Terminal {} provisioned for merchant {}", serialNumber, merchant.getId());

        return terminal;
    }

    /**
     * Genera un serial number único para terminal virtual
     */
    private String generateVirtualTerminalSerial(MerchantId merchantId) {
        return "VT-" + merchantId.getValue().substring(0, 8) + "-" + System.currentTimeMillis();
    }

    /**
     * Genera un serial number único para terminal móvil
     */
    private String generateMobileTerminalSerial(MerchantId merchantId, String deviceId) {
        return "MT-" + merchantId.getValue().substring(0, 8) + "-" +
                deviceId.substring(0, Math.min(8, deviceId.length()));
    }
}
