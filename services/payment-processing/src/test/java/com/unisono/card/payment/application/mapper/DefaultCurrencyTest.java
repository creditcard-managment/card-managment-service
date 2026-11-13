package com.unisono.card.payment.application.mapper;

import com.unisono.card.payment.application.dto.response.SettlementResponse;
import com.unisono.card.payment.application.dto.response.TransactionSummaryResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

/**
 * Sin transacciones, resúmenes y liquidaciones informan la moneda del sistema (ARS), no USD
 */
class DefaultCurrencyTest {

    @Test
    void emptySummaryUsesTheSystemCurrency() {
        TransactionMapper mapper = mock(TransactionMapper.class, CALLS_REAL_METHODS);

        TransactionSummaryResponse summary = mapper.toTransactionSummary(List.of());

        assertThat(summary.getCurrency()).isEqualTo("ARS");
        assertThat(summary.getTotalTransactions()).isZero();
    }

    @Test
    void emptySettlementUsesTheSystemCurrency() {
        SettlementMapper mapper = mock(SettlementMapper.class, CALLS_REAL_METHODS);

        SettlementResponse response = mapper.toSettlementResponse("batch-1", List.of(), List.of());

        assertThat(response.getCurrency()).isEqualTo("ARS");
    }
}
