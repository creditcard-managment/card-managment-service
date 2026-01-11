package com.unisono.card.payment.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.unisono.card.payment.domain.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for refund processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    private String refundTransactionId;
    private String originalTransactionId;
    private String cardId;

    private BigDecimal refundAmount;
    private String currency;

    private TransactionStatus status;
    private String reason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;

    private boolean approved;
    private String responseMessage;
}
