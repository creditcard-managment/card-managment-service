package com.unisono.card.data.infrastructure.adapter.output.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.data.domain.model.CardCategory;
import com.unisono.card.data.domain.model.CardType;
import com.unisono.card.data.domain.port.output.EncryptionPort;
import com.unisono.card.data.domain.service.CardEventEmitter;
import com.unisono.card.data.domain.service.CardExpirationService;
import com.unisono.card.data.infrastructure.adapter.output.persistence.adapter.CardRepositoryAdapter;
import com.unisono.card.data.infrastructure.adapter.output.persistence.adapter.OutboxEventAdapter;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.CardEntity;
import com.unisono.card.data.infrastructure.adapter.output.persistence.mapper.CardEntityMapper;
import com.unisono.card.data.infrastructure.adapter.output.persistence.repository.JpaCardRepository;
import com.unisono.card.data.infrastructure.adapter.output.persistence.repository.JpaOutboxEventRepository;
import com.unisono.card.events.config.EventSerializationConfig;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.infrastructure.security.BCryptHashingService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Base de los tests de persistencia de card-data contra un PostgreSQL real (Testcontainers). El esquema lo genera
 * Hibernate a partir de las entidades: prueba consultas, locks y mapeos, no las migraciones (viven en
 * card-management-database). Cada test maneja sus transacciones. Sin Docker, los tests se saltean.
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CardDataPostgresTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class CardDataPostgresTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        // Un solo contenedor para todas las clases de test (se detiene al terminar la JVM)
        POSTGRES.start();
    }

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
    JpaCardRepository cards;

    @Autowired
    JpaOutboxEventRepository outbox;

    @Autowired
    CardRepositoryAdapter adapter;

    @Autowired
    PlatformTransactionManager transactionManager;

    @MockBean
    EncryptionPort encryptionPort;

    @MockBean
    BCryptHashingService hashingService;

    TransactionTemplate tx;

    @BeforeEach
    void clean() {
        tx = new TransactionTemplate(transactionManager);
        outbox.deleteAll();
        cards.deleteAll();
    }

    CardStatus status(String id) {
        return cards.findById(id).orElseThrow().getStatus();
    }

    /** Tarjeta sin PAN ni CVV (el cifrado está mockeado). */
    String save(CardStatus status, Integer month, Integer year) {
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
