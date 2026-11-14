package com.unisono.card.limits.domain.service;

import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.limits.domain.port.input.ValidatePurchaseUseCase.ValidateCashAdvanceCommand;
import com.unisono.card.limits.domain.port.input.ValidatePurchaseUseCase.ValidatePurchaseCommand;
import com.unisono.card.limits.domain.port.input.ValidatePurchaseUseCase.ValidationResult;
import com.unisono.card.limits.domain.port.output.CardValidationPort;
import com.unisono.card.limits.domain.port.output.CreditLimitRepository;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * validate/purchase y validate/cash-advance: una tarjeta que no está activa se rechaza (200, approved = false)
 */
@ExtendWith(MockitoExtension.class)
class CreditValidationServiceTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private CreditLimitRepository creditLimitRepository;

    @Mock
    private CardValidationPort cardValidationPort;

    private CreditValidationService service;

    @BeforeEach
    void setUp() {
        service = new CreditValidationService(creditLimitRepository, cardValidationPort);
        when(creditLimitRepository.findByCardId(CARD_ID))
                .thenReturn(Optional.of(CreditLimit.assign(CARD_ID, Money.ofARS(500_000), 0.3, "test")));
    }

    @Test
    void activeCardWithinLimitsIsApproved() {
        when(cardValidationPort.isCardActive(CARD_ID)).thenReturn(true);

        assertThat(service.validatePurchase(new ValidatePurchaseCommand(CARD_ID, Money.ofARS(5_000))).approved())
                .isTrue();
        assertThat(service.validateCashAdvance(new ValidateCashAdvanceCommand(CARD_ID, Money.ofARS(5_000))).approved())
                .isTrue();
    }

    @Test
    void inactiveCardIsRejectedWithItsReason() {
        when(cardValidationPort.isCardActive(CARD_ID)).thenReturn(false);

        ValidationResult purchase = service.validatePurchase(new ValidatePurchaseCommand(CARD_ID, Money.ofARS(5_000)));
        ValidationResult advance = service.validateCashAdvance(new ValidateCashAdvanceCommand(CARD_ID, Money.ofARS(5_000)));

        assertThat(purchase.approved()).isFalse();
        assertThat(purchase.reason()).isEqualTo(CreditValidationService.CARD_NOT_ACTIVE);
        assertThat(advance.approved()).isFalse();
        assertThat(advance.reason()).isEqualTo(CreditValidationService.CARD_NOT_ACTIVE);
    }
}
