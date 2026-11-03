package com.unisono.card.payment.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Vista de solo lectura de la tabla merchants (V4). La administra el módulo de comercios;
 * payment-processing solo valida que el comercio exista y esté activo.
 */
@Entity
@Immutable
@Table(name = "merchants")
@Getter
@NoArgsConstructor
public class MerchantEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "status", nullable = false)
    private String status;
}
