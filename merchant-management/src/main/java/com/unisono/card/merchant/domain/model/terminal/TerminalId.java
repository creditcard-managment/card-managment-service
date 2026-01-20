package com.unisono.card.merchant.domain.model.terminal;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Identificador único de terminal
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TerminalId implements Serializable {

    private String value;

    public static TerminalId generate() {
        return new TerminalId(UUID.randomUUID().toString());
    }

    public static TerminalId of(String value) {
        Objects.requireNonNull(value, "TerminalId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("TerminalId value cannot be empty");
        }
        return new TerminalId(value.trim());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalId)) return false;
        TerminalId that = (TerminalId) o;
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
