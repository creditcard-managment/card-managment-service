package com.unisono.card.payment.domain.model;

import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import com.unisono.card.shared.domain.valueobject.CardNumber;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Value Object representing card data for payment processing (PCI compliant)
 * IMPORTANT: Never store complete card numbers or CVV
 */
@Getter
@EqualsAndHashCode
public class CardData implements Serializable {

    private final String cardNumberToken; // Tokenized card number (PCI compliance)
    private final String lastFourDigits;
    private final String cardholderName;
    private final ExpirationDate expirationDate;

    private CardData(String cardNumberToken, String lastFourDigits,
                     String cardholderName, ExpirationDate expirationDate) {
        if (StringUtils.isBlank(cardNumberToken)) {
            throw new InvalidValueObjectException("Card number token cannot be blank");
        }
        if (StringUtils.isBlank(lastFourDigits) || !lastFourDigits.matches("\\d{4}")) {
            throw new InvalidValueObjectException("Last four digits must be exactly 4 numeric characters");
        }
        if (expirationDate == null) {
            throw new InvalidValueObjectException("Expiration date cannot be null");
        }

        this.cardNumberToken = cardNumberToken;
        this.lastFourDigits = lastFourDigits;
        this.cardholderName = StringUtils.trimToNull(cardholderName);
        this.expirationDate = expirationDate;
    }

    public static CardData of(String token, String lastFour, String holderName, ExpirationDate expDate) {
        return new CardData(token, lastFour, holderName, expDate);
    }

    public static CardData fromCardNumber(CardNumber cardNumber, String holderName, ExpirationDate expDate) {
        String maskedNumber = cardNumber.getMasked();
        String lastFour = maskedNumber.substring(maskedNumber.length() - 4);
        String token = "TKN-" + java.util.UUID.randomUUID().toString(); // En prod, usar servicio de tokenización real

        return new CardData(token, lastFour, holderName, expDate);
    }

    public String getMaskedCardNumber() {
        return "************" + lastFourDigits;
    }

    public boolean isExpired() {
        return expirationDate.isExpired();
    }

    @Override
    public String toString() {
        return String.format("Card ****%s - %s", lastFourDigits, cardholderName);
    }
}
