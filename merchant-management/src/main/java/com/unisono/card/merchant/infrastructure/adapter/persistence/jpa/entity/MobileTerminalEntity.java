package com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.entity;

import com.unisono.card.merchant.domain.model.terminal.TerminalType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity para Mobile Terminal
 */
@Entity
@DiscriminatorValue("MOBILE")
@Getter
@Setter
@NoArgsConstructor
public class MobileTerminalEntity extends PaymentTerminalEntity {

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "operating_system", length = 50)
    private String operatingSystem;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "has_external_reader")
    private boolean hasExternalReader;

    @Column(name = "supports_bluetooth")
    private boolean supportsBluetooth;

    @Column(name = "supports_nfc")
    private boolean supportsNFC;

    @Column(name = "reader_serial_number", length = 50)
    private String readerSerialNumber;

    @Override
    public TerminalType getTerminalType() {
        return TerminalType.MOBILE;
    }
}
