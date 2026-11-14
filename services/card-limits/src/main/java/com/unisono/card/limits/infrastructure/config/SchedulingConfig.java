package com.unisono.card.limits.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas: CreditLimitOutboxRelay (publicación) y CreditLimitOutboxCleaner (limpieza)
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
