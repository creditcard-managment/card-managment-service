package com.unisono.card.data.domain.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardManagementServiceTest {

//  @Mock
//  private CardRepository cardRepository;

//  @InjectMocks
//  private CardManagementService service;

//  private CardHolder validHolder;
//  private ClientId clientId;

//  @BeforeEach
//  void setUp() {
//      clientId = ClientId.of("12345678");
//      validHolder = CardHolder.create(
//              "Juan",
//              "Pérez",
//              "12345678",
//              "juan@example.com",
//              "+5491123456789",
//              LocalDate.of(1990, 1, 1)
//      );
//  }

//  @Test
//  void shouldCreateCard() {
//      // Given
//      CreateCardUseCase.CreateCardCommand command = new CreateCardUseCase.CreateCardCommand(
//              clientId,
//              validHolder,
//              CardType.VISA,
//              CardCategory.CLASSIC
//      );

//      Card expectedCard = Card.create(clientId, validHolder, CardType.VISA, CardCategory.CLASSIC);
//      when(cardRepository.save(any(Card.class))).thenReturn(expectedCard);

//      // When
//      Card result = service.execute(command);

//      // Then
//      assertThat(result).isNotNull();
//      assertThat(result.getStatus()).isEqualTo(CardStatus.REQUESTED);
//      assertThat(result.getClientId()).isEqualTo(clientId);
//      verify(cardRepository, times(1)).save(any(Card.class));
//  }

//  @Test
//  void shouldFindCardById() {
//      // Given
//      CardId cardId = CardId.generate();
//      Card card = Card.create(clientId, validHolder, CardType.VISA, CardCategory.CLASSIC);
//      when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

//      // When
//      Optional<Card> result = service.findById(cardId);

//      // Then
//      assertThat(result).isPresent();
//      assertThat(result.get()).isEqualTo(card);
//      verify(cardRepository, times(1)).findById(cardId);
//  }
}
