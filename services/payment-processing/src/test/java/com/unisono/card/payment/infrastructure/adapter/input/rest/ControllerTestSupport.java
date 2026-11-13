package com.unisono.card.payment.infrastructure.adapter.input.rest;

import com.unisono.card.payment.application.mapper.ErrorMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * MockMvc standalone con el GlobalExceptionHandler real: valida request, status y JSON sin levantar Spring
 */
final class ControllerTestSupport {

    private ControllerTestSupport() {
    }

    static MockMvc mockMvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new ErrorMapper()))
                .build();
    }
}
