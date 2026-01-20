package com.unisono.card.merchant.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Perfil del Merchant con información de contacto
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MerchantProfile {

    private String contactName;
    private String email;
    private String phone;
    private String website;
    private Address address;

    public static MerchantProfile of(
            String contactName,
            String email,
            String phone,
            String website,
            Address address
    ) {
        validateContactName(contactName);
        validateEmail(email);
        validatePhone(phone);
        Objects.requireNonNull(address, "Address cannot be null");

        return new MerchantProfile(
                contactName.trim(),
                email.trim().toLowerCase(),
                phone.trim(),
                website != null ? website.trim() : null,
                address
        );
    }

    private static void validateContactName(String contactName) {
        Objects.requireNonNull(contactName, "Contact name cannot be null");
        if (contactName.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact name cannot be empty");
        }
        if (contactName.length() > 200) {
            throw new IllegalArgumentException("Contact name too long (max 200 characters)");
        }
    }

    private static void validateEmail(String email) {
        Objects.requireNonNull(email, "Email cannot be null");
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private static void validatePhone(String phone) {
        Objects.requireNonNull(phone, "Phone cannot be null");
        String cleaned = phone.replaceAll("[^0-9+]", "");
        if (cleaned.length() < 8 || cleaned.length() > 20) {
            throw new IllegalArgumentException("Invalid phone number");
        }
    }

    public MerchantProfile withEmail(String newEmail) {
        return MerchantProfile.of(this.contactName, newEmail, this.phone, this.website, this.address);
    }

    public MerchantProfile withPhone(String newPhone) {
        return MerchantProfile.of(this.contactName, this.email, newPhone, this.website, this.address);
    }

    public MerchantProfile withAddress(Address newAddress) {
        return MerchantProfile.of(this.contactName, this.email, this.phone, this.website, newAddress);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MerchantProfile)) return false;
        MerchantProfile that = (MerchantProfile) o;
        return Objects.equals(email, that.email) &&
                Objects.equals(phone, that.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, phone);
    }

    @Override
    public String toString() {
        return "MerchantProfile{" +
                "contactName='" + contactName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
