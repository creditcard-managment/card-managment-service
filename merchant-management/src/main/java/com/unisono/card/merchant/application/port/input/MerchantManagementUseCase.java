package com.unisono.card.merchant.application.port.input;

import com.unisono.card.merchant.application.dto.request.RegisterMerchantRequest;
import com.unisono.card.merchant.application.dto.request.UpdateMerchantProfileRequest;
import com.unisono.card.merchant.application.dto.response.MerchantResponse;

/**
 * Puerto de entrada para gestión de merchants
 */
public interface MerchantManagementUseCase {

    interface Register {
        MerchantResponse execute(RegisterMerchantRequest request);
    }

    interface Activate {
        MerchantResponse execute(String merchantId, String activatedBy);
    }

    interface Suspend {
        MerchantResponse execute(String merchantId, String reason, String suspendedBy);
    }

    interface UpdateProfile {
        MerchantResponse execute(String merchantId, UpdateMerchantProfileRequest request);
    }

    interface FindById {
        MerchantResponse execute(String merchantId);
    }

    interface FindAll {
        java.util.List<MerchantResponse> execute(int page, int size);
    }
}
