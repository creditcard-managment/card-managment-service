package com.unisono.card.limits.infrastructure.adapter.output.persistence;

import com.unisono.card.events.config.EventSerializationConfig;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CreditLimitConsumedEvent;
import com.unisono.card.events.domain.CreditLimitReleasedEvent;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.limits.domain.port.input.RecalculateAvailableCreditUseCase.ConsumeCreditCommand;
import com.unisono.card.limits.domain.port.input.RecalculateAvailableCreditUseCase.ReleaseCreditCommand;
import com.unisono.card.limits.domain.port.output.CardValidationPort;
import com.unisono.card.limits.domain.port.output.EventOutboxPort;
import com.unisono.card.limits.domain.service.CreditLimitEventEmitter;
import com.unisono.card.limits.domain.service.CreditLimitManagementService;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.adapter.CreditLimitOutboxEventAdapter;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.adapter.CreditLimitRepositoryAdapter;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.adapter.CreditOperationRepositoryAdapter;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.entity.CreditLimitEntity;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.entity.CreditLimitOutboxEventEntity;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.mapper.CreditLimitEntityMapper;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.repository.JpaCreditLimitOutboxEventRepository;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.repository.JpaCreditLimitRepository;
import com.unisono.card.events.domain.CreditLimitUpdatedEvent;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Outbox de card-limits contra un PostgreSQL real (Testcontainers): el evento se guarda en la misma transacción
 * que el cambio del límite y se revierte con él. El esquema lo genera Hibernate a partir de las entidades (las
 * migraciones viven en card-management-database). Sin Docker, los tests se saltean.
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CreditLimitOutboxPersistenceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CreditLimitOutboxPersistenceTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Configuration
    @EntityScan(basePackageClasses = CreditLimitEntity.class)
    @EnableJpaRepositories(basePackageClasses = JpaCreditLimitRepository.class)
    @Import({CreditLimitRepositoryAdapter.class, CreditOperationRepositoryAdapter.class, CreditLimitEntityMapper.class,
            CreditLimitOutboxEventAdapter.class, CreditLimitEventEmitter.class, CreditLimitManagementService.class})
    static class Config {
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private CreditLimitManagementService service;

    @Autowired
    private CreditLimitRepositoryAdapter creditLimits;

    @Autowired
    private JpaCreditLimitOutboxEventRepository outbox;

    @Autowired
    private EventOutboxPort eventOutbox;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private CardValidationPort cardValidationPort;

    private CardId cardId;
    private String creditLimitId;

    @BeforeEach
    void setUp() {
        outbox.deleteAll();
        when(cardValidationPort.isCardActive(any())).thenReturn(true);
        cardId = CardId.of(UUID.randomUUID().toString());
        creditLimitId = new TransactionTemplate(transactionManager).execute(s ->
                creditLimits.save(CreditLimit.assign(cardId, Money.ofARS(500_000), 0.3, "test")).getId());
    }

    @Test
    void consumptionAndItsEventAreSavedTogether() throws Exception {
        service.consumeForPurchase(new ConsumeCreditCommand(creditLimitId, Money.ofARS(20_000), "compra", "op-1"));

        List<CreditLimitOutboxEventEntity> events = outbox.findAll();
        assertThat(events).hasSize(1);
        CreditLimitOutboxEventEntity row = events.get(0);
        assertThat(row.getTopic()).isEqualTo(KafkaTopics.CREDIT_LIMIT_CONSUMED);
        assertThat(row.getAggregateId()).isEqualTo(cardId.getValue());
        assertThat(row.getEventType()).isEqualTo("CreditLimitConsumedEvent");
        assertThat(row.getPublishedAt()).isNull();

        CreditLimitConsumedEvent event = EventSerializationConfig.objectMapper()
                .readValue(row.getPayload(), CreditLimitConsumedEvent.class);
        assertThat(event.getEventId()).isEqualTo(row.getEventId());
        assertThat(event.getOperationId()).isEqualTo("op-1");
        assertThat(event.getAmount()).isEqualByComparingTo("20000");
        assertThat(event.getAvailableAmount()).isEqualByComparingTo("480000");
    }

    @Test
    void rejectedConsumptionLeavesNoEvent() {
        assertThatThrownBy(() -> service.consumeForPurchase(
                new ConsumeCreditCommand(creditLimitId, Money.ofARS(900_000), "compra", "op-2")))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(outbox.count()).isZero();
    }

    @Test
    void paymentIsPublishedAsReleased() throws Exception {
        service.consumeForPurchase(new ConsumeCreditCommand(creditLimitId, Money.ofARS(20_000), "compra", "op-3"));
        service.releaseCredit(new ReleaseCreditCommand(creditLimitId, Money.ofARS(5_000), "pago"));

        CreditLimitOutboxEventEntity payment = outbox.findAll().stream()
                .filter(e -> e.getTopic().equals(KafkaTopics.CREDIT_LIMIT_RELEASED)).findFirst().orElseThrow();
        CreditLimitReleasedEvent event = EventSerializationConfig.objectMapper()
                .readValue(payment.getPayload(), CreditLimitReleasedEvent.class);
        assertThat(event.getOperationType()).isEqualTo("PAYMENT");
        assertThat(event.getConsumedAmount()).isEqualByComparingTo("15000");
    }

    @Test
    void limitChangeIsPublishedAsUpdated() throws Exception {
        service.increasePurchaseLimit(new com.unisono.card.limits.domain.port.input.UpdateCreditLimitUseCase
                .IncreasePurchaseLimitCommand(creditLimitId, Money.ofARS(100_000), "Aumento", "analista"));

        CreditLimitOutboxEventEntity row = outbox.findAll().get(0);
        assertThat(row.getTopic()).isEqualTo(KafkaTopics.CREDIT_LIMIT_UPDATED);
        CreditLimitUpdatedEvent event = EventSerializationConfig.objectMapper()
                .readValue(row.getPayload(), CreditLimitUpdatedEvent.class);
        assertThat(event.getNewAmount()).isEqualByComparingTo("600000");
    }

    @Test
    void eventsCannotBeEnqueuedOutsideATransaction() {
        assertThatThrownBy(() -> eventOutbox.enqueue(KafkaTopics.CREDIT_LIMIT_CONSUMED, cardId.getValue(),
                new CreditLimitConsumedEvent(cardId.getValue(), creditLimitId, null, "PURCHASE", null, null, null, "ARS")))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
