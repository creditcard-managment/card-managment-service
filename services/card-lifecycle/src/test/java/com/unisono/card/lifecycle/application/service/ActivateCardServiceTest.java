package com.unisono.card.lifecycle.application.service;

import com.unisono.card.lifecycle.domain.exception.CardDataIntegrationException;
import com.unisono.card.lifecycle.domain.exception.InvalidStateTransitionException;
import com.unisono.card.lifecycle.domain.model.CardLifecycleEvent;
import com.unisono.card.lifecycle.domain.model.CardLifecycleState;
import com.unisono.card.lifecycle.domain.model.StateTransition;
import com.unisono.card.lifecycle.domain.port.input.ActivateCardUseCase.ActivateCardCommand;
import com.unisono.card.lifecycle.domain.port.output.CardDataClient;
import com.unisono.card.lifecycle.domain.port.output.LifecycleStateMachinePort;
import com.unisono.card.lifecycle.domain.port.output.StateTransitionRepository;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.domain.valueobject.PIN;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivateCardServiceTest {

    private static final CardId CARD_ID = CardId.of("be546553-f910-4891-8881-e83e1e7f488f");
    private static final PIN PIN_OK = PIN.of("4821");

    @Mock
    private CardDataClient cardDataClient;

    @Mock
    private LifecycleStateMachinePort stateMachine;

    @Mock
    private StateTransitionRepository transitionRepository;

    private ActivateCardService service;

    private final ActivateCardCommand command =
            new ActivateCardCommand(CARD_ID, PIN_OK, "Activación vía app", "cliente@email.com");

    @BeforeEach
    void setUp() {
        LifecycleTransitionExecutor executor =
                new LifecycleTransitionExecutor(cardDataClient, stateMachine, transitionRepository);
        service = new ActivateCardService(cardDataClient, executor);
        lenient().when(transitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void activatesAndRecordsRealFromState() {
        when(cardDataClient.getStatus(CARD_ID)).thenReturn(CardStatus.ISSUED);
        when(stateMachine.fire(eq(CARD_ID), eq(CardLifecycleState.ISSUED), eq(CardLifecycleEvent.ACTIVATE), any()))
                .thenReturn(CardLifecycleState.ACTIVE);
        when(cardDataClient.validatePin(CARD_ID, PIN_OK)).thenReturn(true);

        StateTransition result = service.execute(command);

        verify(cardDataClient).updateStatus(CARD_ID, "ACTIVE", "Activación vía app", "cliente@email.com");
        verify(cardDataClient).updateActivatedAt(eq(CARD_ID), any());
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getFromState()).isEqualTo("ISSUED");
        assertThat(result.getToState()).isEqualTo("ACTIVE");
        assertThat(result.getPerformedBy()).isEqualTo("cliente@email.com");
    }

    @Test
    void wrongPinAbortsActivationAndRecordsFailure() {
        when(cardDataClient.getStatus(CARD_ID)).thenReturn(CardStatus.ISSUED);
        when(stateMachine.fire(any(), any(), any(), any())).thenReturn(CardLifecycleState.ACTIVE);
        when(cardDataClient.validatePin(CARD_ID, PIN_OK)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PIN incorrecto");

        verify(cardDataClient, never()).updateStatus(any(), any(), any(), any());
        verify(cardDataClient, never()).updateActivatedAt(any(), any());
        StateTransition failed = capturedSave();
        assertThat(failed.isFailed()).isTrue();
        assertThat(failed.getFromState()).isEqualTo("ISSUED");
        assertThat(failed.getToState()).isEqualTo("ACTIVE");
        assertThat(failed.getErrorMessage()).contains("PIN incorrecto");
    }

    @Test
    void rejectedByStateMachineDoesNotTouchCardDataNorPin() {
        when(cardDataClient.getStatus(CARD_ID)).thenReturn(CardStatus.ISSUED);
        when(stateMachine.fire(any(), any(), any(), any()))
                .thenThrow(new InvalidStateTransitionException("sin límite de crédito"));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(cardDataClient, never()).validatePin(any(), any());
        verify(cardDataClient, never()).updateStatus(any(), any(), any(), any());
        assertThat(capturedSave().getErrorMessage()).isEqualTo("sin límite de crédito");
    }

    @Test
    void cardDataRejectionIsRecordedAsFailedTransition() {
        when(cardDataClient.getStatus(CARD_ID)).thenReturn(CardStatus.ISSUED);
        when(stateMachine.fire(any(), any(), any(), any())).thenReturn(CardLifecycleState.ACTIVE);
        when(cardDataClient.validatePin(CARD_ID, PIN_OK)).thenReturn(true);
        doThrow(new BusinessRuleException("Transición no permitida")).when(cardDataClient)
                .updateStatus(CARD_ID, "ACTIVE", "Activación vía app", "cliente@email.com");

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class);

        assertThat(capturedSave().isFailed()).isTrue();
    }

    @Test
    void unknownCardIsNotRecorded() {
        when(cardDataClient.getStatus(CARD_ID)).thenThrow(new NotFoundException("Card", CARD_ID.getValue()));

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(NotFoundException.class);

        verifyNoInteractions(stateMachine, transitionRepository);
    }

    @Test
    void auditFailureDoesNotHideOriginalError() {
        when(cardDataClient.getStatus(CARD_ID))
                .thenThrow(new CardDataIntegrationException("card-data caído", null));
        when(transitionRepository.save(any())).thenThrow(new RuntimeException("DB caída"));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(CardDataIntegrationException.class)
                .hasMessage("card-data caído");
    }

    private StateTransition capturedSave() {
        ArgumentCaptor<StateTransition> captor = ArgumentCaptor.forClass(StateTransition.class);
        verify(transitionRepository).save(captor.capture());
        return captor.getValue();
    }
}
