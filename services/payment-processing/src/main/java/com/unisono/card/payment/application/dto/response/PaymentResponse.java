package com.unisono.card.payment.application.dto.response;

import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.model.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String transactionId;
    private String cardId;
    private TransactionType transactionType;
    private TransactionStatus status;

    private BigDecimal amount;
    private String currency;

    private String authorizationCode;
    private String responseCode;
    private String responseMessage;

    private Integer riskScore;

    private String merchantId;
    private String merchantName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime transactionDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime authorizedAt;

    private boolean approved;
    private String declineReason;
}
