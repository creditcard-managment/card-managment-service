package com.unisono.card.limits;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.unisono.card.limits",   // ✅ Escanear card-limits
        "com.unisono.card.shared"    // ✅ Escanear card-shared
})
public class CardLimitsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardLimitsApplication.class, args);
    }
}