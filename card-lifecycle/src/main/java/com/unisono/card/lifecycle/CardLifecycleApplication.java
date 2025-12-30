package com.unisono.card.lifecycle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Card Lifecycle Service - Microservicio de gestión del ciclo de vida de tarjetas
 *
 * @author Juan Pérez
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@ComponentScan(basePackages = {
        "com.unisono.card.lifecycle",
        "com.unisono.card.shared"  // ✅ Para encontrar EncryptionService y HashingService
})
public class CardLifecycleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardLifecycleApplication.class, args);
    }
}
