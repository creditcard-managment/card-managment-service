package com.unisono.card.payment.domain.port.input;

import com.unisono.card.payment.application.dto.request.ProcessRefundCommand;
import com.unisono.card.payment.domain.model.Transaction;

/**
 * Use case for processing refund transactions
 */
public interface ProcessRefundUseCase {

    /**
     * Process a full refund for an original transaction
     */
    Transaction processFullRefund(ProcessRefundCommand command);

    /**
     * Process a partial refund for an original transaction
     */
    Transaction processPartialRefund(ProcessRefundCommand command);
}
