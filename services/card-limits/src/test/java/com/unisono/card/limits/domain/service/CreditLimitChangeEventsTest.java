package com.unisono.card.limits.domain.service;

import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CreditLimitAssignedEvent;
import com.unisono.card.events.domain.CreditLimitUpdatedEvent;
import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.limits.domain.port.input.AssignCreditLimitUseCase.AssignCreditLimitCommand;
import com.unisono.card.limits.domain.port.input.UpdateCreditLimitUseCase.DecreasePurchaseLimitCommand;
import com.unisono.card.limits.domain.port.input.UpdateCreditLimitUseCase.IncreasePurchaseLimitCommand;
import com.unisono.card.limits.domain.port.input.UpdateCreditLimitUseCase.UpdateCashAdvanceLimitCommand;
import com.unisono.card.limits.domain.port.input.UpdateCreditLimitUseCase.UpdateTemporalLimitsCommand;
import com.unisono.card.limits.domain.port.output.CardValidationPort;
import com.unisono.card.limits.domain.port.output.CreditLimitRepository;
import com.unisono.card.limits.domain.port.output.CreditOperationRepository;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Eventos credit-limit.assigned y credit-limit.updated de la asignación y los cambios de límites
 */
@ExtendWith(MockitoExtension.class)
class CreditLimitChangeEventsTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private CreditLimitRepository creditLimitRepository;

    @Mock
    private CardValidationPort cardValidationPort;

    @Mock
    private CreditOperationRepository creditOperationRepository;

    private final List<Map.Entry<String, DomainEvent>> outbox = new ArrayList<>();
    private CreditLimitManagementService service;

    @BeforeEach
    void setUp() {
        service = new CreditLimitManagementService(creditLimitRepository, cardValidationPort, creditOperationRepository,
                new CreditLimitEventEmitter((topic, key, event) -> outbox.add(Map.entry(topic, event))));
        lenient().when(creditLimitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(creditLimitRepository.saveHistory(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void assignmentPublishesASingleAssignedEventWithEveryLimit() {
        when(cardValidationPort.cardExists(CARD_ID)).thenReturn(true);
        when(cardValidationPort.canAssignCreditLimit(CARD_ID)).thenReturn(true);

        CreditLimit limit = service.execute(new AssignCreditLimitCommand(CARD_ID, Money.ofARS(500_000), 0.3,
                Money.ofARS(80_000), Money.ofARS(300_000), "admin"));

        // Los límites diario y mensual iniciales no publican updated: son parte de la asignación
        assertThat(outbox).extracting(Map.Entry::getKey).containsExactly(KafkaTopics.CREDIT_LIMIT_ASSIGNED);
        CreditLimitAssignedEvent assigned = (CreditLimitAssignedEvent) outbox.get(0).getValue();
        assertThat(assigned.getCardId()).isEqualTo(CARD_ID.getValue());
        assertThat(assigned.getCreditLimitId()).isEqualTo(limit.getId());
        assertThat(assigned.getPurchaseLimitAmount()).isEqualByComparingTo("500000");
        assertThat(assigned.getCashAdvanceLimitAmount()).isEqualByComparingTo("150000");
        assertThat(assigned.getDailyLimitAmount()).isEqualByComparingTo("80000");
        assertThat(assigned.getMonthlyLimitAmount()).isEqualByComparingTo("300000");
        assertThat(assigned.getCurrency()).isEqualTo("ARS");
        assertThat(assigned.getAssignedBy()).isEqualTo("admin");
    }

    @Test
    void rejectedAssignmentPublishesNothing() {
        when(cardValidationPort.cardExists(CARD_ID)).thenReturn(true);
        when(cardValidationPort.canAssignCreditLimit(CARD_ID)).thenReturn(true);
        when(creditLimitRepository.existsByCardId(CARD_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(new AssignCreditLimitCommand(CARD_ID, Money.ofARS(500_000), 0.3,
                null, null, "admin"))).isInstanceOf(BusinessRuleException.class);
        assertThat(outbox).isEmpty();
    }

    @Test
    void purchaseLimitChangesPublishUpdatedWithPreviousAndNewAmount() {
        CreditLimit limit = existing();

        service.increasePurchaseLimit(new IncreasePurchaseLimitCommand(limit.getId(), Money.ofARS(100_000),
                "Aumento por antigüedad", "analista"));
        service.decreasePurchaseLimit(new DecreasePurchaseLimitCommand(limit.getId(), Money.ofARS(50_000),
                "Riesgo", "analista"));

        assertThat(outbox).extracting(Map.Entry::getKey)
                .containsExactly(KafkaTopics.CREDIT_LIMIT_UPDATED, KafkaTopics.CREDIT_LIMIT_UPDATED);
        CreditLimitUpdatedEvent increase = (CreditLimitUpdatedEvent) outbox.get(0).getValue();
        assertThat(increase.getLimitType()).isEqualTo("PURCHASE");
        assertThat(increase.getPreviousAmount()).isEqualByComparingTo("500000");
        assertThat(increase.getNewAmount()).isEqualByComparingTo("600000");
        assertThat(increase.getReason()).isEqualTo("Aumento por antigüedad");
        assertThat(increase.getUpdatedBy()).isEqualTo("analista");
        assertThat(increase.getCreditLimitId()).isEqualTo(limit.getId());

        CreditLimitUpdatedEvent decrease = (CreditLimitUpdatedEvent) outbox.get(1).getValue();
        assertThat(decrease.getPreviousAmount()).isEqualByComparingTo("600000");
        assertThat(decrease.getNewAmount()).isEqualByComparingTo("550000");
    }

    @Test
    void cashAdvanceLimitChangeIsPublishedWithItsType() {
        CreditLimit limit = existing();

        service.updateCashAdvanceLimit(new UpdateCashAdvanceLimitCommand(limit.getId(), Money.ofARS(100_000),
                "Ajuste", "analista"));

        CreditLimitUpdatedEvent updated = (CreditLimitUpdatedEvent) outbox.get(0).getValue();
        assertThat(updated.getLimitType()).isEqualTo("CASH_ADVANCE");
        assertThat(updated.getNewAmount()).isEqualByComparingTo("100000");
    }

    @Test
    void changingDailyAndMonthlyLimitsPublishesOneEventEach() {
        CreditLimit limit = existing();

        service.updateTemporalLimits(new UpdateTemporalLimitsCommand(limit.getId(), Money.ofARS(50_000),
                Money.ofARS(200_000), "Viaje", "cliente"));

        assertThat(outbox).extracting(e -> ((CreditLimitUpdatedEvent) e.getValue()).getLimitType())
                .containsExactly("DAILY", "MONTHLY");
        assertThat(outbox).extracting(e -> ((CreditLimitUpdatedEvent) e.getValue()).getNewAmount())
                .usingElementComparator(java.math.BigDecimal::compareTo)
                .containsExactly(new java.math.BigDecimal("50000"), new java.math.BigDecimal("200000"));
    }

    @Test
    void limitChangesLockTheRowToAvoidLostUpdates() {
        CreditLimit limit = existing();

        service.increasePurchaseLimit(new IncreasePurchaseLimitCommand(limit.getId(), Money.ofARS(1_000), "r", "a"));
        service.decreasePurchaseLimit(new DecreasePurchaseLimitCommand(limit.getId(), Money.ofARS(1_000), "r", "a"));
        service.updateCashAdvanceLimit(new UpdateCashAdvanceLimitCommand(limit.getId(), Money.ofARS(1_000), "r", "a"));
        service.updateTemporalLimits(new UpdateTemporalLimitsCommand(limit.getId(), Money.ofARS(1_000), null, "r", "a"));

        verify(creditLimitRepository, org.mockito.Mockito.times(4)).findByIdForUpdate(limit.getId());
        verify(creditLimitRepository, never()).findById(any());
    }

    @Test
    void dailyAndMonthlyCanBeRaisedTogetherAboveThePreviousMonthly() {
        CreditLimit limit = existing(); // mensual por defecto 500.000

        service.updateTemporalLimits(new UpdateTemporalLimitsCommand(limit.getId(), Money.ofARS(600_000),
                Money.ofARS(1_000_000), "Viaje", "cliente"));

        assertThat(limit.getDailyLimit().getAmount()).isEqualTo(Money.ofARS(600_000));
        assertThat(limit.getMonthlyLimit().getAmount()).isEqualTo(Money.ofARS(1_000_000));
    }

    @Test
    void dailyAboveMonthlyIsRejectedAndPublishesNothing() {
        CreditLimit limit = existing();

        assertThatThrownBy(() -> service.updateTemporalLimits(new UpdateTemporalLimitsCommand(limit.getId(),
                Money.ofARS(600_000), null, "Viaje", "cliente")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no puede superar al mensual");
        assertThat(outbox).isEmpty();
    }

    @Test
    void assignmentWithNullCashAdvancePercentageUsesTheDefault() {
        when(cardValidationPort.cardExists(CARD_ID)).thenReturn(true);
        when(cardValidationPort.canAssignCreditLimit(CARD_ID)).thenReturn(true);

        CreditLimit limit = service.execute(new AssignCreditLimitCommand(CARD_ID, Money.ofARS(500_000), null,
                null, null, "admin"));

        assertThat(limit.getCashAdvanceLimit().getAmount()).isEqualTo(Money.ofARS(150_000));
    }

    @Test
    void assignmentWithDailyAboveMonthlyIsRejected() {
        when(cardValidationPort.cardExists(CARD_ID)).thenReturn(true);
        when(cardValidationPort.canAssignCreditLimit(CARD_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(new AssignCreditLimitCommand(CARD_ID, Money.ofARS(500_000), 0.3,
                Money.ofARS(300_000), Money.ofARS(200_000), "admin")))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(outbox).isEmpty();
    }

    private CreditLimit existing() {
        CreditLimit limit = CreditLimit.assign(CARD_ID, Money.ofARS(500_000), 0.3, "test");
        // Las entradas del historial de la asignación ya están guardadas
        limit.getHistory().forEach(h -> org.springframework.test.util.ReflectionTestUtils.setField(h, "id", 1L));
        when(creditLimitRepository.findByIdForUpdate(limit.getId())).thenReturn(Optional.of(limit));
        return limit;
    }
}
