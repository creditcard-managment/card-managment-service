package com.unisono.card.payment.infrastructure.adapter.output.event;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentOutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/**
 * Limpieza del outbox de pagos (payment_outbox_events)
 *
 * Borra los eventos publicados hace más de retention-days. Los eventos fallidos
 * (failed_at) se conservan para revisión manual y se informan en el log.
 */
@Slf4j
@Component
public class PaymentOutboxCleaner {

    private final JpaPaymentOutboxEventRepository repository;
    private final TransactionTemplate transactionTemplate;

    @Value("${payment.outbox.retention-days:7}")
    private int retentionDays;

    public PaymentOutboxCleaner(JpaPaymentOutboxEventRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(cron = "${payment.outbox.cleanup-cron:0 0 3 * * *}")
    public void cleanup() {
        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        Integer deleted = transactionTemplate.execute(status -> repository.deletePublishedBefore(before));

        log.info("🧹 Payment outbox: {} published event(s) older than {} days deleted", deleted, retentionDays);

        long failed = repository.countByFailedAtIsNotNull();
        if (failed > 0) {
            log.warn("☠️ Payment outbox: {} event(s) failed permanently and need manual review " +
                    "(payment_outbox_events.failed_at IS NOT NULL)", failed);
        }
    }
}
