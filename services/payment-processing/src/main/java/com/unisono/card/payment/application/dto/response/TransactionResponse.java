package com.unisono.card.payment.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.unisono.card.payment.domain.model.PaymentMethod;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Complete transaction details response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {

    // Transaction basic info
    private String transactionId;
    private String cardId;
    private TransactionType type;
    private TransactionStatus status;

    // Amount details
    private BigDecimal amount;
    private String currency;
    private BigDecimal localAmount;
    private String localCurrency;
    private BigDecimal exchangeRate;

    // Card information (masked)
    private String cardLastFour;
    private String cardholderName;
    private String cardExpirationDate;

    // Authorization
    private String authorizationCode;
    private String authorizationResponseCode;
    private Integer riskScore;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime authorizedAt;

    // Merchant information
    private String merchantId;
    private String merchantName;
    private String merchantCategoryCode;
    private String merchantCity;
    private String merchantCountry;

    // Transaction details
    private PaymentMethod paymentMethod;
    private String terminalId;
    private String posEntryMode;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime transactionDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime settlementDate;

    // Status info
    private String declineReason;
    private String reversalReason;

    // Metadata
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private Integer version;
}
