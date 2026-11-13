package com.unisono.card.payment.infrastructure.adapter.output.creditlimit;

import com.unisono.card.payment.domain.exception.ExternalServiceException;
import com.unisono.card.shared.domain.valueobject.CardId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditOperationCompensatorTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private CreditLimitAdapter adapter;
    @Mock
    private CreditOperationJournal journal;
    @InjectMocks
    private CreditOperationCompensator compensator;

    @Test
    void shouldCancelInCardLimitsAndMarkCompensated() {
        when(adapter.cancelOperation("op-1", CARD_ID)).thenReturn("CANCELLED");

        compensator.compensate("op-1", CARD_ID);

        verify(journal).markCompensated("op-1");
    }

    @Test
    void shouldRecordTheFailureAndNotThrowWhenCardLimitsIsDown() {
        when(adapter.cancelOperation("op-1", CARD_ID))
                .thenThrow(new ExternalServiceException("card-limits", "Connection refused", null));

        compensator.compensate("op-1", CARD_ID);

        verify(journal, never()).markCompensated(anyString());
        verify(journal).markCompensationFailed(eq("op-1"), contains("Connection refused"));
    }
}
