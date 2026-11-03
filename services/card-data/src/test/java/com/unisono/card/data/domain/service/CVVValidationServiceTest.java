package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.ValidateCVVUseCase.ValidateCVVCommand;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CVV;
import com.unisono.card.shared.domain.valueobject.CardId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CVVValidationServiceTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private CardRepository cardRepository;
    @Mock
    private Card card;

    private CVVValidationService service;

    @BeforeEach
    void setUp() {
        service = new CVVValidationService(cardRepository);
        lenient().when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
        lenient().when(card.getCvv()).thenReturn(CVV.of("123"));
    }

    @Test
    void shouldAcceptMatchingCvv() {
        assertThat(service.execute(new ValidateCVVCommand(CARD_ID, "123"))).isTrue();
    }

    @Test
    void shouldRejectWrongCvv() {
        assertThat(service.execute(new ValidateCVVCommand(CARD_ID, "124"))).isFalse();
    }

    @Test
    void shouldRejectWhenCardHasNoCvv() {
        when(card.getCvv()).thenReturn(null);

        assertThat(service.execute(new ValidateCVVCommand(CARD_ID, "123"))).isFalse();
    }

    @Test
    void shouldFailOnBlockedCard() {
        when(card.isBlocked()).thenReturn(true);

        assertThatThrownBy(() -> service.execute(new ValidateCVVCommand(CARD_ID, "123")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldFailWhenCardDoesNotExist() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new ValidateCVVCommand(CARD_ID, "123")))
                .isInstanceOf(NotFoundException.class);
    }
}
