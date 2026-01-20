package com.unisono.card.merchant.domain.model.fees;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Identificador único de estructura de fees
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FeeStructureId implements Serializable {

    private String value;

    public static FeeStructureId generate() {
        return new FeeStructureId(UUID.randomUUID().toString());
    }

    public static FeeStructureId of(String value) {
        Objects.requireNonNull(value, "FeeStructureId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("FeeStructureId value cannot be empty");
        }
        return new FeeStructureId(value.trim());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FeeStructureId)) return false;
        FeeStructureId that = (FeeStructureId) o;
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
