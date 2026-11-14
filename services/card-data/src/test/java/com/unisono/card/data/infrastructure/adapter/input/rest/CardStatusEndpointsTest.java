package com.unisono.card.data.infrastructure.adapter.input.rest;

import com.unisono.card.data.application.mapper.CardDtoMapper;
import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.model.CardCategory;
import com.unisono.card.data.domain.model.CardHolder;
import com.unisono.card.data.domain.model.CardType;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.data.domain.service.CardEventEmitter;
import com.unisono.card.data.domain.service.CardManagementService;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.*;
import com.unisono.card.data.domain.service.CardSecurityService;
import com.unisono.card.data.infrastructure.config.KafkaProducerConfig;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.domain.valueobject.ClientId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoints de cambio de estado consumidos por card-lifecycle, con los servicios reales
 * y un repositorio en memoria. Usa el mismo ObjectMapper que la aplicación.
 */
class CardStatusEndpointsTest {

    private static final String VALID_CARD_DATA = """
            {"cardNumber":"4532015112830366","cvv":"123","pin":"4821","expirationMonth":12,"expirationYear":%d}
            """;

    private record EnqueuedEvent(String topic, String key, DomainEvent event) {}

    private InMemoryCardRepository repository;
    private List<EnqueuedEvent> outbox;
    private MockMvc mockMvc;
    private String cardId;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCardRepository();
        outbox = new ArrayList<>();
        CardEventEmitter emitter = new CardEventEmitter(
                (topic, aggregateId, event) -> outbox.add(new EnqueuedEvent(topic, aggregateId, event)));
        CardManagementService managementService = new CardManagementService(repository, emitter);
        CardSecurityService securityService = new CardSecurityService(repository, emitter);
        CardDtoMapper mapper = new CardDtoMapper();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new CardController(managementService, managementService, managementService, managementService, mapper),
                        new CardSecurityController(securityService, securityService, mapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new KafkaProducerConfig().objectMapper()))
                .build();

        cardId = repository.save(newCard()).getId().getValue();
    }

    // ========== PUT /status ==========

    @Test
    void shouldApproveRequestedCard() throws Exception {
        putJson("/status", "{\"status\":\"APPROVED\"}").andExpect(status().isOk());

        assertThat(storedCard().getStatus()).isEqualTo(CardStatus.APPROVED);
    }

    @Test
    void shouldReturn422ForInvalidTransition() throws Exception {
        putJson("/status", "{\"status\":\"ACTIVE\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Business Rule Violation"));

        assertThat(storedCard().getStatus()).isEqualTo(CardStatus.REQUESTED);
    }

    @Test
    void shouldReturn400ForUnknownStatus() throws Exception {
        putJson("/status", "{\"status\":\"FOO\"}").andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenStatusIsMissing() throws Exception {
        putJson("/status", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void shouldReturn404ForUnknownCard() throws Exception {
        mockMvc.perform(put("/api/v1/cards/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isNotFound());
    }

    // ========== PUT /card-data ==========

    @Test
    void shouldLoadCardData() throws Exception {
        approve();

        putJson("/card-data", VALID_CARD_DATA.formatted(LocalDate.now().getYear() + 2))
                .andExpect(status().isOk());

        Card card = storedCard();
        assertThat(card.getCardNumber().getLastFourDigits()).isEqualTo("0366");
        assertThat(card.getExpirationDate()).isNotNull();
        assertThat(card.getIssuedAt()).isNotNull();
    }

    @Test
    void shouldReturn400ForNonNumericCardNumber() throws Exception {
        putJson("/card-data", VALID_CARD_DATA.formatted(LocalDate.now().getYear() + 2)
                .replace("4532015112830366", "45320A5112830366"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.cardNumber").exists());
    }

    @Test
    void shouldReturn400ForPastExpirationYear() throws Exception {
        putJson("/card-data", VALID_CARD_DATA.formatted(LocalDate.now().getYear() - 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    void shouldReturn400ForNonNumericExpirationYear() throws Exception {
        putJson("/card-data", VALID_CARD_DATA.formatted(2030).replace("2030", "\"x\""))
                .andExpect(status().isBadRequest());
    }

    // ========== PUT /activated-at ==========

    @Test
    void shouldStoreActivationDate() throws Exception {
        putJson("/activated-at", "{\"activatedAt\":\"2026-11-02T10:15:30\"}").andExpect(status().isOk());

        assertThat(storedCard().getActivatedAt()).isEqualTo(LocalDateTime.of(2026, 11, 2, 10, 15, 30));
    }

    @Test
    void shouldReturn400ForMalformedActivationDate() throws Exception {
        putJson("/activated-at", "{\"activatedAt\":\"no-es-fecha\"}").andExpect(status().isBadRequest());
    }

    // ========== Flujo completo y bloqueo ==========

    @Test
    void shouldCompleteLifecycleFlowAndBlockUnblock() throws Exception {
        approve();
        putJson("/card-data", VALID_CARD_DATA.formatted(LocalDate.now().getYear() + 2)).andExpect(status().isOk());
        putJson("/status", "{\"status\":\"ISSUED\"}").andExpect(status().isOk());
        putJson("/status", "{\"status\":\"ACTIVE\"}").andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/cards/" + cardId + "/unblock"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/v1/cards/" + cardId + "/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Pérdida\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        mockMvc.perform(post("/api/v1/cards/" + cardId + "/unblock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn422WhenBlockingRequestedCard() throws Exception {
        mockMvc.perform(post("/api/v1/cards/" + cardId + "/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Pérdida\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ========== Eventos ==========

    @Test
    void shouldEnqueueOneEventPerTransitionWithReasonAndAuthor() throws Exception {
        putJson("/status", "{\"status\":\"APPROVED\",\"reason\":\"Cumple\",\"performedBy\":\"admin\"}")
                .andExpect(status().isOk());
        putJson("/card-data", VALID_CARD_DATA.formatted(LocalDate.now().getYear() + 2)).andExpect(status().isOk());
        putJson("/status", "{\"status\":\"ISSUED\",\"reason\":\"Emisión\",\"performedBy\":\"emisor\"}")
                .andExpect(status().isOk());
        putJson("/status", "{\"status\":\"ACTIVE\",\"reason\":\"App\",\"performedBy\":\"cliente\"}")
                .andExpect(status().isOk());
        postJson("/block-fraud", "{\"reason\":\"Consumos raros\",\"performedBy\":\"antifraude\"}")
                .andExpect(status().isOk());
        postJson("/unblock", "{\"reason\":\"Falsa alarma\",\"performedBy\":\"antifraude\"}")
                .andExpect(status().isOk());
        postJson("/cancel", "{\"reason\":\"Baja\",\"performedBy\":\"cliente\"}")
                .andExpect(status().isOk());

        assertThat(outbox).extracting(EnqueuedEvent::topic).containsExactly(
                KafkaTopics.CARD_APPROVED, KafkaTopics.CARD_ISSUED, KafkaTopics.CARD_ACTIVATED,
                KafkaTopics.CARD_BLOCKED, KafkaTopics.CARD_UNBLOCKED, KafkaTopics.CARD_CANCELLED);
        assertThat(outbox).extracting(EnqueuedEvent::key).containsOnly(cardId);

        CardApprovedEvent approved = eventAt(0);
        assertThat(approved.getReason()).isEqualTo("Cumple");
        assertThat(approved.getApprovedBy()).isEqualTo("admin");

        CardIssuedEvent issued = eventAt(1);
        assertThat(issued.getLastFourDigits()).isEqualTo("0366");
        assertThat(issued.getExpirationMonth()).isEqualTo(12);
        assertThat(issued.getIssuedBy()).isEqualTo("emisor");

        CardActivatedEvent activated = eventAt(2);
        assertThat(activated.getActivatedAt()).isNotNull();
        assertThat(activated.getActivatedBy()).isEqualTo("cliente");

        CardBlockedEvent blocked = eventAt(3);
        assertThat(blocked.getBlockType()).isEqualTo("FRAUD");
        assertThat(blocked.getBlockedAt()).isNotNull();
        assertThat(blocked.getBlockedBy()).isEqualTo("antifraude");

        CardUnblockedEvent unblocked = eventAt(4);
        assertThat(unblocked.getReason()).isEqualTo("Falsa alarma");

        CardCancelledEvent cancelled = eventAt(5);
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(cancelled.getCancelledBy()).isEqualTo("cliente");
    }

    @Test
    void shouldEnqueueExpiredEventAndRecordWhenItExpired() throws Exception {
        putJson("/status", "{\"status\":\"APPROVED\"}").andExpect(status().isOk());
        putJson("/status", "{\"status\":\"ISSUED\"}").andExpect(status().isOk());
        putJson("/status", "{\"status\":\"ACTIVE\"}").andExpect(status().isOk());
        putJson("/status", "{\"status\":\"EXPIRED\",\"reason\":\"Vencimiento\",\"performedBy\":\"batch\"}")
                .andExpect(status().isOk());

        assertThat(outbox).extracting(EnqueuedEvent::topic).endsWith(KafkaTopics.CARD_EXPIRED);
        CardExpiredEvent expired = eventAt(outbox.size() - 1);
        assertThat(expired.getCardId()).isEqualTo(cardId);
        assertThat(expired.getReason()).isEqualTo("Vencimiento");
        assertThat(expired.getExpiredBy()).isEqualTo("batch");
        assertThat(expired.getExpiredAt()).isNotNull();
        assertThat(storedCard().getExpiredAt()).isEqualTo(expired.getExpiredAt());
    }

    @Test
    void shouldEnqueueRejectedEvent() throws Exception {
        putJson("/status", "{\"status\":\"REJECTED\",\"reason\":\"No califica\",\"performedBy\":\"admin\"}")
                .andExpect(status().isOk());

        assertThat(outbox).extracting(EnqueuedEvent::topic).containsExactly(KafkaTopics.CARD_REJECTED);
        CardRejectedEvent rejected = eventAt(0);
        assertThat(rejected.getRejectedBy()).isEqualTo("admin");
    }

    @Test
    void shouldUseSystemAsAuthorWhenNoneIsInformed() throws Exception {
        putJson("/status", "{\"status\":\"APPROVED\"}").andExpect(status().isOk());

        CardApprovedEvent approved = eventAt(0);
        assertThat(approved.getApprovedBy()).isEqualTo("system");
    }

    @Test
    void shouldTreatActiveAfterBlockAsUnblockInStatusEndpoint() throws Exception {
        putJson("/status", "{\"status\":\"APPROVED\"}").andExpect(status().isOk());
        putJson("/status", "{\"status\":\"ISSUED\"}").andExpect(status().isOk());
        putJson("/status", "{\"status\":\"ACTIVE\"}").andExpect(status().isOk());
        putJson("/status", "{\"status\":\"BLOCKED\"}").andExpect(status().isOk());
        putJson("/status", "{\"status\":\"ACTIVE\"}").andExpect(status().isOk());

        assertThat(outbox).extracting(EnqueuedEvent::topic).endsWith(
                KafkaTopics.CARD_ACTIVATED, KafkaTopics.CARD_BLOCKED, KafkaTopics.CARD_UNBLOCKED);
    }

    @Test
    void shouldNotEnqueueEventWhenTransitionIsRejected() throws Exception {
        putJson("/status", "{\"status\":\"ACTIVE\"}").andExpect(status().isUnprocessableEntity());
        postJson("/cancel", "{\"reason\":\"x\"}").andExpect(status().isUnprocessableEntity());
        postJson("/block", "{\"reason\":\"x\"}").andExpect(status().isUnprocessableEntity());

        assertThat(outbox).isEmpty();
    }

    @Test
    void shouldNeverPublishSensitiveData() throws Exception {
        putJson("/status", "{\"status\":\"APPROVED\"}").andExpect(status().isOk());
        putJson("/card-data", VALID_CARD_DATA.formatted(LocalDate.now().getYear() + 2)).andExpect(status().isOk());
        putJson("/status", "{\"status\":\"ISSUED\"}").andExpect(status().isOk());

        String json = new KafkaProducerConfig().objectMapper().writeValueAsString(eventAt(1));

        assertThat(json).doesNotContain("4532015112830366").doesNotContain("4821")
                .doesNotContain("cvv").doesNotContain("pin");
    }

    @SuppressWarnings("unchecked")
    private <T extends DomainEvent> T eventAt(int index) {
        return (T) outbox.get(index).event();
    }

    // ========== POST /cancel ==========

    @Test
    void shouldCancelApprovedCardStoringReasonAndDate() throws Exception {
        approve();

        postJson("/cancel", "{\"reason\":\"Cliente solicitó cancelación\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Card card = storedCard();
        assertThat(card.getStatus()).isEqualTo(CardStatus.CANCELLED);
        assertThat(card.getCancelledReason()).isEqualTo("Cliente solicitó cancelación");
        assertThat(card.getCancelledAt()).isNotNull();
    }

    @Test
    void shouldReturn422WhenCancellingRequestedCard() throws Exception {
        postJson("/cancel", "{\"reason\":\"x\"}").andExpect(status().isUnprocessableEntity());

        assertThat(storedCard().getStatus()).isEqualTo(CardStatus.REQUESTED);
    }

    @Test
    void shouldReturn400WhenCancelReasonIsMissing() throws Exception {
        approve();

        postJson("/cancel", "{}").andExpect(status().isBadRequest());
    }

    // ========== Helpers ==========

    private void approve() throws Exception {
        putJson("/status", "{\"status\":\"APPROVED\"}").andExpect(status().isOk());
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/cards/" + cardId + path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions putJson(String path, String body) throws Exception {
        return mockMvc.perform(put("/api/v1/cards/" + cardId + path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Card storedCard() {
        return repository.findById(CardId.of(cardId)).orElseThrow();
    }

    private static Card newCard() {
        CardHolder holder = CardHolder.create(
                "Juan",
                "Pérez",
                "12345678",
                "juan@example.com",
                "+5491123456789",
                LocalDate.of(1990, 1, 1)
        );
        return Card.create(ClientId.of("12345678"), holder, CardType.VISA, CardCategory.CLASSIC);
    }

    private static class InMemoryCardRepository implements CardRepository {

        private final Map<String, Card> cards = new HashMap<>();

        @Override
        public Card save(Card card) {
            cards.put(card.getId().getValue(), card);
            return card;
        }

        @Override
        public Optional<Card> findById(CardId cardId) {
            return Optional.ofNullable(cards.get(cardId.getValue()));
        }

        @Override
        public List<Card> findByClientId(ClientId clientId) {
            return cards.values().stream().filter(c -> c.getClientId().equals(clientId)).toList();
        }

        @Override
        public Optional<Card> findByLastFourDigitsAndClientId(String lastFourDigits, ClientId clientId) {
            return Optional.empty();
        }

        @Override
        public Optional<Card> findByCardNumber(String encryptedCardNumber) {
            return Optional.empty();
        }

        @Override
        public List<Card> lockExpiredBatch(java.time.YearMonth currentMonth, int limit) {
            throw new UnsupportedOperationException("not used by the REST endpoints");
        }

        @Override
        public void delete(CardId cardId) {
            cards.remove(cardId.getValue());
        }

        @Override
        public boolean existsById(CardId cardId) {
            return cards.containsKey(cardId.getValue());
        }
    }
}
