package com.unisono.card.limits.infrastructure.adapter.input.rest;

import com.unisono.card.limits.application.dto.request.AssignCreditLimitRequest;
import com.unisono.card.limits.application.dto.request.ConsumeCreditRequest;
import com.unisono.card.limits.application.dto.request.ReleaseCreditRequest;
import com.unisono.card.limits.application.dto.request.RestoreCreditRequest;
import com.unisono.card.limits.application.dto.request.UpdateCreditLimitRequest;
import com.unisono.card.limits.application.dto.request.UpdateTemporalLimitsRequest;
import com.unisono.card.limits.application.dto.response.CreditInfoResponse;
import com.unisono.card.limits.application.dto.response.CreditLimitHistoryResponse;
import com.unisono.card.limits.application.dto.response.CreditLimitResponse;
import com.unisono.card.limits.application.mapper.CreditLimitDtoMapper;
import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.limits.domain.model.CreditLimitHistory;
import com.unisono.card.limits.domain.port.input.AssignCreditLimitUseCase;
import com.unisono.card.limits.domain.port.input.AssignCreditLimitUseCase.AssignCreditLimitCommand;
import com.unisono.card.limits.domain.port.input.GetCreditInfoUseCase;
import com.unisono.card.limits.domain.port.input.RecalculateAvailableCreditUseCase;
import com.unisono.card.limits.domain.port.input.RecalculateAvailableCreditUseCase.ConsumeCreditCommand;
import com.unisono.card.limits.domain.port.input.RecalculateAvailableCreditUseCase.ReleaseCreditCommand;
import com.unisono.card.limits.domain.port.input.RecalculateAvailableCreditUseCase.RestoreCreditCommand;
import com.unisono.card.limits.domain.port.input.UpdateCreditLimitUseCase;
import com.unisono.card.limits.domain.port.input.UpdateCreditLimitUseCase.DecreasePurchaseLimitCommand;
import com.unisono.card.limits.domain.port.input.UpdateCreditLimitUseCase.IncreasePurchaseLimitCommand;
import com.unisono.card.limits.domain.port.input.UpdateCreditLimitUseCase.UpdateCashAdvanceLimitCommand;
import com.unisono.card.limits.domain.port.input.UpdateCreditLimitUseCase.UpdateTemporalLimitsCommand;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests de {@link CreditLimitController}: se invoca el controller directamente, sin contexto Spring,
 * cubriendo todas las ramas (límites diario/mensual opcionales, Optional presente/vacío e historial vacío/no vacío).
 */
@ExtendWith(MockitoExtension.class)
class CreditLimitControllerTest {

    private static final String CARD_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String CREDIT_LIMIT_ID = "cl-123";
    private static final String CURRENCY = "ARS";

    @Mock
    private AssignCreditLimitUseCase assignCreditLimitUseCase;
    @Mock
    private UpdateCreditLimitUseCase updateCreditLimitUseCase;
    @Mock
    private RecalculateAvailableCreditUseCase recalculateAvailableCreditUseCase;
    @Mock
    private GetCreditInfoUseCase getCreditInfoUseCase;
    @Mock
    private CreditLimitDtoMapper mapper;

    @InjectMocks
    private CreditLimitController controller;

    private final CreditLimit creditLimit = mock(CreditLimit.class);
    private final CreditLimitResponse creditLimitResponse = CreditLimitResponse.builder().build();

    private static Money ars(long amount) {
        return Money.ofARS(amount);
    }

    // ========================================
    // ASSIGN CREDIT LIMIT (path cardId)
    // ========================================

    @Nested
    @DisplayName("POST /cards/{cardId}")
    class AssignCreditLimitByPath {

        private AssignCreditLimitRequest.AssignCreditLimitRequestBuilder baseRequest() {
            return AssignCreditLimitRequest.builder()
                    .purchaseLimitAmount(new BigDecimal("500000"))
                    .currency(CURRENCY)
                    .cashAdvancePercentage(0.4)
                    .assignedBy("admin@unisono.com");
        }

