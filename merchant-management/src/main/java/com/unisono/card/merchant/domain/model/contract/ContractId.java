package com.unisono.card.merchant.domain.model.contract;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Identificador único de contrato
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractId implements Serializable {

    private String value;

    public static ContractId generate() {
        return new ContractId(UUID.randomUUID().toString());
    }

    public static ContractId of(String value) {
        Objects.requireNonNull(value, "ContractId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("ContractId value cannot be empty");
        }
        return new ContractId(value.trim());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContractId)) return false;
        ContractId that = (ContractId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
