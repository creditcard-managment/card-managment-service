package com.unisono.card.data.infrastructure.adapter.input.rest;

import com.unisono.card.data.application.dto.response.CardResponse;
import com.unisono.card.data.application.mapper.CardDtoMapper;
import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.ResetPINUseCase;
import com.unisono.card.data.domain.port.input.ResetPINUseCase.ResetPINCommand;
import com.unisono.card.data.domain.port.input.ValidatePINUseCase;
import com.unisono.card.data.domain.port.input.ValidatePINUseCase.ValidatePINCommand;
import com.unisono.card.data.infrastructure.config.KafkaProducerConfig;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PINController aislado: casos de uso y mapper mockeados, GlobalExceptionHandler real
 * y el mismo ObjectMapper que la aplicación.
 */
@ExtendWith(MockitoExtension.class)
class PINControllerTest {

    private static final String CARD_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private ValidatePINUseCase validatePINUseCase;

    @Mock
    private ResetPINUseCase resetPINUseCase;

    @Mock
    private CardDtoMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PINController(validatePINUseCase, resetPINUseCase, mapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new KafkaProducerConfig().objectMapper()))
                .build();
    }

    // ========== POST /validate-pin ==========

    @Nested
    class ValidatePIN {

        @Test
        void shouldReturnValidTrueWhenPinMatches() throws Exception {
            when(validatePINUseCase.execute(any())).thenReturn(true);

            validatePin(CARD_ID, "{\"pin\":\"4821\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));

            ArgumentCaptor<ValidatePINCommand> command = ArgumentCaptor.forClass(ValidatePINCommand.class);
            verify(validatePINUseCase).execute(command.capture());
            assertThat(command.getValue().cardId()).isEqualTo(CardId.of(CARD_ID));
            assertThat(command.getValue().plainPin()).isEqualTo("4821");
        }

        @Test
        void shouldReturnValidFalseWhenPinDoesNotMatch() throws Exception {
            when(validatePINUseCase.execute(any())).thenReturn(false);

            validatePin(CARD_ID, "{\"pin\":\"9999\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false));
        }

        @Test
        void shouldNormalizeCardIdToLowerCase() throws Exception {
            when(validatePINUseCase.execute(any())).thenReturn(true);

            validatePin(CARD_ID.toUpperCase(), "{\"pin\":\"4821\"}").andExpect(status().isOk());

            ArgumentCaptor<ValidatePINCommand> command = ArgumentCaptor.forClass(ValidatePINCommand.class);
            verify(validatePINUseCase).execute(command.capture());
            assertThat(command.getValue().cardId().getValue()).isEqualTo(CARD_ID);
        }

        @ParameterizedTest
        @ValueSource(strings = {"{\"pin\":\"123\"}", "{\"pin\":\"12345\"}", "{\"pin\":\"12a4\"}"})
        void shouldReturn400WhenPinIsMalformed(String body) throws Exception {
            validatePin(CARD_ID, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.pin").exists());

            verifyNoInteractions(validatePINUseCase);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "{}",
                "{\"pin\":\"\"}",
                "{\"pinBlock\":\"00112233445566778899AABBCCDDEEFF\"}",
                "{\"pinBlock\":\"00112233445566778899AABBCCDDEEFF\",\"pinKeyId\":\"pek-v1\",\"pin\":\"4821\"}",
                "{\"pin\":\"4821\",\"pinKeyId\":\"pek-v1\"}"
        })
        void shouldReturn400UnlessItSendsEitherAPinBlockWithItsKeyOrAPin(String body) throws Exception {
            validatePin(CARD_ID, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.pinOrPinBlock").exists());

            verifyNoInteractions(validatePINUseCase);
        }

        @Test
        void shouldReturn400WhenPinBlockIsNotHex() throws Exception {
            validatePin(CARD_ID, "{\"pinBlock\":\"" + "Z".repeat(32) + "\",\"pinKeyId\":\"pek-v1\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.pinBlock").exists());

            verifyNoInteractions(validatePINUseCase);
        }

        @Test
        void shouldPassThePinBlockAndItsKeyToTheUseCase() throws Exception {
            when(validatePINUseCase.execute(any())).thenReturn(true);

            validatePin(CARD_ID, "{\"pinBlock\":\"00112233445566778899aabbccddeeff\",\"pinKeyId\":\"pek-v1\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));

            ArgumentCaptor<ValidatePINCommand> command = ArgumentCaptor.forClass(ValidatePINCommand.class);
            verify(validatePINUseCase).execute(command.capture());
            assertThat(command.getValue().pinBlock()).isEqualTo("00112233445566778899aabbccddeeff");
            assertThat(command.getValue().pinKeyId()).isEqualTo("pek-v1");
            assertThat(command.getValue().plainPin()).isNull();
        }

        @Test
        void shouldReturn400WhenBodyIsNotJson() throws Exception {
            validatePin(CARD_ID, "{pin")
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(validatePINUseCase);
        }

        @Test
        void shouldReturn400WhenCardIdIsNotUuid() throws Exception {
            validatePin("card-123", "{\"pin\":\"4821\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("UUID")));

            verifyNoInteractions(validatePINUseCase);
        }

        @Test
        void shouldReturn404WhenCardDoesNotExist() throws Exception {
            when(validatePINUseCase.execute(any())).thenThrow(new NotFoundException("Card", CARD_ID));

            validatePin(CARD_ID, "{\"pin\":\"4821\"}")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }

        @Test
        void shouldReturn422WhenPinIsBlocked() throws Exception {
            when(validatePINUseCase.execute(any()))
                    .thenThrow(new BusinessRuleException("PIN bloqueado por intentos fallidos"));

            validatePin(CARD_ID, "{\"pin\":\"4821\"}")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("PIN bloqueado por intentos fallidos"));
        }

        private ResultActions validatePin(String cardId, String body) throws Exception {
            return mockMvc.perform(post("/api/v1/cards/{cardId}/validate-pin", cardId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
        }
    }

    // ========== POST /reset-pin ==========

    @Nested
    class ResetPIN {

        @Test
        void shouldResetPinAndReturnMappedCard() throws Exception {
            Card card = mock(Card.class);
            when(resetPINUseCase.execute(any())).thenReturn(card);
            when(mapper.toResponse(card)).thenReturn(CardResponse.builder()
                    .id(CARD_ID)
                    .maskedCardNumber("**** **** **** 0366")
                    .build());

            resetPin(CARD_ID, "{\"oldPin\":\"4821\",\"newPin\":\"9073\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(CARD_ID))
                    .andExpect(jsonPath("$.maskedCardNumber").value("**** **** **** 0366"));

            ArgumentCaptor<ResetPINCommand> command = ArgumentCaptor.forClass(ResetPINCommand.class);
            verify(resetPINUseCase).execute(command.capture());
            assertThat(command.getValue().cardId()).isEqualTo(CardId.of(CARD_ID));
            assertThat(command.getValue().oldPlainPin()).isEqualTo("4821");
            assertThat(command.getValue().newPlainPin()).isEqualTo("9073");
            verify(mapper).toResponse(card);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "{\"newPin\":\"9073\"}",
                "{\"oldPin\":\"\",\"newPin\":\"9073\"}",
                "{\"oldPin\":\"48\",\"newPin\":\"9073\"}"
        })
        void shouldReturn400WhenOldPinIsMissingOrMalformed(String body) throws Exception {
            resetPin(CARD_ID, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.oldPin").exists())
                    .andExpect(jsonPath("$.fieldErrors.newPin").doesNotExist());

            verifyNoInteractions(resetPINUseCase, mapper);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "{\"oldPin\":\"4821\"}",
                "{\"oldPin\":\"4821\",\"newPin\":\"\"}",
                "{\"oldPin\":\"4821\",\"newPin\":\"90731\"}"
        })
        void shouldReturn400WhenNewPinIsMissingOrMalformed(String body) throws Exception {
            resetPin(CARD_ID, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.newPin").exists())
                    .andExpect(jsonPath("$.fieldErrors.oldPin").doesNotExist());

            verifyNoInteractions(resetPINUseCase, mapper);
        }

        @Test
        void shouldReturn400WhenCardIdIsNotUuid() throws Exception {
            resetPin("card-123", "{\"oldPin\":\"4821\",\"newPin\":\"9073\"}")
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(resetPINUseCase, mapper);
        }

        @Test
        void shouldReturn404WhenCardDoesNotExist() throws Exception {
            when(resetPINUseCase.execute(any())).thenThrow(new NotFoundException("Card", CARD_ID));

            resetPin(CARD_ID, "{\"oldPin\":\"4821\",\"newPin\":\"9073\"}")
                    .andExpect(status().isNotFound());

            verifyNoInteractions(mapper);
        }

        @Test
        void shouldReturn422WhenOldPinIsWrong() throws Exception {
            when(resetPINUseCase.execute(any())).thenThrow(new BusinessRuleException("PIN actual incorrecto"));

            resetPin(CARD_ID, "{\"oldPin\":\"0000\",\"newPin\":\"9073\"}")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("PIN actual incorrecto"));

            verifyNoInteractions(mapper);
        }

        @Test
        void shouldReturn400WhenNewPinBreaksDomainRules() throws Exception {
            // "1234" pasa el @Pattern del DTO pero el VO PIN rechaza secuencias
            when(resetPINUseCase.execute(any()))
                    .thenThrow(new InvalidValueObjectException(
                            "El PIN no puede ser una secuencia obvia (ej: 1234)"));

            resetPin(CARD_ID, "{\"oldPin\":\"4821\",\"newPin\":\"1234\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("El PIN no puede ser una secuencia obvia (ej: 1234)"));

            verifyNoInteractions(mapper);
        }

        private ResultActions resetPin(String cardId, String body) throws Exception {
            return mockMvc.perform(post("/api/v1/cards/{cardId}/reset-pin", cardId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
        }
    }
}
