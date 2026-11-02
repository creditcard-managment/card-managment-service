package com.unisono.card.payment.domain.port.output;

import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.Transaction;

/**
 * Port for external authorization service (network processor)
 */
public interface AuthorizationPort {

    /**
     * Request authorization from card network (Visa/Mastercard/etc)
     */
    AuthorizationCode requestAuthorization(Transaction transaction);

    /**
     * Capture a pre-authorized transaction
     */
    void captureAuthorization(Transaction transaction);

    /**
     * Void/cancel an authorization
     */
    void voidAuthorization(Transaction transaction);
}
