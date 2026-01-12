package com.unisono.card.payment.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * RestTemplate configuration for external service calls
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setBufferRequestBody(false);
                    return new BufferingClientHttpRequestFactory(factory);
                })
                .interceptors(loggingInterceptor())
                .build();
    }

    /**
     * Logging interceptor for REST calls
     */
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            log.debug("REST Request: {} {}", request.getMethod(), request.getURI());

            var response = execution.execute(request, body);

            log.debug("REST Response: {} - Status: {}",
                    request.getURI(), response.getStatusCode());

            return response;
        };
    }
}
