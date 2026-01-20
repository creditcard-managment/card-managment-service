package com.unisono.card.merchant.domain.model.terminal;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

/**
 * Configuración de una terminal de pago
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TerminalConfiguration {

    private Set<String> acceptedCardBrands; // Visa, Mastercard, AMEX, etc.
    private BigDecimal maxTransactionAmount;
    private BigDecimal minTransactionAmount;
    private boolean allowRefunds;
    private boolean allowVoids;
    private boolean allowPartialApprovals;
    private boolean requireSignature;
    private BigDecimal signatureThreshold;
    private boolean requirePin;
    private Integer timeoutSeconds;
    private String currency;

    public static TerminalConfiguration standard(String currency) {
        return new TerminalConfiguration(
                Set.of("VISA", "MASTERCARD", "AMEX"),
                new BigDecimal("999999.99"),
                new BigDecimal("1.00"),
                true,
                true,
                false,
                true,
                new BigDecimal("1000.00"),
                true,
                30,
                currency
        );
    }

    public static TerminalConfiguration of(
            Set<String> acceptedCardBrands,
            BigDecimal maxTransactionAmount,
            BigDecimal minTransactionAmount,
            boolean allowRefunds,
            boolean allowVoids,
            boolean allowPartialApprovals,
            boolean requireSignature,
            BigDecimal signatureThreshold,
            boolean requirePin,
            Integer timeoutSeconds,
            String currency
    ) {
        Objects.requireNonNull(acceptedCardBrands, "Accepted card brands cannot be null");
        Objects.requireNonNull(maxTransactionAmount, "Max transaction amount cannot be null");
        Objects.requireNonNull(minTransactionAmount, "Min transaction amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        validateAmounts(minTransactionAmount, maxTransactionAmount);
        validateTimeout(timeoutSeconds);

        return new TerminalConfiguration(
                acceptedCardBrands,
                maxTransactionAmount,
                minTransactionAmount,
                allowRefunds,
                allowVoids,
                allowPartialApprovals,
                requireSignature,
                signatureThreshold != null ? signatureThreshold : new BigDecimal("1000.00"),
                requirePin,
                timeoutSeconds != null ? timeoutSeconds : 30,
                currency.toUpperCase()
        );
    }

    private static void validateAmounts(BigDecimal min, BigDecimal max) {
        if (min.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Minimum amount must be greater than zero");
        }
        if (max.compareTo(min) < 0) {
            throw new IllegalArgumentException("Maximum amount must be greater than or equal to minimum");
        }
    }

    private static void validateTimeout(Integer timeout) {
        if (timeout != null && (timeout < 5 || timeout > 300)) {
            throw new IllegalArgumentException("Timeout must be between 5 and 300 seconds");
        }
    }

    public boolean isCardBrandAccepted(String cardBrand) {
        return acceptedCardBrands.contains(cardBrand.toUpperCase());
    }

    public boolean isAmountValid(BigDecimal amount) {
        return amount.compareTo(minTransactionAmount) >= 0 &&
                amount.compareTo(maxTransactionAmount) <= 0;
    }

    public boolean requiresSignatureFor(BigDecimal amount) {
        return requireSignature && amount.compareTo(signatureThreshold) >= 0;
    }

    public TerminalConfiguration withMaxAmount(BigDecimal newMax) {
        return TerminalConfiguration.of(
                this.acceptedCardBrands,
                newMax,
                this.minTransactionAmount,
                this.allowRefunds,
                this.allowVoids,
                this.allowPartialApprovals,
                this.requireSignature,
                this.signatureThreshold,
                this.requirePin,
                this.timeoutSeconds,
                this.currency
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalConfiguration)) return false;
        TerminalConfiguration that = (TerminalConfiguration) o;
        return Objects.equals(acceptedCardBrands, that.acceptedCardBrands) &&
                Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(acceptedCardBrands, currency);
    }

    @Override
    public String toString() {
        return "TerminalConfiguration{" +
                "currency='" + currency + '\'' +
                ", maxAmount=" + maxTransactionAmount +
                ", acceptedBrands=" + acceptedCardBrands +
                '}';
    }
}
