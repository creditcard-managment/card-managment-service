package com.unisono.card.merchant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot Application class for Merchant Management Service
 */
@SpringBootApplication
@EnableAsync
public class MerchantManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(MerchantManagementApplication.class, args);
    }
}
