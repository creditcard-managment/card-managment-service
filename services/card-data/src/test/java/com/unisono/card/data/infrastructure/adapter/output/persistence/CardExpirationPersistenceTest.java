package com.unisono.card.data.infrastructure.adapter.output.persistence;

import com.unisono.card.data.domain.model.CardCategory;
import com.unisono.card.data.domain.model.CardType;
import com.unisono.card.data.domain.port.output.EncryptionPort;
import com.unisono.card.data.domain.service.CardEventEmitter;
import com.unisono.card.data.domain.service.CardExpirationService;
import com.unisono.card.data.infrastructure.adapter.output.persistence.adapter.CardRepositoryAdapter;
import com.unisono.card.data.infrastructure.adapter.output.persistence.adapter.OutboxEventAdapter;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.CardEntity;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.OutboxEventEntity;
import com.unisono.card.data.infrastructure.adapter.output.persistence.mapper.CardEntityMapper;
import com.unisono.card.data.infrastructure.adapter.output.persistence.repository.JpaCardRepository;
import com.unisono.card.data.infrastructure.adapter.output.persistence.repository.JpaOutboxEventRepository;
import com.unisono.card.events.config.EventSerializationConfig;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.infrastructure.security.BCryptHashingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vencimiento contra un PostgreSQL real (Testcontainers): la consulta por fecha y estado, FOR UPDATE SKIP LOCKED
 * y el card.expired en el outbox en la misma transacción. El esquema lo genera Hibernate a partir de las
 * entidades (las migraciones viven en card-management-database). Sin Docker, los tests se saltean.
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CardExpirationPersistenceTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    private static final YearMonth DECEMBER_2026 = YearMonth.of(2026, 12);

    /**
     * Configuración propia: CardDataApplication declara un @ComponentScan explícito que levantaría toda la app
     */
    @Configuration
    @EntityScan(basePackageClasses = CardEntity.class)
    @EnableJpaRepositories(basePackageClasses = JpaCardRepository.class)
    @Import({CardRepositoryAdapter.class, CardEntityMapper.class, OutboxEventAdapter.class, CardEventEmitter.class,
            CardExpirationService.class})
    static class Config {
        @Bean
        ObjectMapper objectMapper() {
            return EventSerializationConfig.objectMapper();
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private JpaCardRepository cards;

    @Autowired
    private JpaOutboxEventRepository outbox;

    @Autowired
    private CardRepositoryAdapter adapter;

    @Autowired
    private CardExpirationService service;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private EncryptionPort encryptionPort;

    @MockBean
    private BCryptHashingService hashingService;

    private TransactionTemplate tx;

    @BeforeEach
    void clean() {
        tx = new TransactionTemplate(transactionManager);
        outbox.deleteAll();
        cards.deleteAll();
    }

    @Test
    void locksOnlyCardsThatExpiredBeforeTheCurrentMonthAndCanExpire() {
        String active = save(CardStatus.ACTIVE, 11, 2026);
        String issued = save(CardStatus.ISSUED, 1, 2025);
        String blocked = save(CardStatus.BLOCKED, 6, 2026);
        String fraud = save(CardStatus.BLOCKED_FRAUD, 12, 2025);
        save(CardStatus.ACTIVE, 12, 2026);             // vence a fin de mes: todavía válida
        save(CardStatus.ACTIVE, 1, 2027);
        save(CardStatus.CANCELLED, 1, 2025);           // estados finales o sin emitir: no vencen
        save(CardStatus.EXPIRED, 1, 2025);
        save(CardStatus.APPROVED, null, null);
        String deleted = save(CardStatus.ACTIVE, 1, 2025);
        cards.findById(deleted).ifPresent(e -> { e.markAsDeleted(); cards.save(e); });

        List<String> locked = tx.execute(s -> adapter.lockExpiredBatch(DECEMBER_2026, 100).stream()
                .map(card -> card.getId().getValue()).toList());

        assertThat(locked).containsExactlyInAnyOrder(active, issued, blocked, fraud);
    }

    @Test
    void expiresTheCardsAndEnqueuesCardExpiredInTheSameTransaction() {
        String active = save(CardStatus.ACTIVE, 11, 2026);
        String blocked = save(CardStatus.BLOCKED, 10, 2026);
        String valid = save(CardStatus.ACTIVE, 12, 2026);

        List<CardId> expired = service.expireBatch(DECEMBER_2026, 100);

        assertThat(expired).extracting(CardId::getValue).containsExactlyInAnyOrder(active, blocked);
        assertThat(status(active)).isEqualTo(CardStatus.EXPIRED);
        assertThat(status(blocked)).isEqualTo(CardStatus.EXPIRED);
        assertThat(status(valid)).isEqualTo(CardStatus.ACTIVE);
        assertThat(cards.findById(active).orElseThrow().getExpiredAt()).isNotNull();

        List<OutboxEventEntity> events = outbox.findAll();
        assertThat(events).extracting(OutboxEventEntity::getTopic).containsOnly(KafkaTopics.CARD_EXPIRED);
        assertThat(events).extracting(OutboxEventEntity::getAggregateId).containsExactlyInAnyOrder(active, blocked);

        // Una segunda pasada no encuentra nada
        assertThat(service.expireBatch(DECEMBER_2026, 100)).isEmpty();
    }

    @Test
    void cardsLockedByAnotherTransactionAreSkipped() throws Exception {
        String first = save(CardStatus.ACTIVE, 1, 2026);
        String second = save(CardStatus.ACTIVE, 2, 2026);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        // Otra instancia del job toma una tarjeta y mantiene la transacción abierta
        CompletableFuture<List<String>> other = CompletableFuture.supplyAsync(() -> tx.execute(s -> {
            List<String> ids = adapter.lockExpiredBatch(DECEMBER_2026, 1).stream()
                    .map(card -> card.getId().getValue()).toList();
            locked.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return ids;
        }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        List<String> mine = tx.execute(s -> adapter.lockExpiredBatch(DECEMBER_2026, 100).stream()
                .map(card -> card.getId().getValue()).toList());
        release.countDown();

        assertThat(other.get(10, TimeUnit.SECONDS)).hasSize(1);
        assertThat(mine).hasSize(1).doesNotContainAnyElementsOf(other.get());
        assertThat(List.of(mine.get(0), other.get().get(0))).containsExactlyInAnyOrder(first, second);
    }

    private CardStatus status(String id) {
        return cards.findById(id).orElseThrow().getStatus();
    }

    private String save(CardStatus status, Integer month, Integer year) {
        String id = UUID.randomUUID().toString();
        cards.save(CardEntity.builder()
                .id(id)
                .clientId("12345678")
                .holderFirstName("Juan")
                .holderLastName("Pérez")
                .holderDocumentNumber("12345678")
                .holderEmail("juan@example.com")
                .holderBirthDate(LocalDate.of(1990, 1, 1))
                .type(CardType.VISA)
                .category(CardCategory.CLASSIC)
                .status(status)
                .expirationMonth(month)
                .expirationYear(year)
                .build());
        return id;
    }
}
