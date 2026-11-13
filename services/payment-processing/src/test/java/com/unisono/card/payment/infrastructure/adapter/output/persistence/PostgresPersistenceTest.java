package com.unisono.card.payment.infrastructure.adapter.output.persistence;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.adapter.TransactionPersistenceAdapter;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.mapper.TransactionEntityMapper;
import com.unisono.card.payment.infrastructure.config.DatabaseConfig;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de los tests de persistencia contra un PostgreSQL real (Testcontainers), con el DatabaseConfig del servicio.
 * El esquema lo genera Hibernate a partir de las entidades: prueba consultas, locks y mapeos, no las migraciones
 * (esas viven en card-management-database). Sin Docker, los tests se saltean.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(classes = {DatabaseConfig.class, TransactionPersistenceAdapter.class, TransactionEntityMapper.class})
abstract class PostgresPersistenceTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        // Un solo contenedor para todas las clases de test (se detiene al terminar la JVM)
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @Autowired
    @Qualifier("paymentTransactionManager")
    PlatformTransactionManager transactionManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    TransactionTemplate tx;

    @BeforeEach
    void cleanDatabase() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> entityManager()
                .createNativeQuery("TRUNCATE transactions, payment_outbox_events, payment_credit_operations")
                .executeUpdate());
    }

    /**
     * EntityManager de la transacción en curso (la que abre {@link #tx})
     */
    jakarta.persistence.EntityManager entityManager() {
        return org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
    }
}
