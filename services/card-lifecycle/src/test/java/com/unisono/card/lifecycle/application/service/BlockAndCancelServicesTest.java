package com.unisono.card.lifecycle.application.service;

import com.unisono.card.lifecycle.domain.exception.InvalidStateTransitionException;
import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.port.input.BlockCardUseCase.BlockCardCommand;
import com.unisono.card.lifecycle.domain.port.input.CancelCardUseCase.CancelCardCommand;
import com.unisono.card.lifecycle.domain.port.input.RejectCardUseCase.RejectCardCommand;
import com.unisono.card.lifecycle.domain.port.output.CardDataClient;
import com.unisono.card.lifecycle.domain.port.output.LifecycleStateMachinePort;
import com.unisono.card.lifecycle.domain.port.output.StateTransitionRepository;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockAndCancelServicesTest {

    private static final CardId CARD_ID = CardId.of("be546553-f910-4891-8881-e83e1e7f488f");

    @Mock
    private CardDataClient cardDataClient;

    @Mock
    private LifecycleStateMachinePort stateMachine;

    @Mock
    private StateTransitionRepository transitionRepository;

    private BlockCardService blockService;
    private CancelCardService cancelService;
    private RejectCardService rejectService;

    @BeforeEach
    void setUp() {
        LifecycleTransitionExecutor executor =
                new LifecycleTransitionExecutor(cardDataClient, stateMachine, transitionRepository);
        blockService = new BlockCardService(cardDataClient, executor);
        cancelService = new CancelCardService(cardDataClient, executor);
        rejectService = new RejectCardService(cardDataClient, executor);
        lenient().when(transitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void blockGoesThroughCardDataBlockEndpoint() {
        when(cardDataClient.getStatus(CARD_ID)).thenReturn(CardStatus.ACTIVE);
        when(stateMachine.fire(eq(CARD_ID), eq(CardLifecycleState.ACTIVE), eq(CardLifecycleEvent.BLOCK), any()))
                .thenReturn(CardLifecycleState.BLOCKED);

        StateTransition result = blockService.executeBlock(new BlockCardCommand(CARD_ID, "Pérdida", "usuario"));

        verify(cardDataClient).block(CARD_ID, "Pérdida", "usuario");
        verify(cardDataClient, never()).updateStatus(any(), any(), any(), any());
        assertThat(result.getFromState()).isEqualTo("ACTIVE");
        assertThat(result.getToState()).isEqualTo("BLOCKED");
        assertThat(result.getReason()).isEqualTo("Pérdida");
    }

    @Test
    void fraudBlockUsesFraudEndpoint() {
        when(cardDataClient.getStatus(CARD_ID)).thenReturn(CardStatus.ACTIVE);
        when(stateMachine.fire(any(), any(), eq(CardLifecycleEvent.BLOCK_FRAUD), any()))
                .thenReturn(CardLifecycleState.BLOCKED_FRAUD);

        StateTransition result = blockService.executeBlockFraud(new BlockCardCommand(CARD_ID, "Fraude", "antifraude"));

        verify(cardDataClient).blockForFraud(CARD_ID, "Fraude", "antifraude");
        assertThat(result.getToState()).isEqualTo("BLOCKED_FRAUD");
    }

    @Test
    void invalidBlockIsRecordedWithoutCallingCardData() {
        when(cardDataClient.getStatus(CARD_ID)).thenReturn(CardStatus.REQUESTED);
        when(stateMachine.fire(any(), any(), any(), any()))
                .thenThrow(new InvalidStateTransitionException("no se puede bloquear"));

        assertThatThrownBy(() -> blockService.executeBlock(new BlockCardCommand(CARD_ID, "x", "u")))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(cardDataClient, never()).block(any(), any(), any());
        StateTransition failed = captured();
        assertThat(failed.isFailed()).isTrue();
        assertThat(failed.getFromState()).isEqualTo("REQUESTED");
        assertThat(failed.getToState()).isEqualTo("BLOCKED");
    }

    @Test
    void cancelGoesThroughCardDataCancelEndpoint() {
        when(cardDataClient.getStatus(CARD_ID)).thenReturn(CardStatus.ISSUED);
        when(stateMachine.fire(any(), eq(CardLifecycleState.ISSUED), eq(CardLifecycleEvent.CANCEL), any()))
                .thenReturn(CardLifecycleState.CANCELLED);

        StateTransition result = cancelService.execute(new CancelCardCommand(CARD_ID, "Baja", "cliente"));

        verify(cardDataClient).cancel(CARD_ID, "Baja", "cliente");
        assertThat(result.getFromState()).isEqualTo("ISSUED");
        assertThat(result.getToState()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelRejectedByCardDataIsRecorded() {
        when(cardDataClient.getStatus(CARD_ID)).thenReturn(CardStatus.ACTIVE);
        when(stateMachine.fire(any(), any(), any(), any())).thenReturn(CardLifecycleState.CANCELLED);
        doThrow(new BusinessRuleException("Transición de estado inválida")).when(cardDataClient).cancel(CARD_ID, "Baja", "cliente");

        assertThatThrownBy(() -> cancelService.execute(new CancelCardCommand(CARD_ID, "Baja", "cliente")))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(captured().getErrorMessage()).isEqualTo("Transición de estado inválida");
    }

    @Test
    void rejectUpdatesStatusInCardData() {
        when(cardDataClient.getStatus(CARD_ID)).thenReturn(CardStatus.REQUESTED);
        when(stateMachine.fire(any(), any(), eq(CardLifecycleEvent.REJECT), any()))
                .thenReturn(CardLifecycleState.REJECTED);

        StateTransition transition = rejectService.execute(new RejectCardCommand(CARD_ID, "No califica", "admin"));

        verify(cardDataClient).updateStatus(CARD_ID, "REJECTED", "No califica", "admin");
        assertThat(transition.getFromState()).isEqualTo("REQUESTED");
        assertThat(transition.getToState()).isEqualTo("REJECTED");
        assertThat(transition.isSuccessful()).isTrue();
        // La fila exitosa la registra el consumidor de card.rejected (CardStatusEventConsumer), no el servicio
        verify(transitionRepository, never()).save(any());
    }

    private StateTransition captured() {
        ArgumentCaptor<StateTransition> captor = ArgumentCaptor.forClass(StateTransition.class);
        verify(transitionRepository).save(captor.capture());
        return captor.getValue();
    }
}
