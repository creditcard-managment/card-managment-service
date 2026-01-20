package com.unisono.card.merchant.domain.model.fees;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Interchange Fee (fee cobrado por el banco emisor)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InterchangeFee {

    private BigDecimal rate; // Tasa de interchange (%)
    private BigDecimal fixedAmount;
    private String cardBrand;

    public static InterchangeFee of(BigDecimal rate, BigDecimal fixedAmount, String cardBrand) {
        Objects.requireNonNull(rate, "Interchange rate cannot be null");
        Objects.requireNonNull(cardBrand, "Card brand cannot be null");

        validateRate(rate);

        return new InterchangeFee(
                rate,
                fixedAmount != null ? fixedAmount : BigDecimal.ZERO,
                cardBrand.toUpperCase()
        );
    }

    public static InterchangeFee standard(String cardBrand) {
        // Tasas estándar aproximadas
        BigDecimal standardRate = switch (cardBrand.toUpperCase()) {
            case "VISA" -> new BigDecimal("1.51");
            case "MASTERCARD" -> new BigDecimal("1.58");
            case "AMEX" -> new BigDecimal("2.50");
            default -> new BigDecimal("1.75");
        };

        return new InterchangeFee(
                standardRate,
                new BigDecimal("0.10"),
                cardBrand.toUpperCase()
        );
    }

    private static void validateRate(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("10")) > 0) {
            throw new IllegalArgumentException("Interchange rate must be between 0 and 10%");
        }
    }

    public BigDecimal calculateFee(BigDecimal transactionAmount) {
        Objects.requireNonNull(transactionAmount, "Transaction amount cannot be null");

        BigDecimal percentageFee = transactionAmount
                .multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        return percentageFee.add(fixedAmount).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InterchangeFee)) return false;
        InterchangeFee that = (InterchangeFee) o;
        return Objects.equals(rate, that.rate) &&
                Objects.equals(cardBrand, that.cardBrand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rate, cardBrand);
    }

    @Override
    public String toString() {
        return "InterchangeFee{" +
                "rate=" + rate +
                "%, fixed=" + fixedAmount +
                ", brand='" + cardBrand + '\'' +
                '}';
    }
}
