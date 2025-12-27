package com.unisono.card.data.domain.model;

import org.junit.jupiter.api.Test;

class CardTest {

    //@Test
    //void shouldCreateCardInRequestedState() {
//       ClientId clientId = ClientId.of("12345678");
//       CardHolder holder = createValidHolder();

//       Card card = Card.create(clientId, holder, CardType.VISA, CardCategory.CLASSIC);

//       assertThat(card.getStatus()).isEqualTo(CardStatus.REQUESTED);
//       assertThat(card.getClientId()).isEqualTo(clientId);
//       assertThat(card.getType()).isEqualTo(CardType.VISA);
//       assertThat(card.getCategory()).isEqualTo(CardCategory.CLASSIC);
//   }

//   @Test
//   void shouldApproveCard() {
//       Card card = createRequestedCard();

//       card.approve();

//       assertThat(card.getStatus()).isEqualTo(CardStatus.APPROVED);
//   }

//   @Test
//   void shouldIssueApprovedCard() {
//       Card card = createApprovedCard();
//       CardNumber cardNumber = CardNumber.of("4532123456789010");
//       CVV cvv = CVV.of("123");
//       ExpirationDate expiration = ExpirationDate.generateDefault();

//       card.issue(cardNumber, cvv, "hashedPin", expiration);

//       assertThat(card.getStatus()).isEqualTo(CardStatus.ISSUED);
//       assertThat(card.getCardNumber()).isEqualTo(cardNumber);
//   }

//   @Test
//   void shouldActivateIssuedCard() {
//       Card card = createIssuedCard();

//       card.activate();

//       assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
//       assertThat(card.getActivatedAt()).isNotNull();
//   }

//   @Test
//   void shouldNotActivateAlreadyActiveCard() {
//       Card card = createActiveCard();

//       assertThatThrownBy(card::activate)
//               .isInstanceOf(BusinessRuleException.class)
//               .hasMessageContaining("ya está activa");
//   }

//   @Test
//   void shouldBlockActiveCard() {
//       Card card = createActiveCard();

//       card.block("Pérdida de tarjeta");

//       assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
//       assertThat(card.getBlockReason()).isEqualTo("Pérdida de tarjeta");
//       assertThat(card.isBlocked()).isTrue();
//   }

//   @Test
//   void shouldUnblockBlockedCard() {
//       Card card = createBlockedCard();

//       card.unblock();

//       assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
//       assertThat(card.getBlockReason()).isNull();
//   }

//   @Test
//   void shouldResetPINOnActiveCard() {
//       Card card = createActiveCard();

//       card.resetPIN("newHashedPin");

//       assertThat(card.getPinHash()).isEqualTo("newHashedPin");
//   }

//   @Test
//   void shouldNotResetPINOnBlockedCard() {
//       Card card = createBlockedCard();

//       assertThatThrownBy(() -> card.resetPIN("newHash"))
//               .isInstanceOf(BusinessRuleException.class)
//               .hasMessageContaining("activas");
//   }

//   @Test
//   void shouldBeOperationalWhenActiveAndNotExpired() {
//       Card card = createActiveCard();

//       assertThat(card.isOperational()).isTrue();
//   }

//   @Test
//   void shouldNotBeOperationalWhenBlocked() {
//       Card card = createBlockedCard();

//       assertThat(card.isOperational()).isFalse();
//   }

//   // Helper methods
//   private Card createRequestedCard() {
//       return Card.create(
//               ClientId.of("12345678"),
//               createValidHolder(),
//               CardType.VISA,
//               CardCategory.CLASSIC
//       );
//   }

//   private Card createApprovedCard() {
//       Card card = createRequestedCard();
//       card.approve();
//       return card;
//   }

//   private Card createIssuedCard() {
//       Card card = createApprovedCard();
//       card.issue(
//               CardNumber.of("4532123456789010"),
//               CVV.of("123"),
//               "hashedPin",
//               ExpirationDate.generateDefault()
//       );
//       return card;
//   }

//   private Card createActiveCard() {
//       Card card = createIssuedCard();
//       card.activate();
//       return card;
//   }

//   private Card createBlockedCard() {
//       Card card = createActiveCard();
//       card.block("Test");
//       return card;
//   }

//   private CardHolder createValidHolder() {
//       return CardHolder.create(
//               "Juan",
//               "Pérez",
//               "12345678",
//               "juan@example.com",
//               "+5491123456789",
//               LocalDate.of(1990, 1, 1)
//       );
//   }
}
