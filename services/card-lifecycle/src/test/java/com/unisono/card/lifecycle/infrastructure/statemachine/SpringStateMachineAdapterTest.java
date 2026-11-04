package com.unisono.card.lifecycle.infrastructure.statemachine;

import com.unisono.card.lifecycle.domain.exception.InvalidStateTransitionException;
import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.TransitionContext;
import com.unisono.card.lifecycle.domain.port.output.CreditLimitPort;
import com.unisono.card.lifecycle.infrastructure.statemachine.config.CardStateMachineConfig;
import com.unisono.card.lifecycle.infrastructure.statemachine.guard.ActivationGuard;
import com.unisono.card.lifecycle.infrastructure.statemachine.guard.ApprovalGuard;
import com.unisono.card.lifecycle.infrastructure.statemachine.guard.BlockGuard;
import com.unisono.card.lifecycle.infrastructure.statemachine.guard.IssuanceGuard;
import com.unisono.card.lifecycle.infrastructure.statemachine.listener.StateMachineEventListener;
import com.unisono.card.shared.domain.valueobject.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.YearMonth;
import java.util.Map;

import static com.unisono.card.lifecycle.domain.model.TransitionContext.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Ejecuta la state machine real (config + guards) sin base de datos ni card-data.
 */
@SpringJUnitConfig(classes = {
        CardStateMachineConfig.class,
        SpringStateMachineAdapter.class,
        ApprovalGuard.class, IssuanceGuard.class, ActivationGuard.class, BlockGuard.class,
        StateMachineEventListener.class
})
class SpringStateMachineAdapterTest {

    private static final CardId CARD_ID = CardId.of("be546553-f910-4891-8881-e83e1e7f488f");

    @Autowired
    private SpringStateMachineAdapter adapter;

    @MockBean
    private CreditLimitPort creditLimitPort;

    private final TransitionContext context = TransitionContext.of("motivo", "admin@bank.com");

    @Test
    void approvesFromRequested() {
        CardLifecycleState target = adapter.fire(CARD_ID, CardLifecycleState.REQUESTED, CardLifecycleEvent.APPROVE, context);

        assertThat(target).isEqualTo(CardLifecycleState.APPROVED);
    }

    @Test
    void rejectsEventWithoutTransitionFromCurrentState() {
        assertThatThrownBy(() ->
                adapter.fire(CARD_ID, CardLifecycleState.ACTIVE, CardLifecycleEvent.APPROVE, context))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Aprobar")
                .hasMessageContaining("Activa");
    }

    @Test
    void approvalGuardRequiresReason() {
        TransitionContext withoutReason = TransitionContext.of(" ", "admin@bank.com");

        assertThatThrownBy(() ->
                adapter.fire(CARD_ID, CardLifecycleState.REQUESTED, CardLifecycleEvent.APPROVE, withoutReason))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("La aprobación requiere una razón");
    }

    @Test
    void activatesWhenCardHasCreditLimit() {
        when(creditLimitPort.canActivate(any())).thenReturn(true);

        CardLifecycleState target = adapter.fire(CARD_ID, CardLifecycleState.ISSUED, CardLifecycleEvent.ACTIVATE, context);

        assertThat(target).isEqualTo(CardLifecycleState.ACTIVE);
    }

    @Test
    void activationGuardRejectsCardWithoutCreditLimit() {
        when(creditLimitPort.canActivate(any())).thenReturn(false);

        assertThatThrownBy(() ->
                adapter.fire(CARD_ID, CardLifecycleState.ISSUED, CardLifecycleEvent.ACTIVATE, context))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("límite de crédito");
    }

    @Test
    void issuesWithCompleteData() {
        YearMonth exp = YearMonth.now().plusYears(3);
        TransitionContext issuance = TransitionContext.of("emisión", "system", Map.of(
                CARD_NUMBER, CardNumber.of("4532015112830366"),
                CVV, com.unisono.card.shared.domain.valueobject.CVV.of("123"),
                PIN, com.unisono.card.shared.domain.valueobject.PIN.of("4821"),
                EXPIRATION_DATE, ExpirationDate.forIssuance(exp.getMonthValue(), exp.getYear())));

        CardLifecycleState target = adapter.fire(CARD_ID, CardLifecycleState.APPROVED, CardLifecycleEvent.ISSUE, issuance);

        assertThat(target).isEqualTo(CardLifecycleState.ISSUED);
    }

    @Test
    void blocksActiveCardWithReason() {
        assertThat(adapter.fire(CARD_ID, CardLifecycleState.ACTIVE, CardLifecycleEvent.BLOCK_FRAUD, context))
                .isEqualTo(CardLifecycleState.BLOCKED_FRAUD);
    }

    @Test
    void blockGuardRequiresReason() {
        TransitionContext withoutReason = TransitionContext.of(null, "admin@bank.com");

        assertThatThrownBy(() ->
                adapter.fire(CARD_ID, CardLifecycleState.ACTIVE, CardLifecycleEvent.BLOCK, withoutReason))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("El bloqueo requiere una razón");
    }

    @Test
    void cancelsFromBlockedButNotFromRequested() {
        assertThat(adapter.fire(CARD_ID, CardLifecycleState.BLOCKED, CardLifecycleEvent.CANCEL, context))
                .isEqualTo(CardLifecycleState.CANCELLED);

        assertThatThrownBy(() ->
                adapter.fire(CARD_ID, CardLifecycleState.REQUESTED, CardLifecycleEvent.CANCEL, context))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void rejectsOnlyFromRequested() {
        assertThat(adapter.fire(CARD_ID, CardLifecycleState.REQUESTED, CardLifecycleEvent.REJECT, context))
                .isEqualTo(CardLifecycleState.REJECTED);
    }

    @Test
    void issuanceGuardRejectsMissingData() {
        assertThatThrownBy(() ->
                adapter.fire(CARD_ID, CardLifecycleState.APPROVED, CardLifecycleEvent.ISSUE, context))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("La emisión requiere el número de tarjeta");
    }
}
