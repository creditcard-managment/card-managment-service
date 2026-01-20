package com.unisono.card.data.infrastructure.config;

import com.unisono.card.data.domain.port.input.*;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.data.domain.port.output.EncryptionPort;
import com.unisono.card.data.domain.service.CardManagementService;
import com.unisono.card.data.domain.service.CardSecurityService;
import com.unisono.card.data.domain.service.CardValidationService;
import com.unisono.card.data.domain.service.PINManagementService;
import com.unisono.card.data.domain.service.PinEncryptionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Use Cases
 *
 * Define beans de servicios de dominio que implementan los use cases.
 */
@Configuration
public class CardUseCaseConfig {

    /**
     * CardManagementService implementa:
     * - CreateCardUseCase
     * - GetCardUseCase
     */
    @Bean
    public CardManagementService cardManagementService(CardRepository cardRepository, ApplicationEventPublisher event, PinEncryptionService pinEncryptionService) {
        return new CardManagementService(cardRepository, event, pinEncryptionService);
    }

    /**
     * CardSecurityService implementa:
     * - BlockCardUseCase
     * - UnblockCardUseCase
     */
    @Bean
    public CardSecurityService cardSecurityService(CardRepository cardRepository) {
        return new CardSecurityService(cardRepository);
    }

    /**
     * PINManagementService implementa:
     * - ValidatePINUseCase
     * - ResetPINUseCase

    @Bean
    public PINManagementService pinManagementService(
            CardRepository cardRepository,
            EncryptionPort encryptionPort
    ) {
        return new PINManagementService(cardRepository, encryptionPort);
    }
     */

    /**
     * CardValidationService (servicio auxiliar)
     */
    @Bean
    public CardValidationService cardValidationService() {
        return new CardValidationService();
    }
}
