package com.unisono.card.payment.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita los jobs programados: relay y limpieza del outbox, reconciliación de la saga de crédito
 * y expiración de preautorizaciones
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
