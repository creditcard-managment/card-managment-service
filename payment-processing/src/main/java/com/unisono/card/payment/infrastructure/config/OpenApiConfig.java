package com.unisono.card.payment.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI paymentProcessingOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:" + serverPort);
        server.setDescription("Payment Processing API - Development");

        Contact contact = new Contact();
        contact.setName("Card Management Team");
        contact.setEmail("card-team@unisono.com");

        License license = new License();
        license.setName("Proprietary");

        Info info = new Info()
                .title("Payment Processing API")
                .version("1.0.0")
                .description("API for processing card payment transactions with fraud detection, authorization, and settlement")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}
