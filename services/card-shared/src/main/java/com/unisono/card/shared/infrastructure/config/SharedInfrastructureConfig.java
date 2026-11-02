package com.unisono.card.shared.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de infraestructura compartida
 * Habilita el escaneo de componentes en card-shared
 */
@Configuration
@ComponentScan(basePackages = "com.unisono.card.shared.infrastructure")
public class SharedInfrastructureConfig {
}
