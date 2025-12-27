package com.unisono.card.limits.infrastructure.config;

import com.unisono.card.limits.domain.port.output.CardValidationPort;
import com.unisono.card.limits.domain.port.output.CreditLimitRepository;
import com.unisono.card.limits.domain.service.CreditLimitManagementService;
import com.unisono.card.limits.domain.service.CreditValidationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Use Cases para card-limits
 */
@Configuration
public class CreditLimitUseCaseConfig {

    /**
     * CreditLimitManagementService implementa:
     * - AssignCreditLimitUseCase
     * - UpdateCreditLimitUseCase
     * - GetCreditInfoUseCase
     * - RecalculateAvailableCreditUseCase
     */
    @Bean
    public CreditLimitManagementService creditLimitManagementService(
            CreditLimitRepository creditLimitRepository,
            CardValidationPort cardValidationPort
    ) {
        return new CreditLimitManagementService(creditLimitRepository, cardValidationPort);
    }

    /**
     * CreditValidationService implementa:
     * - ValidatePurchaseUseCase
     */
    @Bean
    public CreditValidationService creditValidationService(
            CreditLimitRepository creditLimitRepository
    ) {
        return new CreditValidationService(creditLimitRepository);
    }
}
