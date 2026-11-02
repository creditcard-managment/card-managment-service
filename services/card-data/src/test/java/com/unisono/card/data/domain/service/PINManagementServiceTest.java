package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.model.CardCategory;
import com.unisono.card.data.domain.model.CardHolder;
import com.unisono.card.data.domain.model.CardType;
import com.unisono.card.data.domain.port.input.ResetPINUseCase.ResetPINCommand;
import com.unisono.card.data.domain.port.input.ValidatePINUseCase.ValidatePINCommand;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.data.domain.port.output.EncryptionPort;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.domain.valueobject.ClientId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PINManagementServiceTest {

    private static final String PIN_HASH = "$2a$10$storedhash";
    private static final String CORRECT_PIN = "4821";
    private static final String WRONG_PIN = "1357";

    @Mock
    private CardRepository cardRepository;

    @Mock
    private EncryptionPort encryptionPort;

    private PINManagementService service;
    private Card card;

    @BeforeEach
    void setUp() {
        service = new PINManagementService(cardRepository, encryptionPort);
        card = activeCardWithPin();
        lenient().when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        lenient().when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(encryptionPort.verifyHash(eq(CORRECT_PIN), eq(PIN_HASH))).thenReturn(true);
        lenient().when(encryptionPort.verifyHash(eq(WRONG_PIN), anyString())).thenReturn(false);
    }

    @Test
    void shouldBlockCardAfterThreeWrongPins() {
        for (int i = 0; i < Card.MAX_PIN_ATTEMPTS; i++) {
            assertThat(service.execute(new ValidatePINCommand(card.getId(), WRONG_PIN))).isFalse();
        }

        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(cardRepository, times(Card.MAX_PIN_ATTEMPTS)).save(card);
    }

    @Test
    void shouldResetCounterOnCorrectPin() {
        service.execute(new ValidatePINCommand(card.getId(), WRONG_PIN));
        service.execute(new ValidatePINCommand(card.getId(), WRONG_PIN));

        assertThat(service.execute(new ValidatePINCommand(card.getId(), CORRECT_PIN))).isTrue();

        assertThat(card.getPinFailedAttempts()).isZero();
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void shouldRejectPinValidationOnBlockedCard() {
        card.block("Pérdida");

        assertThatThrownBy(() -> service.execute(new ValidatePINCommand(card.getId(), CORRECT_PIN)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("bloqueada");
        verify(encryptionPort, never()).verifyHash(anyString(), anyString());
    }

    @Test
    void shouldRegisterAttemptWhenResetUsesWrongCurrentPin() {
        assertThatThrownBy(() -> service.execute(new ResetPINCommand(card.getId(), "9073", WRONG_PIN)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Intentos restantes: 2");

        // El intento se guarda antes de lanzar la excepción (noRollbackFor en el servicio)
        assertThat(card.getPinFailedAttempts()).isEqualTo(1);
        verify(cardRepository).save(card);
    }

    @Test
    void shouldBlockCardWhenResetFailsThreeTimes() {
        for (int i = 0; i < Card.MAX_PIN_ATTEMPTS - 1; i++) {
            assertThatThrownBy(() -> service.execute(new ResetPINCommand(card.getId(), "9073", WRONG_PIN)))
                    .isInstanceOf(BusinessRuleException.class);
        }

        assertThatThrownBy(() -> service.execute(new ResetPINCommand(card.getId(), "9073", WRONG_PIN)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("bloqueada por exceso de intentos");
        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    void shouldResetPinWithCorrectCurrentPin() {
        when(encryptionPort.hash("9073")).thenReturn("$2a$10$newhash");
        service.execute(new ValidatePINCommand(card.getId(), WRONG_PIN));

        Card updated = service.execute(new ResetPINCommand(card.getId(), "9073", CORRECT_PIN));

        assertThat(updated.getPinHash()).isEqualTo("$2a$10$newhash");
        assertThat(updated.getPinFailedAttempts()).isZero();
    }

    private static Card activeCardWithPin() {
        CardHolder holder = CardHolder.create(
                "Juan",
                "Pérez",
                "12345678",
                "juan@example.com",
                "+5491123456789",
                LocalDate.of(1990, 1, 1)
        );
        Card card = Card.create(ClientId.of("12345678"), holder, CardType.VISA, CardCategory.CLASSIC);
        card.updateStatus(CardStatus.APPROVED);
        card.updateStatus(CardStatus.ISSUED);
        card.updateStatus(CardStatus.ACTIVE);
        card.updatePIN(PIN_HASH);
        return card;
    }
}
