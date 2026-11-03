package com.unisono.card.payment.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Vista de solo lectura de la tabla payment_terminals (V4): terminales POS y cajeros con su comercio.
 */
@Entity
@Immutable
@Table(name = "payment_terminals")
@Getter
@NoArgsConstructor
public class PaymentTerminalEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "merchant_id", nullable = false, length = 36)
    private String merchantId;

    @Column(name = "terminal_type", nullable = false)
    private String terminalType;

    @Column(name = "status", nullable = false)
    private String status;
}
