package com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.entity;

import com.unisono.card.merchant.domain.model.terminal.TerminalType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

/**
 * JPA Entity para POS Terminal
 */
@Entity
@DiscriminatorValue("POS")
@Getter
@Setter
@NoArgsConstructor
public class POSTerminalEntity extends PaymentTerminalEntity {

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Column(name = "has_contactless_reader")
    private boolean hasContactlessReader;

    @Column(name = "has_chip_reader")
    private boolean hasChipReader;

    @Column(name = "has_magnetic_stripe_reader")
    private boolean hasMagneticStripeReader;

    @Column(name = "has_pin_pad")
    private boolean hasPinPad;

    @Column(name = "has_printer")
    private boolean hasPrinter;

    @Override
    public TerminalType getTerminalType() {
        return TerminalType.POS;
    }
}
