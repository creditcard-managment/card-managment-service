package com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.mapper;

import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.terminal.*;
import com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.entity.*;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * MapStruct Mapper para convertir entre PaymentTerminalEntity y PaymentTerminal
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@Component
public interface TerminalEntityMapper {

    // ==========================================
    // Entity -> Domain (con lógica polimórfica)
    // ==========================================

    default PaymentTerminal toDomain(PaymentTerminalEntity entity) {
        if (entity == null) return null;

        if (entity instanceof POSTerminalEntity pos) {
            return toPOSTerminal(pos);
        } else if (entity instanceof VirtualTerminalEntity virtual) {
            return toVirtualTerminal(virtual);
        } else if (entity instanceof MobileTerminalEntity mobile) {
            return toMobileTerminal(mobile);
        }

        throw new IllegalArgumentException("Unknown terminal type: " + entity.getClass());
    }

    // ==========================================
    // POS Terminal Mapping
    // ==========================================

    default POSTerminal toPOSTerminal(POSTerminalEntity entity) {
        if (entity == null) return null;

        return POSTerminal.builder()
                .id(TerminalId.of(entity.getId()))
                .merchantId(MerchantId.of(entity.getMerchant().getId()))
                .serialNumber(entity.getSerialNumber())
                .configuration(mapConfiguration(entity))
                .manufacturer(entity.getManufacturer())
                .model(entity.getModel())
                .firmwareVersion(entity.getFirmwareVersion())
                .hasContactlessReader(entity.isHasContactlessReader())
                .hasChipReader(entity.isHasChipReader())
                .hasMagneticStripeReader(entity.isHasMagneticStripeReader())
                .hasPinPad(entity.isHasPinPad())
                .hasPrinter(entity.isHasPrinter())
                .build();
    }

    default POSTerminalEntity toPOSTerminalEntity(POSTerminal terminal) {
        if (terminal == null) return null;

        POSTerminalEntity entity = new POSTerminalEntity();
        mapCommonFields(terminal, entity);

        entity.setManufacturer(terminal.getManufacturer());
        entity.setModel(terminal.getModel());
        entity.setFirmwareVersion(terminal.getFirmwareVersion());
        entity.setHasContactlessReader(terminal.isHasContactlessReader());
        entity.setHasChipReader(terminal.isHasChipReader());
        entity.setHasMagneticStripeReader(terminal.isHasMagneticStripeReader());
        entity.setHasPinPad(terminal.isHasPinPad());
        entity.setHasPrinter(terminal.isHasPrinter());

        return entity;
    }

    // ==========================================
    // Virtual Terminal Mapping
    // ==========================================

    default VirtualTerminal toVirtualTerminal(VirtualTerminalEntity entity) {
        if (entity == null) return null;

        return VirtualTerminal.builder()
                .id(TerminalId.of(entity.getId()))
                .merchantId(MerchantId.of(entity.getMerchant().getId()))
                .serialNumber(entity.getSerialNumber())
                .configuration(mapConfiguration(entity))
                .apiEndpoint(entity.getApiEndpoint())
                .apiVersion(entity.getApiVersion())
                .allowedIpAddresses(entity.getAllowedIpAddresses())
                .supports3DSecure(entity.isSupports3DSecure())
                .supportsTokenization(entity.isSupportsTokenization())
                .supportsRecurringPayments(entity.isSupportsRecurringPayments())
                .maxConcurrentTransactions(entity.getMaxConcurrentTransactions())
                .build();
    }

    default VirtualTerminalEntity toVirtualTerminalEntity(VirtualTerminal terminal) {
        if (terminal == null) return null;

        VirtualTerminalEntity entity = new VirtualTerminalEntity();
        mapCommonFields(terminal, entity);

        entity.setApiEndpoint(terminal.getApiEndpoint());
        entity.setApiVersion(terminal.getApiVersion());
        entity.setAllowedIpAddresses(terminal.getAllowedIpAddresses());
        entity.setSupports3DSecure(terminal.isSupports3DSecure());
        entity.setSupportsTokenization(terminal.isSupportsTokenization());
        entity.setSupportsRecurringPayments(terminal.isSupportsRecurringPayments());
        entity.setMaxConcurrentTransactions(terminal.getMaxConcurrentTransactions());

        return entity;
    }

    // ==========================================
    // Mobile Terminal Mapping
    // ==========================================

