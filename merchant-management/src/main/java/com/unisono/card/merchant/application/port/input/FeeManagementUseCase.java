package com.unisono.card.merchant.application.port.input;

import com.unisono.card.merchant.application.dto.request.UpdateFeeStructureRequest;
import com.unisono.card.merchant.application.dto.response.FeeStructureResponse;

/**
 * Puerto de entrada para gestión de fees
 */
public interface FeeManagementUseCase {

    interface GetFeeStructure {
        FeeStructureResponse execute(String merchantId);
    }

    interface UpdateFeeStructure {
        FeeStructureResponse execute(String merchantId, UpdateFeeStructureRequest request);
    }
}
