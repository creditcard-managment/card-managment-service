package com.unisono.card.payment.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Main configuration for payment processing module
 */
@Configuration
@ComponentScan(basePackages = {
        "com.unisono.card.payment.domain.service",
        "com.unisono.card.payment.application.service",
        "com.unisono.card.payment.application.mapper",
        "com.unisono.card.payment.infrastructure"
})
public class PaymentProcessingConfig {
    // Component scanning configuration
}

