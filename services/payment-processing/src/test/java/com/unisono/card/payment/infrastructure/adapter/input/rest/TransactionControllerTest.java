package com.unisono.card.payment.infrastructure.adapter.input.rest;

import com.unisono.card.payment.application.mapper.TransactionMapperImpl;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.port.input.QueryTransactionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.unisono.card.payment.support.TestTransactions.CARD_ID;
import static com.unisono.card.payment.support.TestTransactions.approvedPurchase;
import static com.unisono.card.payment.support.TestTransactions.declinedPurchase;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private QueryTransactionUseCase useCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvc(new TransactionController(useCase, new TransactionMapperImpl()));
    }

    @Test
    void shouldReturnATransactionById() throws Exception {
        Transaction transaction = approvedPurchase(1_500);
        when(useCase.findById(transaction.getTransactionId())).thenReturn(Optional.of(transaction));

        mockMvc.perform(get("/api/v1/transactions/{id}", transaction.getTransactionId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transaction.getTransactionId().toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldReturn404ForAnUnknownTransaction() throws Exception {
        TransactionId unknown = TransactionId.generate();
        when(useCase.findById(unknown)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/transactions/{id}", unknown.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TRANSACTION_NOT_FOUND"));
    }

    @Test
    void shouldReturn400ForAnInvalidTransactionId() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListTheTransactionsOfACard() throws Exception {
        when(useCase.findByCardId(CARD_ID)).thenReturn(List.of(approvedPurchase(1_000), approvedPurchase(2_000)));

        mockMvc.perform(get("/api/v1/transactions/card/{cardId}", CARD_ID.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldFilterByStatus() throws Exception {
        when(useCase.findByCardIdAndStatus(CARD_ID, TransactionStatus.DECLINED))
                .thenReturn(List.of(declinedPurchase(1_000, "51")));

        mockMvc.perform(get("/api/v1/transactions/card/{cardId}/status/{status}", CARD_ID.getValue(), "DECLINED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DECLINED"));
    }

    @Test
    void shouldFilterByDateRange() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 11, 30, 23, 59, 59);
        when(useCase.findByCardIdAndDateRange(CARD_ID, from, to)).thenReturn(List.of(approvedPurchase(1_000)));

        mockMvc.perform(get("/api/v1/transactions/card/{cardId}/range", CARD_ID.getValue())
                        .param("fromDate", "2026-11-01T00:00:00")
                        .param("toDate", "2026-11-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(useCase).findByCardIdAndDateRange(CARD_ID, from, to);
    }

    @Test
    void shouldSummarizeTheTransactionsOfACard() throws Exception {
        when(useCase.findByCardId(CARD_ID)).thenReturn(List.of(approvedPurchase(1_000), declinedPurchase(500, "51")));

        mockMvc.perform(get("/api/v1/transactions/card/{cardId}/summary", CARD_ID.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(2))
                .andExpect(jsonPath("$.approvedCount").value(1))
                .andExpect(jsonPath("$.declinedCount").value(1))
                .andExpect(jsonPath("$.currency").value("ARS"));
    }

    @Test
    void shouldListThePendingSettlement() throws Exception {
        when(useCase.findPendingSettlement()).thenReturn(List.of(approvedPurchase(1_000)));

        mockMvc.perform(get("/api/v1/transactions/pending-settlement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }
}
