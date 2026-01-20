package com.unisono.card.merchant.domain.repository;

import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.terminal.*;

import java.util.List;
import java.util.Optional;

/**
 * Repository para terminales de pago
 */
public interface PaymentTerminalRepository {


    /**
     * Guarda o actualiza una terminal
     */
    PaymentTerminal save(PaymentTerminal terminal);

    /**
     * Guarda o actualiza una terminal POS (método específico)
     */
    default POSTerminal save(POSTerminal terminal) {
        return (POSTerminal) save((PaymentTerminal) terminal);
    }

    /**
     * Guarda o actualiza una terminal móvil (método específico)
     */
    default MobileTerminal save(MobileTerminal terminal) {
        return (MobileTerminal) save((PaymentTerminal) terminal);
    }

    /**
     * Guarda o actualiza una terminal virtual (método específico)
     */
    default VirtualTerminal save(VirtualTerminal terminal) {
        return (VirtualTerminal) save((PaymentTerminal) terminal);
    }

    /**
     * Busca una terminal por su ID
     */
    Optional<PaymentTerminal> findById(TerminalId terminalId);

    /**
     * Busca una terminal por número de serie
     */
    Optional<PaymentTerminal> findBySerialNumber(String serialNumber);

    /**
     * Busca todas las terminales de un merchant
     */
    List<PaymentTerminal> findByMerchantId(MerchantId merchantId);

    /**
     * Busca terminales activas de un merchant
     */
    List<PaymentTerminal> findActiveByMerchantId(MerchantId merchantId);

    /**
     * Busca terminales por tipo
     */
    List<PaymentTerminal> findByType(TerminalType type);

    /**
     * Busca terminales por estado
     */
    List<PaymentTerminal> findByStatus(TerminalStatus status);

    /**
     * Busca terminales de un merchant por tipo
     */
    List<PaymentTerminal> findByMerchantIdAndType(MerchantId merchantId, TerminalType type);

    /**
     * Verifica si existe una terminal con el serial number dado
     */
    boolean existsBySerialNumber(String serialNumber);

    /**
     * Cuenta terminales activas de un merchant
     */
    long countActiveByMerchantId(MerchantId merchantId);

    /**
     * Elimina una terminal
     */
    void delete(TerminalId terminalId);

    /**
     * Encuentra terminales que requieren mantenimiento
     */
    List<PaymentTerminal> findTerminalsRequiringMaintenance();
}
