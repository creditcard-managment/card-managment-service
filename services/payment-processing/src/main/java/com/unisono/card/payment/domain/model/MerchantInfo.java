package com.unisono.card.payment.domain.model;


import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Value Object representing merchant information in a transaction
 */
@Getter
@EqualsAndHashCode
public class MerchantInfo implements Serializable {

    private final String merchantId;
    private final String merchantName;
    private final String merchantCategoryCode; // MCC (4 dígitos)
    private final String city;
    private final String country; // ISO 3166-1 alpha-2

    private MerchantInfo(String merchantId, String merchantName, String merchantCategoryCode,
                         String city, String country) {
        if (StringUtils.isBlank(merchantId)) {
            throw new InvalidValueObjectException("Merchant ID cannot be blank");
        }
        if (StringUtils.isBlank(merchantName)) {
            throw new InvalidValueObjectException("Merchant name cannot be blank");
        }
        if (StringUtils.isNotBlank(merchantCategoryCode) && !merchantCategoryCode.matches("\\d{4}")) {
            throw new InvalidValueObjectException("Invalid MCC format. Must be 4 digits");
        }
        if (StringUtils.isNotBlank(country) && !country.matches("[A-Z]{2}")) {
            throw new InvalidValueObjectException("Invalid country code. Must be ISO 3166-1 alpha-2");
        }

        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.merchantCategoryCode = merchantCategoryCode;
        this.city = city;
        this.country = country;
    }

    public static MerchantInfo of(String merchantId, String merchantName, String mcc,
                                  String city, String country) {
        return new MerchantInfo(merchantId, merchantName, mcc, city, country);
    }

    public static MerchantInfo simple(String merchantId, String merchantName) {
        return new MerchantInfo(merchantId, merchantName, null, null, null);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %s, %s", merchantName, merchantId, city, country);
    }
}
