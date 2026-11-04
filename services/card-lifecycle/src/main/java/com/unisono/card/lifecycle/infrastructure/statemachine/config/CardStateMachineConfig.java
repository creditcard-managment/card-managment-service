package com.unisono.card.lifecycle.infrastructure.statemachine.config;

import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.infrastructure.statemachine.guard.*;
import com.unisono.card.lifecycle.infrastructure.statemachine.listener.StateMachineEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

/**
 * Configuración de Spring State Machine para el ciclo de vida de tarjetas
 *
 * Define:
 * - Estados (States)
 * - Eventos (Events)
 * - Transiciones (Transitions)
 * - Guardas (Guards)
 */
@Slf4j
@Configuration
@EnableStateMachineFactory
@RequiredArgsConstructor
public class CardStateMachineConfig extends EnumStateMachineConfigurerAdapter<CardLifecycleState, CardLifecycleEvent> {

    // Guards
    private final ApprovalGuard approvalGuard;
    private final IssuanceGuard issuanceGuard;
    private final ActivationGuard activationGuard;
    private final BlockGuard blockGuard;

    // Sin actions: los efectos (llamadas a card-data) los ejecutan los servicios de
    // aplicación cuando la máquina acepta el evento (ver LifecycleTransitionExecutor).

    // Listener
    private final StateMachineEventListener eventListener;

    // ========================================
    // CONFIGURATION
    // ========================================

    @Override
    public void configure(StateMachineConfigurationConfigurer<CardLifecycleState, CardLifecycleEvent> config)
            throws Exception {
        config
                .withConfiguration()
                .autoStartup(false) // SpringStateMachineAdapter la posiciona y arranca por evento
                .listener(eventListener);
    }

    // ========================================
    // STATES
    // ========================================

    @Override
    public void configure(StateMachineStateConfigurer<CardLifecycleState, CardLifecycleEvent> states)
            throws Exception {
        states
                .withStates()
                .initial(CardLifecycleState.REQUESTED)
                .states(EnumSet.allOf(CardLifecycleState.class))
                .end(CardLifecycleState.CANCELLED)
                .end(CardLifecycleState.REJECTED)
                .end(CardLifecycleState.EXPIRED);
    }

    // ========================================
    // TRANSITIONS
    // ========================================

    @Override
    public void configure(StateMachineTransitionConfigurer<CardLifecycleState, CardLifecycleEvent> transitions)
            throws Exception {
        transitions
                // ========================================
                // Desde REQUESTED
                // ========================================

                // REQUESTED → APPROVED
                .withExternal()
                .source(CardLifecycleState.REQUESTED)
                .target(CardLifecycleState.APPROVED)
                .event(CardLifecycleEvent.APPROVE)
                .guard(approvalGuard)
                .and()

                // REQUESTED → REJECTED
                .withExternal()
                .source(CardLifecycleState.REQUESTED)
                .target(CardLifecycleState.REJECTED)
                .event(CardLifecycleEvent.REJECT)
                .and()

                // ========================================
                // Desde APPROVED
                // ========================================

                // APPROVED → ISSUED
                .withExternal()
                .source(CardLifecycleState.APPROVED)
                .target(CardLifecycleState.ISSUED)
                .event(CardLifecycleEvent.ISSUE)
                .guard(issuanceGuard)
                .and()

                // APPROVED → CANCELLED
                .withExternal()
                .source(CardLifecycleState.APPROVED)
                .target(CardLifecycleState.CANCELLED)
                .event(CardLifecycleEvent.CANCEL)
                .and()

                // ========================================
                // Desde ISSUED
                // ========================================

                // ISSUED → ACTIVE
                .withExternal()
                .source(CardLifecycleState.ISSUED)
                .target(CardLifecycleState.ACTIVE)
                .event(CardLifecycleEvent.ACTIVATE)
                .guard(activationGuard)
                .and()

                // ISSUED → CANCELLED
                .withExternal()
                .source(CardLifecycleState.ISSUED)
                .target(CardLifecycleState.CANCELLED)
                .event(CardLifecycleEvent.CANCEL)
                .and()

                // ========================================
                // Desde ACTIVE
                // ========================================

                // ACTIVE → BLOCKED
                .withExternal()
                .source(CardLifecycleState.ACTIVE)
                .target(CardLifecycleState.BLOCKED)
                .event(CardLifecycleEvent.BLOCK)
                .guard(blockGuard)
                .and()

                // ACTIVE → BLOCKED_FRAUD
                .withExternal()
                .source(CardLifecycleState.ACTIVE)
                .target(CardLifecycleState.BLOCKED_FRAUD)
                .event(CardLifecycleEvent.BLOCK_FRAUD)
                .guard(blockGuard)
                .and()

                // ACTIVE → EXPIRED
                .withExternal()
                .source(CardLifecycleState.ACTIVE)
                .target(CardLifecycleState.EXPIRED)
                .event(CardLifecycleEvent.EXPIRE)
                .and()

                // ACTIVE → CANCELLED
                .withExternal()
                .source(CardLifecycleState.ACTIVE)
                .target(CardLifecycleState.CANCELLED)
                .event(CardLifecycleEvent.CANCEL)
                .and()

                // ========================================
                // Desde BLOCKED
                // ========================================

                // BLOCKED → ACTIVE
                .withExternal()
                .source(CardLifecycleState.BLOCKED)
                .target(CardLifecycleState.ACTIVE)
                .event(CardLifecycleEvent.UNBLOCK)
                .and()

                // BLOCKED → CANCELLED
                .withExternal()
                .source(CardLifecycleState.BLOCKED)
                .target(CardLifecycleState.CANCELLED)
                .event(CardLifecycleEvent.CANCEL)
                .and()

                // ========================================
                // Desde BLOCKED_FRAUD
                // ========================================

                // BLOCKED_FRAUD → ACTIVE (requiere aprobación especial)
                .withExternal()
                .source(CardLifecycleState.BLOCKED_FRAUD)
                .target(CardLifecycleState.ACTIVE)
                .event(CardLifecycleEvent.UNBLOCK)
                .and()

                // BLOCKED_FRAUD → CANCELLED
                .withExternal()
                .source(CardLifecycleState.BLOCKED_FRAUD)
                .target(CardLifecycleState.CANCELLED)
                .event(CardLifecycleEvent.CANCEL);
    }
}
