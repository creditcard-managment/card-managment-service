package com.unisono.card.merchant.application.port.output;

import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.terminal.PaymentTerminal;
import com.unisono.card.merchant.domain.model.terminal.TerminalId;
import com.unisono.card.merchant.domain.model.terminal.TerminalStatus;
import com.unisono.card.merchant.domain.model.terminal.TerminalType;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de Terminal
 */
public interface TerminalPersistencePort {

    PaymentTerminal save(PaymentTerminal terminal);

    Optional<PaymentTerminal> findById(TerminalId terminalId);

    Optional<PaymentTerminal> findBySerialNumber(String serialNumber);

    List<PaymentTerminal> findByMerchantId(MerchantId merchantId);

    List<PaymentTerminal> findActiveByMerchantId(MerchantId merchantId);

    List<PaymentTerminal> findByType(TerminalType type);

    List<PaymentTerminal> findByStatus(TerminalStatus status);

    boolean existsBySerialNumber(String serialNumber);

    long countActiveByMerchantId(MerchantId merchantId);

    void delete(TerminalId terminalId);
}
