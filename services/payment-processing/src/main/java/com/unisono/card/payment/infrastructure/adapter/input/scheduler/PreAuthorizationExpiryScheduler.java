package com.unisono.card.payment.infrastructure.adapter.input.scheduler;

import com.unisono.card.payment.application.service.PreAuthorizationExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Runs the pre-authorization expiry periodically.
 * Disabled with payment.pre-authorization.expiry.enabled=false
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.pre-authorization.expiry.enabled", havingValue = "true", matchIfMissing = true)
public class PreAuthorizationExpiryScheduler {

    private final PreAuthorizationExpiryService expiryService;

    @Scheduled(
            initialDelayString = "${payment.pre-authorization.expiry.initial-delay-ms:60000}",
            fixedDelayString = "${payment.pre-authorization.expiry.interval-ms:900000}"
    )
    public void expireOverduePreAuthorizations() {
        try {
            expiryService.expireOverdue();
        } catch (Exception ex) {
            log.error("Pre-authorization expiry run failed", ex);
        }
    }
}
