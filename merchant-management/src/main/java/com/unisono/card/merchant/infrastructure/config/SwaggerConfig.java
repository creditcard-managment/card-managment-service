package com.unisono.card.merchant.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI/Swagger para documentación de la API
 */
@Configuration
public class SwaggerConfig {

    @Value("${application.name:Merchant Management Service}")
    private String applicationName;

    @Value("${application.version:1.0.0}")
    private String applicationVersion;

    @Value("${application.description:Microservicio de gestión de comercios para sistema de tarjetas de crédito}")
    private String applicationDescription;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName)
                        .version(applicationVersion)
                        .description(applicationDescription)
                        .contact(new Contact()
                                .name("Banco Credicoop - Equipo de Desarrollo")
                                .email("desarrollo@credicoop.com.ar")
                                .url("https://www.bancocredicoop.coop"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://www.bancocredicoop.coop/licencia")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://api-staging.credicoop.com.ar")
                                .description("Staging Server"),
                        new Server()
                                .url("https://api.credicoop.com.ar")
                                .description("Production Server")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token authentication")));
    }
}