        private AssignCreditLimitCommand executeAndCaptureCommand(AssignCreditLimitRequest request) {
            when(assignCreditLimitUseCase.execute(any())).thenReturn(creditLimit);
            when(mapper.toResponse(creditLimit)).thenReturn(creditLimitResponse);

            ResponseEntity<CreditLimitResponse> result = controller.assignCreditLimit(CARD_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isSameAs(creditLimitResponse);

            ArgumentCaptor<AssignCreditLimitCommand> captor = ArgumentCaptor.forClass(AssignCreditLimitCommand.class);
            verify(assignCreditLimitUseCase).execute(captor.capture());
            verify(mapper, never()).toCommand(any());
            return captor.getValue();
        }

        @Test
        @DisplayName("construye el command con límites diario y mensual cuando vienen informados")
        void shouldBuildCommandWithDailyAndMonthlyLimits() {
            AssignCreditLimitRequest request = baseRequest()
                    .dailyLimitAmount(new BigDecimal("100000"))
                    .monthlyLimitAmount(new BigDecimal("300000"))
                    .build();

            AssignCreditLimitCommand command = executeAndCaptureCommand(request);

            assertThat(command.cardId()).isEqualTo(CardId.of(CARD_ID));
            assertThat(command.purchaseLimitAmount()).isEqualTo(ars(500_000));
            assertThat(command.cashAdvancePercentage()).isEqualTo(0.4);
            assertThat(command.dailyLimitAmount()).isEqualTo(ars(100_000));
            assertThat(command.monthlyLimitAmount()).isEqualTo(ars(300_000));
            assertThat(command.assignedBy()).isEqualTo("admin@unisono.com");
        }

        @Test
        @DisplayName("deja en null los límites diario y mensual cuando no vienen informados")
        void shouldBuildCommandWithoutDailyAndMonthlyLimits() {
            AssignCreditLimitCommand command = executeAndCaptureCommand(baseRequest().build());

            assertThat(command.dailyLimitAmount()).isNull();
            assertThat(command.monthlyLimitAmount()).isNull();
        }

        @Test
        @DisplayName("solo límite diario informado")
        void shouldBuildCommandWithOnlyDailyLimit() {
            AssignCreditLimitCommand command = executeAndCaptureCommand(
                    baseRequest().dailyLimitAmount(new BigDecimal("100000")).build());

            assertThat(command.dailyLimitAmount()).isEqualTo(ars(100_000));
            assertThat(command.monthlyLimitAmount()).isNull();
        }

        @Test
        @DisplayName("solo límite mensual informado")
        void shouldBuildCommandWithOnlyMonthlyLimit() {
            AssignCreditLimitCommand command = executeAndCaptureCommand(
                    baseRequest().monthlyLimitAmount(new BigDecimal("300000")).build());

            assertThat(command.dailyLimitAmount()).isNull();
            assertThat(command.monthlyLimitAmount()).isEqualTo(ars(300_000));
        }

        @Test
        @DisplayName("propaga el error del use case sin mapear respuesta")
        void shouldPropagateUseCaseException() {
            when(assignCreditLimitUseCase.execute(any())).thenThrow(new IllegalStateException("already assigned"));

            assertThatThrownBy(() -> controller.assignCreditLimit(CARD_ID, baseRequest().build()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("already assigned");
            verify(mapper, never()).toResponse(any(CreditLimit.class));
        }
    }

    // ========================================
    // ASSIGN CREDIT LIMIT (body cardId)
    // ========================================

    @Test
    @DisplayName("POST / asigna el límite usando el mapper para construir el command")
    void shouldAssignCreditLimitFromBody() {
        AssignCreditLimitRequest request = AssignCreditLimitRequest.builder()
                .cardId(CARD_ID)
                .purchaseLimitAmount(new BigDecimal("500000"))
                .currency(CURRENCY)
                .assignedBy("admin")
                .build();
        AssignCreditLimitCommand command = new AssignCreditLimitCommand(
                CardId.of(CARD_ID), ars(500_000), 0.3, null, null, "admin");
        when(mapper.toCommand(request)).thenReturn(command);
        when(assignCreditLimitUseCase.execute(command)).thenReturn(creditLimit);
        when(mapper.toResponse(creditLimit)).thenReturn(creditLimitResponse);

        ResponseEntity<CreditLimitResponse> result = controller.assignCreditLimit(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(creditLimitResponse);
    }

    // ========================================
    // GET CREDIT LIMIT
    // ========================================

    @Nested
    @DisplayName("GET /{creditLimitId}")
    class GetCreditLimitById {

        @Test
        void shouldReturnCreditLimitWhenFound() {
            when(getCreditInfoUseCase.findById(CREDIT_LIMIT_ID)).thenReturn(Optional.of(creditLimit));
            when(mapper.toResponse(creditLimit)).thenReturn(creditLimitResponse);

            ResponseEntity<CreditLimitResponse> result = controller.getCreditLimitById(CREDIT_LIMIT_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(creditLimitResponse);
        }

        @Test
        void shouldThrowNotFoundWhenMissing() {
            when(getCreditInfoUseCase.findById(CREDIT_LIMIT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> controller.getCreditLimitById(CREDIT_LIMIT_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("CreditLimit")
                    .hasMessageContaining(CREDIT_LIMIT_ID);
            verifyNoInteractions(mapper);
        }
    }

    @Nested
    @DisplayName("GET /card/{cardId}")
    class GetCreditLimitByCard {

        @Test
        void shouldReturnCreditLimitWhenFound() {
            when(getCreditInfoUseCase.findByCardId(CardId.of(CARD_ID))).thenReturn(Optional.of(creditLimit));
            when(mapper.toResponse(creditLimit)).thenReturn(creditLimitResponse);

            ResponseEntity<CreditLimitResponse> result = controller.getCreditLimitByCard(CARD_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(creditLimitResponse);
        }

        @Test
        void shouldThrowNotFoundWhenMissing() {
            when(getCreditInfoUseCase.findByCardId(CardId.of(CARD_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> controller.getCreditLimitByCard(CARD_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("for card " + CARD_ID);
            verifyNoInteractions(mapper);
        }
    }

    @Nested
    @DisplayName("GET /card/{cardId}/info")
    class GetCreditInfo {

        @Test
        void shouldReturnCreditInfoWhenFound() {
            CreditInfoResponse info = CreditInfoResponse.builder().build();
            when(getCreditInfoUseCase.findByCardId(CardId.of(CARD_ID))).thenReturn(Optional.of(creditLimit));
            when(mapper.toCreditInfoResponse(creditLimit)).thenReturn(info);

            ResponseEntity<CreditInfoResponse> result = controller.getCreditInfo(CARD_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(info);
        }

        @Test
        void shouldThrowNotFoundWhenMissing() {
            when(getCreditInfoUseCase.findByCardId(CardId.of(CARD_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> controller.getCreditInfo(CARD_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("for card " + CARD_ID);
            verifyNoInteractions(mapper);
        }
    }

    // ========================================
    // UPDATE LIMITS
    // ========================================

    @Nested
    @DisplayName("PUT update limits")
    class UpdateLimits {

        private final UpdateCreditLimitRequest request = UpdateCreditLimitRequest.builder()
                .amount(new BigDecimal("10000"))
                .currency(CURRENCY)
                .reason("ajuste")
                .changedBy("admin")
                .build();

        @Test
        void shouldIncreasePurchaseLimit() {
            IncreasePurchaseLimitCommand command =
                    new IncreasePurchaseLimitCommand(CREDIT_LIMIT_ID, ars(10_000), "ajuste", "admin");
            when(mapper.toIncreaseCommand(CREDIT_LIMIT_ID, request)).thenReturn(command);
            when(updateCreditLimitUseCase.increasePurchaseLimit(command)).thenReturn(creditLimit);
            when(mapper.toResponse(creditLimit)).thenReturn(creditLimitResponse);

            ResponseEntity<CreditLimitResponse> result = controller.increasePurchaseLimit(CREDIT_LIMIT_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(creditLimitResponse);
        }

        @Test
        void shouldDecreasePurchaseLimit() {
            DecreasePurchaseLimitCommand command =
                    new DecreasePurchaseLimitCommand(CREDIT_LIMIT_ID, ars(10_000), "ajuste", "admin");
            when(mapper.toDecreaseCommand(CREDIT_LIMIT_ID, request)).thenReturn(command);
            when(updateCreditLimitUseCase.decreasePurchaseLimit(command)).thenReturn(creditLimit);
            when(mapper.toResponse(creditLimit)).thenReturn(creditLimitResponse);

            ResponseEntity<CreditLimitResponse> result = controller.decreasePurchaseLimit(CREDIT_LIMIT_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(creditLimitResponse);
        }

        @Test
        void shouldUpdateCashAdvanceLimit() {
            UpdateCashAdvanceLimitCommand command = mock(UpdateCashAdvanceLimitCommand.class);
            when(mapper.toCashAdvanceCommand(CREDIT_LIMIT_ID, request)).thenReturn(command);
            when(updateCreditLimitUseCase.updateCashAdvanceLimit(command)).thenReturn(creditLimit);
            when(mapper.toResponse(creditLimit)).thenReturn(creditLimitResponse);

            ResponseEntity<CreditLimitResponse> result = controller.updateCashAdvanceLimit(CREDIT_LIMIT_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(creditLimitResponse);
        }

        @Test
        void shouldUpdateTemporalLimits() {
            UpdateTemporalLimitsRequest temporalRequest = UpdateTemporalLimitsRequest.builder()
                    .dailyLimitAmount(new BigDecimal("50000"))
                    .monthlyLimitAmount(new BigDecimal("200000"))
                    .currency(CURRENCY)
                    .reason("ajuste")
                    .changedBy("admin")
                    .build();
            UpdateTemporalLimitsCommand command = mock(UpdateTemporalLimitsCommand.class);
            when(mapper.toTemporalCommand(CREDIT_LIMIT_ID, temporalRequest)).thenReturn(command);
            when(updateCreditLimitUseCase.updateTemporalLimits(command)).thenReturn(creditLimit);
            when(mapper.toResponse(creditLimit)).thenReturn(creditLimitResponse);

            ResponseEntity<CreditLimitResponse> result =
                    controller.updateTemporalLimits(CREDIT_LIMIT_ID, temporalRequest);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(creditLimitResponse);
        }
    }

    // ========================================
    // CONSUME & RELEASE CREDIT
    // ========================================

    @Nested
    @DisplayName("POST consume / release / restore")
    class ConsumeAndRelease {

        private final ConsumeCreditRequest consumeRequest = ConsumeCreditRequest.builder()
                .amount(new BigDecimal("2000"))
                .currency(CURRENCY)
                .description("compra")
                .build();

        private final ConsumeCreditCommand consumeCommand =
                new ConsumeCreditCommand(CREDIT_LIMIT_ID, ars(2_000), "compra");

        @Test
        void shouldConsumeForPurchase() {
            when(mapper.toConsumeCommand(CREDIT_LIMIT_ID, consumeRequest)).thenReturn(consumeCommand);
            when(recalculateAvailableCreditUseCase.consumeForPurchase(consumeCommand)).thenReturn(creditLimit);
            when(mapper.toResponse(creditLimit)).thenReturn(creditLimitResponse);

            ResponseEntity<CreditLimitResponse> result = controller.consumeForPurchase(CREDIT_LIMIT_ID, consumeRequest);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(creditLimitResponse);
            verify(recalculateAvailableCreditUseCase, never()).consumeForCashAdvance(any());
        }

        @Test
        void shouldConsumeForCashAdvance() {
            when(mapper.toConsumeCommand(CREDIT_LIMIT_ID, consumeRequest)).thenReturn(consumeCommand);
            when(recalculateAvailableCreditUseCase.consumeForCashAdvance(consumeCommand)).thenReturn(creditLimit);
            when(mapper.toResponse(creditLimit)).thenReturn(creditLimitResponse);

            ResponseEntity<CreditLimitResponse> result =
                    controller.consumeForCashAdvance(CREDIT_LIMIT_ID, consumeRequest);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(creditLimitResponse);
            verify(recalculateAvailableCreditUseCase, never()).consumeForPurchase(any());
        }

        @Test
        void shouldReleaseCredit() {
            ReleaseCreditRequest request = ReleaseCreditRequest.builder()
                    .paymentAmount(new BigDecimal("5000"))
                    .currency(CURRENCY)
                    .description("pago")
                    .build();
            ReleaseCreditCommand command = new ReleaseCreditCommand(CREDIT_LIMIT_ID, ars(5_000), "pago");
            when(mapper.toReleaseCommand(CREDIT_LIMIT_ID, request)).thenReturn(command);
            when(recalculateAvailableCreditUseCase.releaseCredit(command)).thenReturn(creditLimit);
            when(mapper.toResponse(creditLimit)).thenReturn(creditLimitResponse);

            ResponseEntity<CreditLimitResponse> result = controller.releaseCredit(CREDIT_LIMIT_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(creditLimitResponse);
        }

        @Test
        void shouldRestoreCredit() {
            LocalDate today = LocalDate.now();
            RestoreCreditRequest request = RestoreCreditRequest.builder()
                    .amount(new BigDecimal("2000"))
                    .currency(CURRENCY)
                    .originalTransactionDate(today)
                    .description("reverso")
                    .build();
            RestoreCreditCommand command = new RestoreCreditCommand(CREDIT_LIMIT_ID, ars(2_000), today, "reverso");
            when(mapper.toRestoreCommand(CREDIT_LIMIT_ID, request)).thenReturn(command);
            when(recalculateAvailableCreditUseCase.restoreCredit(command)).thenReturn(creditLimit);
            when(mapper.toResponse(creditLimit)).thenReturn(creditLimitResponse);

            ResponseEntity<CreditLimitResponse> result = controller.restoreCredit(CREDIT_LIMIT_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(creditLimitResponse);
        }
    }

    // ========================================
    // HISTORY
    // ========================================

    @Nested
    @DisplayName("GET /{creditLimitId}/history")
    class History {

        @Test
        void shouldMapEveryHistoryEntryPreservingOrder() {
            CreditLimitHistory first = mock(CreditLimitHistory.class);
            CreditLimitHistory second = mock(CreditLimitHistory.class);
            CreditLimitHistoryResponse firstResponse = CreditLimitHistoryResponse.builder().build();
            CreditLimitHistoryResponse secondResponse = CreditLimitHistoryResponse.builder().build();
            when(getCreditInfoUseCase.getHistory(CREDIT_LIMIT_ID)).thenReturn(List.of(first, second));
            when(mapper.toHistoryResponse(first)).thenReturn(firstResponse);
            when(mapper.toHistoryResponse(second)).thenReturn(secondResponse);

            ResponseEntity<List<CreditLimitHistoryResponse>> result = controller.getHistory(CREDIT_LIMIT_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).containsExactly(firstResponse, secondResponse);
        }

        @Test
        void shouldReturnEmptyListWhenNoHistory() {
            when(getCreditInfoUseCase.getHistory(CREDIT_LIMIT_ID)).thenReturn(List.of());

            ResponseEntity<List<CreditLimitHistoryResponse>> result = controller.getHistory(CREDIT_LIMIT_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEmpty();
            verifyNoInteractions(mapper);
        }
    }

    @Test
    @DisplayName("cancelOperation informa el resultado y el exceso sobre el límite")
    void cancelOperationReportsOutcomeAndOverLimit() {
        CreditLimit real = CreditLimit.assign(CardId.of(CARD_ID), Money.ofARS(100_000), 0.3, "test");
        when(recalculateAvailableCreditUseCase.cancelOperation(any())).thenReturn(
                new RecalculateAvailableCreditUseCase.CancelOperationResult(
                        RecalculateAvailableCreditUseCase.CancelOutcome.CANCELLED, real));

        ResponseEntity<com.unisono.card.limits.application.dto.response.CancelOperationResponse> response =
                controller.cancelOperation(CREDIT_LIMIT_ID, "op-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutcome()).isEqualTo("CANCELLED");
        assertThat(response.getBody().getOperationId()).isEqualTo("op-1");
        assertThat(response.getBody().getAvailableCredit()).isEqualByComparingTo("100000");
        assertThat(response.getBody().getOverLimitAmount()).isEqualByComparingTo("0");
        ArgumentCaptor<RecalculateAvailableCreditUseCase.CancelOperationCommand> command =
                ArgumentCaptor.forClass(RecalculateAvailableCreditUseCase.CancelOperationCommand.class);
        verify(recalculateAvailableCreditUseCase).cancelOperation(command.capture());
        assertThat(command.getValue().creditLimitId()).isEqualTo(CREDIT_LIMIT_ID);
        assertThat(command.getValue().operationId()).isEqualTo("op-1");
    }
}
