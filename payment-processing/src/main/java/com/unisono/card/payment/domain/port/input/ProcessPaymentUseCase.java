package com.unisono.card.payment.domain.port.input;

import com.unisono.card.payment.application.dto.request.ProcessCashAdvanceCommand;
import com.unisono.card.payment.application.dto.request.ProcessPaymentCommand;
import com.unisono.card.payment.domain.model.*;

/**
 * Use case for processing payment transactions
 */
public interface ProcessPaymentUseCase {

    /**
     * Process a purchase transaction
     */
    Transaction processPurchase(ProcessPaymentCommand command);

    /**
     * Process a cash advance transaction
     */
    Transaction processCashAdvance(ProcessCashAdvanceCommand command);

    /**
     * Authorize a transaction (reserve funds)
     */
    Transaction authorizeTransaction(TransactionId transactionId);

    /**
     * Capture a pre-authorized transaction
     */
    Transaction captureTransaction(TransactionId transactionId);
}
