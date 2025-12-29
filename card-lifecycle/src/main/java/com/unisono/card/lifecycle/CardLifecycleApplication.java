package com.unisono.card.lifecycle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Card Lifecycle Service - Microservicio de gestión del ciclo de vida de tarjetas
 *
 * @author Juan Pérez
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
public class CardLifecycleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardLifecycleApplication.class, args);
    }
}
