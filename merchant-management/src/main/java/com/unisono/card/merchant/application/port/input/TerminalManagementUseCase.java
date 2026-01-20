package com.unisono.card.merchant.application.port.input;

import com.unisono.card.merchant.application.dto.request.ProvisionTerminalRequest;
import com.unisono.card.merchant.application.dto.response.TerminalResponse;

import java.util.List;

/**
 * Puerto de entrada para gestión de terminales
 */
public interface TerminalManagementUseCase {

    interface Provision {
        TerminalResponse execute(ProvisionTerminalRequest request);
    }

    interface Activate {
        TerminalResponse execute(String terminalId);
    }

    interface Deactivate {
        TerminalResponse execute(String terminalId, String reason);
    }

    interface FindById {
        TerminalResponse execute(String terminalId);
    }

    interface FindByMerchant {
        List<TerminalResponse> execute(String merchantId);
    }
}
