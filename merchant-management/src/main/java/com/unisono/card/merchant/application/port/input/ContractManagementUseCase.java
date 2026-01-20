package com.unisono.card.merchant.application.port.input;

import com.unisono.card.merchant.application.dto.request.RenewContractRequest;
import com.unisono.card.merchant.application.dto.response.ContractResponse;

/**
 * Puerto de entrada para gestión de contratos
 */
public interface ContractManagementUseCase {

    interface GetContract {
        ContractResponse execute(String merchantId);
    }

    interface RenewContract {
        ContractResponse execute(String merchantId, RenewContractRequest request);
    }

    interface TerminateContract {
        void execute(String merchantId, String reason);
    }
}
