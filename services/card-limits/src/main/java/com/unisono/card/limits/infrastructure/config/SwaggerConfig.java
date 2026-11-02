package com.unisono.card.limits.infrastructure.config;

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
 * Configuración de Swagger/OpenAPI
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8082}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:" + serverPort);
        server.setDescription("Development Server");

        Contact contact = new Contact();
        contact.setName("Unisono - Credit Limits Team");
        contact.setEmail("limits@unisono.com");

        License license = new License();
        license.setName("Proprietary");

        Info info = new Info()
                .title("Credit Limits Service API")
                .version("1.0.0")
                .description("API REST para gestión de límites de crédito - Módulo card-limits")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}
