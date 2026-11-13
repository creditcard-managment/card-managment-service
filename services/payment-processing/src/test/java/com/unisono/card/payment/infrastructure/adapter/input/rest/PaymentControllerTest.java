package com.unisono.card.payment.infrastructure.adapter.input.rest;

import com.unisono.card.payment.application.dto.request.ProcessCashAdvanceCommand;
import com.unisono.card.payment.application.dto.request.ProcessPaymentCommand;
import com.unisono.card.payment.application.mapper.PaymentMapperImpl;
import com.unisono.card.payment.domain.exception.ExternalServiceException;
import com.unisono.card.payment.domain.exception.TransactionNotFoundException;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.port.input.ProcessPaymentUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.unisono.card.payment.support.TestTransactions.CARD_ID;
import static com.unisono.card.payment.support.TestTransactions.approvedPurchase;
import static com.unisono.card.payment.support.TestTransactions.authorizedPurchase;
import static com.unisono.card.payment.support.TestTransactions.declinedPurchase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private static final String PIN_BLOCK = "22A82BAE1D1C1E16F1502C988540787E";

    @Mock
    private ProcessPaymentUseCase useCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvc(new PaymentController(useCase, new PaymentMapperImpl()));
    }

    @Test
    void shouldReturn201WithTheApprovedPurchase() throws Exception {
        Transaction approved = approvedPurchase(1_500);
        when(useCase.processPurchase(any())).thenReturn(approved);

        post("/api/v1/payments/purchase", purchaseJson("1500.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(approved.getTransactionId().toString()))
                .andExpect(jsonPath("$.cardId").value(CARD_ID.getValue()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.currency").value("ARS"))
                .andExpect(jsonPath("$.receiptNumber").doesNotExist());

        ArgumentCaptor<ProcessPaymentCommand> command = ArgumentCaptor.forClass(ProcessPaymentCommand.class);
        verify(useCase).processPurchase(command.capture());
        assertThat(command.getValue().getMerchantId()).isEqualTo("MERCH-001");
    }

    @Test
    void shouldReturn402WithTheDeclinedPurchaseAndItsResponseCode() throws Exception {
        when(useCase.processPurchase(any())).thenReturn(declinedPurchase(1_500, "51"));

        post("/api/v1/payments/purchase", purchaseJson("1500.00"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.approved").value(false))
                .andExpect(jsonPath("$.responseCode").value("51"))
                .andExpect(jsonPath("$.declineReason").value("Declined in test"));
    }

    @Test
    void shouldReturn400WhenThePurchaseIsInvalid() throws Exception {
        post("/api/v1/payments/purchase", purchaseJson("60000.00").replace("\"MERCH-001\"", "\"\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[*].field").value(
                        org.hamcrest.Matchers.containsInAnyOrder("amount", "merchantId")));

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldReturn503WhenADependencyIsDown() throws Exception {
        when(useCase.processPurchase(any())).thenThrow(new ExternalServiceException("card-limits", "timeout", null));

        post("/api/v1/payments/purchase", purchaseJson("1500.00"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    void shouldPassThePinBlockOfACashAdvance() throws Exception {
        when(useCase.processCashAdvance(any())).thenReturn(approvedPurchase(2_000));

        post("/api/v1/payments/cash-advance", cashAdvanceJson("\"pinBlock\":\"" + PIN_BLOCK + "\",\"pinKeyId\":\"pek-v1\""))
                .andExpect(status().isCreated());

        ArgumentCaptor<ProcessCashAdvanceCommand> command = ArgumentCaptor.forClass(ProcessCashAdvanceCommand.class);
        verify(useCase).processCashAdvance(command.capture());
        assertThat(command.getValue().getPinBlock()).isEqualTo(PIN_BLOCK);
        assertThat(command.getValue().getPinKeyId()).isEqualTo("pek-v1");
    }

    @Test
    void shouldRejectACashAdvanceWithAPlainPin() throws Exception {
        post("/api/v1/payments/cash-advance", cashAdvanceJson("\"pin\":\"1234\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[*].field").value(
                        org.hamcrest.Matchers.containsInAnyOrder("pinBlock", "pinKeyId")));

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectAMalformedPinBlock() throws Exception {
        post("/api/v1/payments/cash-advance", cashAdvanceJson("\"pinBlock\":\"1234\",\"pinKeyId\":\"pek-v1\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("pinBlock"));

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldReturn201ForAPreAuthorizationThatStaysAuthorized() throws Exception {
        when(useCase.preAuthorize(any())).thenReturn(authorizedPurchase(3_000));

        post("/api/v1/payments/pre-authorize", purchaseJson("3000.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    void shouldCaptureAPreAuthorization() throws Exception {
        Transaction captured = approvedPurchase(3_000);
        when(useCase.captureTransaction(captured.getTransactionId())).thenReturn(captured);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/payments/{id}/capture", captured.getTransactionId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldReturn404WhenCapturingAnUnknownTransaction() throws Exception {
        Transaction unknown = approvedPurchase(1);
        when(useCase.captureTransaction(unknown.getTransactionId()))
                .thenThrow(new TransactionNotFoundException(unknown.getTransactionId()));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/payments/{id}/capture", unknown.getTransactionId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TRANSACTION_NOT_FOUND"));
    }

    private ResultActions post(String url, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String purchaseJson(String amount) {
        return """
                {"cardId":"%s","cardNumberToken":"tok_123","lastFourDigits":"0366","expirationDate":"12/2030",
                 "amount":%s,"currency":"ARS","merchantId":"MERCH-001","merchantName":"Supermercado Uno",
                 "paymentMethod":"CHIP"}
                """.formatted(CARD_ID.getValue(), amount);
    }

    private static String cashAdvanceJson(String pinFields) {
        return """
                {"cardId":"%s","cardNumberToken":"tok_123","lastFourDigits":"0366","expirationDate":"12/2030",
                 "amount":2000.00,"currency":"ARS","terminalId":"ATM-01",%s}
                """.formatted(CARD_ID.getValue(), pinFields);
    }
}
