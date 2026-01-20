package com.unisono.card.merchant.application.port.output;

import com.unisono.card.merchant.domain.model.MerchantId;

/**
 * Puerto de salida para envío de notificaciones a merchants
 */
public interface MerchantNotificationPort {

    /**
     * Envía email de bienvenida al merchant
     */
    void sendWelcomeEmail(MerchantId merchantId, String email, String merchantName);

    /**
     * Notifica activación de merchant
     */
    void notifyActivation(MerchantId merchantId, String email);

    /**
     * Notifica suspensión de merchant
     */
    void notifySuspension(MerchantId merchantId, String email, String reason);

    /**
     * Notifica renovación de contrato
     */
    void notifyContractRenewal(MerchantId merchantId, String email, java.time.LocalDate newEndDate);

    /**
     * Notifica contrato próximo a expirar
     */
    void notifyContractExpiringSoon(MerchantId merchantId, String email, long daysRemaining);

    /**
     * Notifica activación de terminal
     */
    void notifyTerminalActivated(MerchantId merchantId, String email, String terminalSerial);

    /**
     * Notifica actualización de fees
     */
    void notifyFeeStructureUpdate(MerchantId merchantId, String email);
}
