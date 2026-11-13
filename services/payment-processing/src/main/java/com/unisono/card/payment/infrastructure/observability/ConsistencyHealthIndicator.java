package com.unisono.card.payment.infrastructure.observability;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

/**
 * Estado de la consistencia con card-limits y Kafka en /actuator/health ("paymentConsistency").
 * <p>
 * {@code DEGRADED} (HTTP 200, ver management.endpoint.health.status) cuando hay operaciones de crédito trabadas,
 * eventos del outbox atascados o fallidos: el servicio sigue atendiendo, pero hay datos que revisar. No pasa a
 * {@code DOWN} para que un orquestador no lo reinicie por un problema que un reinicio no arregla.
 */
@Component("paymentConsistency")
@RequiredArgsConstructor
public class ConsistencyHealthIndicator implements HealthIndicator {

    public static final Status DEGRADED = new Status("DEGRADED");

    private final ConsistencyMonitor monitor;

    @Override
    public Health health() {
        boolean degraded = monitor.creditStuck() > 0 || monitor.outboxStalled() > 0 || monitor.outboxFailed() > 0;
        return Health.status(degraded ? DEGRADED : Status.UP)
                .withDetail("creditOperationsPending", monitor.creditPending())
                .withDetail("creditOperationsStuck", monitor.creditStuck())
                .withDetail("outboxPending", monitor.outboxPending())
                .withDetail("outboxStalled", monitor.outboxStalled())
                .withDetail("outboxFailed", monitor.outboxFailed())
                .build();
    }
}
