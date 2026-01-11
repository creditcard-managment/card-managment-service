package com.unisono.card.payment.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA Configuration for Payment Processing module
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.unisono.card.payment.infrastructure.adapter.output.persistence.repository"
)
@EnableJpaAuditing
@EnableTransactionManagement
public class JpaConfig {
    // Configuration is handled by annotations
}
