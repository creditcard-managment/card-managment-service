package com.unisono.card.payment.infrastructure.adapter.input.rest;

import com.unisono.card.payment.application.mapper.SettlementMapperImpl;
import com.unisono.card.payment.application.mapper.TransactionMapperImpl;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.port.input.SettleTransactionsUseCase;
import com.unisono.card.payment.domain.port.input.SettleTransactionsUseCase.BatchSettlementResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.unisono.card.payment.support.TestTransactions.approvedPurchase;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SettlementControllerTest {

    @Mock
    private SettleTransactionsUseCase useCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvc(
                new SettlementController(useCase, new TransactionMapperImpl(), new SettlementMapperImpl()));
    }

    @Test
    void shouldSettleOneTransaction() throws Exception {
        Transaction settled = settled(1_500);
        when(useCase.settleTransaction(settled.getTransactionId())).thenReturn(settled);

        mockMvc.perform(post("/api/v1/settlements/transaction/{id}", settled.getTransactionId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"));
    }

    @Test
    void shouldReportAPartialBatch() throws Exception {
        Transaction settled = settled(1_500);
        TransactionId failed = TransactionId.generate();
        when(useCase.settleBatch(List.of(settled.getTransactionId(), failed)))
                .thenReturn(new BatchSettlementResult(List.of(settled), List.of(failed)));

        mockMvc.perform(post("/api/v1/settlements/batch").contentType(MediaType.APPLICATION_JSON).content("""
                        {"transactionIds":["%s","%s"]}
                        """.formatted(settled.getTransactionId(), failed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.totalTransactions").value(2))
                .andExpect(jsonPath("$.settledCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.failedTransactionIds[0]").value(failed.toString()))
                .andExpect(jsonPath("$.totalAmount").value(1500.00))
                .andExpect(jsonPath("$.currency").value("ARS"));
    }

    @Test
    void shouldReturn400ForAnEmptyBatch() throws Exception {
        mockMvc.perform(post("/api/v1/settlements/batch").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionIds\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldSettleAllPendingAndReportARSWhenThereIsNothing() throws Exception {
        when(useCase.settleAllPending()).thenReturn(new BatchSettlementResult(List.of(), List.of()));

        mockMvc.perform(post("/api/v1/settlements/all-pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalTransactions").value(0))
                .andExpect(jsonPath("$.currency").value("ARS"));
    }

    private static Transaction settled(long amount) {
        Transaction transaction = approvedPurchase(amount);
        transaction.settle();
        return transaction;
    }
}