    default MobileTerminal toMobileTerminal(MobileTerminalEntity entity) {
        if (entity == null) return null;

        return MobileTerminal.builder()
                .id(TerminalId.of(entity.getId()))
                .merchantId(MerchantId.of(entity.getMerchant().getId()))
                .serialNumber(entity.getSerialNumber())
                .configuration(mapConfiguration(entity))
                .deviceId(entity.getDeviceId())
                .deviceModel(entity.getDeviceModel())
                .operatingSystem(entity.getOperatingSystem())
                .appVersion(entity.getAppVersion())
                .hasExternalReader(entity.isHasExternalReader())
                .supportsBluetooth(entity.isSupportsBluetooth())
                .supportsNFC(entity.isSupportsNFC())
                .readerSerialNumber(entity.getReaderSerialNumber())
                .build();
    }

    default MobileTerminalEntity toMobileTerminalEntity(MobileTerminal terminal) {
        if (terminal == null) return null;

        MobileTerminalEntity entity = new MobileTerminalEntity();
        mapCommonFields(terminal, entity);

        entity.setDeviceId(terminal.getDeviceId());
        entity.setDeviceModel(terminal.getDeviceModel());
        entity.setOperatingSystem(terminal.getOperatingSystem());
        entity.setAppVersion(terminal.getAppVersion());
        entity.setHasExternalReader(terminal.isHasExternalReader());
        entity.setSupportsBluetooth(terminal.isSupportsBluetooth());
        entity.setSupportsNFC(terminal.isSupportsNFC());
        entity.setReaderSerialNumber(terminal.getReaderSerialNumber());

        return entity;
    }

    // ==========================================
    // Domain -> Entity (con lógica polimórfica)
    // ==========================================

    default PaymentTerminalEntity toEntity(PaymentTerminal terminal) {
        if (terminal == null) return null;

        if (terminal instanceof POSTerminal pos) {
            return toPOSTerminalEntity(pos);
        } else if (terminal instanceof VirtualTerminal virtual) {
            return toVirtualTerminalEntity(virtual);
        } else if (terminal instanceof MobileTerminal mobile) {
            return toMobileTerminalEntity(mobile);
        }

        throw new IllegalArgumentException("Unknown terminal type: " + terminal.getClass());
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    default TerminalConfiguration mapConfiguration(PaymentTerminalEntity entity) {
        if (entity == null) return null;

        return TerminalConfiguration.of(
                entity.getAcceptedCardBrands() != null ? entity.getAcceptedCardBrands() : Set.of(),
                entity.getMaxTransactionAmount() != null ? entity.getMaxTransactionAmount() : new BigDecimal("999999.99"),
                entity.getMinTransactionAmount() != null ? entity.getMinTransactionAmount() : new BigDecimal("1.00"),
                entity.isAllowRefunds(),
                entity.isAllowVoids(),
                entity.isAllowPartialApprovals(),
                entity.isRequireSignature(),
                entity.getSignatureThreshold() != null ? entity.getSignatureThreshold() : new BigDecimal("1000.00"),
                entity.isRequirePin(),
                entity.getTimeoutSeconds() != null ? entity.getTimeoutSeconds() : 30,
                entity.getCurrency() != null ? entity.getCurrency() : "ARS"
        );
    }

    default void mapCommonFields(PaymentTerminal terminal, PaymentTerminalEntity entity) {
        entity.setId(terminal.getId().getValue());
        entity.setSerialNumber(terminal.getSerialNumber());
        entity.setStatus(terminal.getStatus());
        entity.setActivatedAt(terminal.getActivatedAt());
        entity.setLastUsedAt(terminal.getLastUsedAt());
        entity.setDeactivatedAt(terminal.getDeactivatedAt());

        // Configuration
        TerminalConfiguration config = terminal.getConfiguration();
        entity.setAcceptedCardBrands(config.getAcceptedCardBrands());
        entity.setMaxTransactionAmount(config.getMaxTransactionAmount());
        entity.setMinTransactionAmount(config.getMinTransactionAmount());
        entity.setAllowRefunds(config.isAllowRefunds());
        entity.setAllowVoids(config.isAllowVoids());
        entity.setAllowPartialApprovals(config.isAllowPartialApprovals());
        entity.setRequireSignature(config.isRequireSignature());
        entity.setSignatureThreshold(config.getSignatureThreshold());
        entity.setRequirePin(config.isRequirePin());
        entity.setTimeoutSeconds(config.getTimeoutSeconds());
        entity.setCurrency(config.getCurrency());
    }
}
