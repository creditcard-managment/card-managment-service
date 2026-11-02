package com.unisono.card.payment.domain.model;

import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Value Object representing an authorization code for a transaction
 */
@Getter
@EqualsAndHashCode
public class AuthorizationCode implements Serializable {

    private final String code;
    private final LocalDateTime authorizedAt;
    private final String responseCode; // Código de respuesta (00 = aprobado, etc.)

    private AuthorizationCode(String code, LocalDateTime authorizedAt, String responseCode) {
        if (StringUtils.isBlank(code)) {
            throw new InvalidValueObjectException("Authorization code cannot be blank");
        }
        if (authorizedAt == null) {
            throw new InvalidValueObjectException("Authorization timestamp cannot be null");
        }
        if (StringUtils.isBlank(responseCode)) {
            throw new InvalidValueObjectException("Response code cannot be blank");
        }

        this.code = code;
        this.authorizedAt = authorizedAt;
        this.responseCode = responseCode;
    }

    public static AuthorizationCode approved(String code) {
        return new AuthorizationCode(code, LocalDateTime.now(), "00");
    }

    public static AuthorizationCode declined(String responseCode) {
        return new AuthorizationCode("DECLINED-" + UUID.randomUUID().toString().substring(0, 6),
                LocalDateTime.now(), responseCode);
    }

    public static AuthorizationCode of(String code, LocalDateTime authorizedAt, String responseCode) {
        return new AuthorizationCode(code, authorizedAt, responseCode);
    }

    public boolean isApproved() {
        return "00".equals(responseCode);
    }

    @Override
    public String toString() {
        return String.format("%s (response: %s)", code, responseCode);
    }
}
