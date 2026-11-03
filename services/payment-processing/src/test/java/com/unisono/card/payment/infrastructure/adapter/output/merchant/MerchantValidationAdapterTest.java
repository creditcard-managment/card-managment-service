package com.unisono.card.payment.infrastructure.adapter.output.merchant;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentTerminalEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaMerchantRepository;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentTerminalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantValidationAdapterTest {

    private static final String MERCHANT_ID = "6f1c2d3e-0000-4000-8000-000000000001";

    @Mock
    private JpaMerchantRepository merchantRepository;

    @Mock
    private JpaPaymentTerminalRepository terminalRepository;

    private MerchantValidationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MerchantValidationAdapter(merchantRepository, terminalRepository);
    }

    @Test
    void shouldAcceptActiveMerchant() {
        when(merchantRepository.existsByIdAndStatus(MERCHANT_ID, "ACTIVE")).thenReturn(true);

        assertThat(adapter.isMerchantActive(MERCHANT_ID)).isTrue();
    }

    @Test
    void shouldRejectMissingOrInactiveMerchant() {
        when(merchantRepository.existsByIdAndStatus(MERCHANT_ID, "ACTIVE")).thenReturn(false);

        assertThat(adapter.isMerchantActive(MERCHANT_ID)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void shouldRejectBlankMerchantIdWithoutQuerying(String merchantId) {
        assertThat(adapter.isMerchantActive(merchantId)).isFalse();
        verifyNoInteractions(merchantRepository);
    }

    @Test
    void shouldResolveMerchantOfActiveTerminal() {
        PaymentTerminalEntity terminal = new PaymentTerminalEntity();
        ReflectionTestUtils.setField(terminal, "merchantId", MERCHANT_ID);
        when(terminalRepository.findByIdAndStatus("ATM-01", "ACTIVE")).thenReturn(Optional.of(terminal));

        assertThat(adapter.findMerchantIdByTerminal("ATM-01")).contains(MERCHANT_ID);
    }

    @Test
    void shouldReturnEmptyForUnknownOrInactiveTerminal() {
        when(terminalRepository.findByIdAndStatus("ATM-99", "ACTIVE")).thenReturn(Optional.empty());

        assertThat(adapter.findMerchantIdByTerminal("ATM-99")).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnEmptyForBlankTerminal(String terminalId) {
        assertThat(adapter.findMerchantIdByTerminal(terminalId)).isEmpty();
        verifyNoInteractions(terminalRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"5411", "0000"})
    void shouldAcceptFourDigitMcc(String mcc) {
        assertThat(adapter.isValidMCC(mcc)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"541", "54111", "54a1"})
    void shouldRejectInvalidMcc(String mcc) {
        assertThat(adapter.isValidMCC(mcc)).isFalse();
    }
}
