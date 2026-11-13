package com.unisono.card.payment.support;

import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.CardData;
import com.unisono.card.payment.domain.model.MerchantInfo;
import com.unisono.card.payment.domain.model.PaymentMethod;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import com.unisono.card.shared.domain.valueobject.Money;

import java.time.LocalDate;

/**
 * Transacciones de prueba en distintos estados, armadas con las factories del dominio
 */
public final class TestTransactions {

    public static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");
    public static final String MERCHANT_ID = "MERCH-001";

    private TestTransactions() {
    }

    public static Transaction pendingPurchase(long amount) {
        return Transaction.createPurchase(CARD_ID, cardData(), merchant(),
                TransactionAmount.of(Money.ofARS(amount)), PaymentMethod.CHIP);
    }

    public static Transaction authorizedPurchase(long amount) {
        Transaction transaction = pendingPurchase(amount);
        transaction.authorize(AuthorizationCode.approved("A1B2C3"), 10);
        return transaction;
    }

    public static Transaction approvedPurchase(long amount) {
        Transaction transaction = authorizedPurchase(amount);
        transaction.approve();
        return transaction;
    }

    public static Transaction declinedPurchase(long amount, String responseCode) {
        Transaction transaction = pendingPurchase(amount);
        transaction.decline("Declined in test", AuthorizationCode.declined(responseCode));
        return transaction;
    }

    public static Transaction approvedRefund(long amount, TransactionId originalId) {
        Transaction refund = Transaction.createRefund(CARD_ID, cardData(), merchant(),
                TransactionAmount.of(Money.ofARS(amount)), originalId);
        refund.approveRefund(originalId);
        return refund;
    }

    public static CardData cardData() {
        LocalDate expiration = LocalDate.now().plusYears(2);
        return CardData.of("tok_123", "0366", "Juan Pérez",
                ExpirationDate.of(expiration.getMonthValue(), expiration.getYear()));
    }

    public static MerchantInfo merchant() {
        return MerchantInfo.of(MERCHANT_ID, "Supermercado Uno", "5411", "CABA", "AR");
    }
}
