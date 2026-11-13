package com.unisono.card.payment.infrastructure.adapter.input.rest;

import com.unisono.card.payment.application.mapper.RefundMapperImpl;
import com.unisono.card.payment.domain.exception.InvalidTransactionException;
import com.unisono.card.payment.domain.exception.TransactionNotFoundException;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.port.input.ProcessRefundUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.unisono.card.payment.support.TestTransactions.CARD_ID;
import static com.unisono.card.payment.support.TestTransactions.approvedRefund;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RefundControllerTest {

    private static final TransactionId ORIGINAL_ID = TransactionId.generate();

    @Mock
    private ProcessRefundUseCase useCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvc(new RefundController(useCase, new RefundMapperImpl()));
    }

    @Test
    void shouldReturn201WithTheFullRefundLinkedToTheOriginal() throws Exception {
        Transaction refund = approvedRefund(10_000, ORIGINAL_ID);
        when(useCase.processFullRefund(any())).thenReturn(refund);

        refund("/full", "10000.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalTransactionId").value(ORIGINAL_ID.toString()))
                .andExpect(jsonPath("$.cardId").value(CARD_ID.getValue()))
                .andExpect(jsonPath("$.refundAmount").value(10000.00))
                .andExpect(jsonPath("$.currency").value("ARS"));
    }

    @Test
    void shouldReturn201WithThePartialRefund() throws Exception {
        when(useCase.processPartialRefund(any())).thenReturn(approvedRefund(2_500, ORIGINAL_ID));

        refund("/partial", "2500.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refundAmount").value(2500.00));
    }

    @Test
    void shouldReturn400WhenTheRefundExceedsWhatIsLeft() throws Exception {
        when(useCase.processPartialRefund(any()))
                .thenThrow(new InvalidTransactionException(ORIGINAL_ID, "Refund amount exceeds the amount not yet refunded"));

        refund("/partial", "99999.00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSACTION"));
    }

    @Test
    void shouldReturn404WhenTheOriginalDoesNotExist() throws Exception {
        when(useCase.processFullRefund(any())).thenThrow(new TransactionNotFoundException(ORIGINAL_ID));

        refund("/full", "10000.00")
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/v1/refunds/full").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").isNotEmpty());

        verifyNoInteractions(useCase);
    }

    private ResultActions refund(String path, String amount) throws Exception {
        return mockMvc.perform(post("/api/v1/refunds" + path).contentType(MediaType.APPLICATION_JSON).content("""
                {"originalTransactionId":"%s","cardId":"%s","refundAmount":%s,"currency":"ARS",
                 "merchantId":"MERCH-001","reason":"Producto devuelto"}
                """.formatted(ORIGINAL_ID, CARD_ID.getValue(), amount)));
    }
}
