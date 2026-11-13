package com.unisono.card.payment.infrastructure.adapter.input.rest;

import com.unisono.card.payment.application.mapper.TransactionMapperImpl;
import com.unisono.card.payment.domain.exception.InvalidTransactionException;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.port.input.ReverseTransactionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.unisono.card.payment.support.TestTransactions.approvedPurchase;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReversalControllerTest {

    @Mock
    private ReverseTransactionUseCase useCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvc(new ReversalController(useCase, new TransactionMapperImpl()));
    }

    @Test
    void shouldReverseWithTheGivenReason() throws Exception {
        Transaction reversed = approvedPurchase(1_500);
        reversed.reverse("Cliente desconoce la compra");
        when(useCase.reverseTransaction(eq(reversed.getTransactionId()), eq("Cliente desconoce la compra")))
                .thenReturn(reversed);

        mockMvc.perform(post("/api/v1/reversals").contentType(MediaType.APPLICATION_JSON).content("""
                        {"transactionId":"%s","reason":"Cliente desconoce la compra"}
                        """.formatted(reversed.getTransactionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(reversed.getTransactionId().toString()))
                .andExpect(jsonPath("$.status").value("REVERSED"));
    }

    @Test
    void shouldAutoReverse() throws Exception {
        Transaction reversed = approvedPurchase(1_500);
        reversed.reverse("Automatic reversal due to timeout or system error");
        when(useCase.autoReverse(reversed.getTransactionId())).thenReturn(reversed);

        mockMvc.perform(post("/api/v1/reversals/{id}/auto", reversed.getTransactionId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERSED"));

        verify(useCase).autoReverse(reversed.getTransactionId());
    }

    @Test
    void shouldReturn400WhenTheTransactionCannotBeReversed() throws Exception {
        Transaction transaction = approvedPurchase(1_500);
        when(useCase.reverseTransaction(any(), any()))
                .thenThrow(new InvalidTransactionException(transaction.getTransactionId(), "Transaction has refunds"));

        mockMvc.perform(post("/api/v1/reversals").contentType(MediaType.APPLICATION_JSON).content("""
                        {"transactionId":"%s","reason":"x"}
                        """.formatted(transaction.getTransactionId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSACTION"));
    }

    @Test
    void shouldReturn400WithoutAReason() throws Exception {
        mockMvc.perform(post("/api/v1/reversals").contentType(MediaType.APPLICATION_JSON).content("""
                        {"transactionId":"%s"}
                        """.formatted(approvedPurchase(1).getTransactionId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("reason"));

        verifyNoInteractions(useCase);
    }
}
