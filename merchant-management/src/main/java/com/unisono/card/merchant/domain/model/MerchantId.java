package com.unisono.card.merchant.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Value Object para identificar de manera única a un Merchant
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MerchantId implements Serializable {

    private String value;

    public static MerchantId generate() {
        return new MerchantId(UUID.randomUUID().toString());
    }

    public static MerchantId of(String value) {
        Objects.requireNonNull(value, "MerchantId value cannot be null");

        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("MerchantId value cannot be empty");
        }

        return new MerchantId(value.trim());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MerchantId)) return false;
        MerchantId that = (MerchantId) o;
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
