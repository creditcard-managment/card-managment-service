package com.unisono.card.payment.domain.exception;

import com.unisono.card.payment.domain.model.TransactionId;

/**
 * Exception thrown when transaction authorization fails
 */
public class AuthorizationFailedException extends PaymentException {

    private final TransactionId transactionId;
    private final String responseCode;
    private final String reason;

    public AuthorizationFailedException(TransactionId transactionId, String responseCode, String reason) {
        super(String.format(
                "Authorization failed for transaction %s. Code: %s, Reason: %s",
                transactionId, responseCode, reason
        ));
        this.transactionId = transactionId;
        this.responseCode = responseCode;
        this.reason = reason;
    }

    public static AuthorizationFailedException insufficientFunds(TransactionId transactionId) {
        return new AuthorizationFailedException(transactionId, "51", "Insufficient funds");
    }

    public static AuthorizationFailedException fraudSuspected(TransactionId transactionId) {
        return new AuthorizationFailedException(transactionId, "59", "Suspected fraud");
    }

    public static AuthorizationFailedException cardRestricted(TransactionId transactionId) {
        return new AuthorizationFailedException(transactionId, "62", "Card restricted");
    }

    public static AuthorizationFailedException invalidMerchant(TransactionId transactionId) {
        return new AuthorizationFailedException(transactionId, "03", "Invalid merchant");
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getReason() {
        return reason;
    }
}
