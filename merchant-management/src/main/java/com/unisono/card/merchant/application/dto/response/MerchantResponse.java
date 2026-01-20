package com.unisono.card.merchant.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.unisono.card.merchant.domain.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO Response para Merchant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResponse {

    private String id;
    private String legalName;
    private String tradeName;
    private String taxId;
    private MerchantCategory category;
    private MerchantType type;
    private MerchantStatus status;

    // Contact Information
    private ContactInfo contactInfo;

    // Business Information
    private BusinessInfo businessInfo;

    // Statistics
    private Integer activeTerminalsCount;
    private Integer totalTerminalsCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime registeredAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime activatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdatedAt;

    // Factory method
    public static MerchantResponse from(Merchant merchant) {
        return MerchantResponse.builder()
                .id(merchant.getId().getValue())
                .legalName(merchant.getBusinessInformation().getLegalName())
                .tradeName(merchant.getBusinessInformation().getTradeName())
                .taxId(merchant.getTaxInformation().getFormattedTaxId())
                .category(merchant.getCategory())
                .type(merchant.getType())
                .status(merchant.getStatus())
                .contactInfo(ContactInfo.from(merchant.getProfile()))
                .businessInfo(BusinessInfo.from(merchant.getBusinessInformation()))
                .activeTerminalsCount(merchant.getActiveTerminalsCount())
                .totalTerminalsCount(merchant.getTerminals().size())
                .registeredAt(merchant.getRegisteredAt())
                .activatedAt(merchant.getActivatedAt())
                .lastUpdatedAt(merchant.getLastUpdatedAt())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfo {
        private String contactName;
        private String email;
        private String phone;
        private String website;
        private AddressDto address;

        public static ContactInfo from(MerchantProfile profile) {
            return ContactInfo.builder()
                    .contactName(profile.getContactName())
                    .email(profile.getEmail())
                    .phone(profile.getPhone())
                    .website(profile.getWebsite())
                    .address(AddressDto.from(profile.getAddress()))
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessInfo {
        private String registrationNumber;
        private String businessDescription;
        private Integer yearsInBusiness;
        private String taxCategory;
        private boolean vatResponsible;

        public static BusinessInfo from(BusinessInformation businessInformation) {
            return BusinessInfo.builder()
                    .registrationNumber(businessInformation.getRegistrationNumber())
                    .businessDescription(businessInformation.getBusinessDescription())
                    .yearsInBusiness(businessInformation.getYearsInBusiness())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        private String streetAddress;
        private String streetNumber;
        private String floor;
        private String apartment;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private Double latitude;
        private Double longitude;
        private String fullAddress;

        public static AddressDto from(Address address) {
            return AddressDto.builder()
                    .streetAddress(address.getStreetAddress())
                    .streetNumber(address.getStreetNumber())
                    .floor(address.getFloor())
                    .apartment(address.getApartment())
                    .city(address.getCity())
                    .state(address.getState())
                    .postalCode(address.getPostalCode())
                    .country(address.getCountry())
                    .latitude(address.getLatitude())
                    .longitude(address.getLongitude())
                    .fullAddress(address.getFullAddress())
                    .build();
        }
    }
}
