package com.unisono.card.merchant.application.usecase;

import com.unisono.card.merchant.application.dto.request.UpdateMerchantProfileRequest;
import com.unisono.card.merchant.application.dto.response.MerchantResponse;
import com.unisono.card.merchant.application.port.input.MerchantManagementUseCase;
import com.unisono.card.merchant.application.port.output.MerchantPersistencePort;
import com.unisono.card.merchant.domain.exception.MerchantNotFoundException;
import com.unisono.card.merchant.domain.model.Address;
import com.unisono.card.merchant.domain.model.Merchant;
import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.MerchantProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Actualizar perfil de merchant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateMerchantProfileUseCase implements MerchantManagementUseCase.UpdateProfile {

    private final MerchantPersistencePort persistencePort;

    @Override
    @Transactional
    public MerchantResponse execute(String merchantId, UpdateMerchantProfileRequest request) {
        log.info("Updating merchant profile: {}", merchantId);

        MerchantId id = MerchantId.of(merchantId);

        Merchant merchant = persistencePort.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id));

        // Construir nuevo perfil
        Address address = Address.of(
                request.getStreetAddress(),
                request.getStreetNumber(),
                request.getCity(),
                request.getState(),
                request.getPostalCode(),
                request.getCountry()
        );

        MerchantProfile newProfile = MerchantProfile.of(
                request.getContactName(),
                request.getContactEmail(),
                request.getContactPhone(),
                request.getWebsite(),
                address
        );

        merchant.updateProfile(newProfile);

        Merchant savedMerchant = persistencePort.save(merchant);

        log.info("Merchant profile updated successfully: {}", merchantId);

        return MerchantResponse.from(savedMerchant);
    }
}
