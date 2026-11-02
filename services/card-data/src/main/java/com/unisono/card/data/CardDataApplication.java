package com.unisono.card.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Card Data Service - Microservicio de gestión de datos de tarjetas
 *
 * @author Juan Pérez
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@ComponentScan(basePackages = {
        "com.unisono.card.data",           // ✅ Escanear card-data
        "com.unisono.card.shared"          // ✅ Escanear card-shared (para AesEncryptionService)
})
public class CardDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDataApplication.class, args);
    }
}
