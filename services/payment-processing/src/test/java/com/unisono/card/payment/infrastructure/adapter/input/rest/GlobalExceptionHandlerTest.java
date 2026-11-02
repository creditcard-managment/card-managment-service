package com.unisono.card.payment.infrastructure.adapter.input.rest;

import com.unisono.card.payment.application.mapper.ErrorMapper;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler(new ErrorMapper()))
                .build();
    }

    @Test
    void shouldReturn400ForInvalidValueObject() throws Exception {
        mockMvc.perform(get("/invalid-expiration"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VALUE"));
    }

    @Test
    void shouldReturn422ForDifferentCurrencies() throws Exception {
        mockMvc.perform(get("/mixed-currencies"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/invalid-expiration")
        void invalidExpiration() {
            ExpirationDate.of("13/2030");
        }

        @GetMapping("/mixed-currencies")
        void mixedCurrencies() {
            Money.ofARS(100).add(Money.of(BigDecimal.TEN, "USD"));
        }
    }
}
