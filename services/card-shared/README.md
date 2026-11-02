# card-shared

Librería base (*shared kernel*) del sistema de tarjetas: Value Objects, excepciones de dominio y servicios de cifrado y hashing. Es un `jar` sin `main` que usan `card-data`, `card-lifecycle`, `card-limits` y `payment-processing`.

> Documentación técnica funcional completa: [`docs/technical/SHARED.md`](../../docs/technical/SHARED.md)

## Contenido

| Paquete | Clases |
|---|---|
| `domain.valueobject` | `CardId`, `ClientId`, `CardNumber`, `CVV`, `PIN`, `ExpirationDate`, `Money`, `Percentage`, `CardStatus` |
| `domain.exception` | `DomainException`, `ValidationException`, `InvalidValueObjectException`, `BusinessRuleException`, `NotFoundException` |
| `infrastructure.security` | `EncryptionService` / `AesEncryptionService` (AES-256-GCM, claves versionadas), `HashingService` / `BCryptHashingService`, `EncryptionProperties` |

Los Value Objects son inmutables, se crean por factory (`CardNumber.of(...)`, `CardId.generate()`, `Money.ofARS(...)`) y lanzan `InvalidValueObjectException` si el dato es inválido.

```java
CardNumber pan = CardNumber.of("4532 0151-1283 0366");
pan.getMasked();                       // "**** **** **** 0366"

Money total = Money.ofARS(1000).add(Money.ofARS(500));

if (!CardStatus.ACTIVE.canTransitionTo(CardStatus.EXPIRED)) { ... }
```

## Uso

```xml
<dependency>
    <groupId>com.unisono.card</groupId>
    <artifactId>card-shared</artifactId>
    <version>${project.version}</version>
</dependency>
```

Para los Value Objects y las excepciones alcanza con la dependencia. Los servicios de seguridad son beans de Spring: solo los registra un módulo que escanee `com.unisono.card.shared` (hoy, `card-data`), y ese módulo debe configurar el llavero de claves:

```yaml
encryption:
  active-key-id: "${ENCRYPTION_ACTIVE_KEY_ID:k1}"
  keys:
    k1: "${ENCRYPTION_KEY_K1}"            # openssl rand -base64 32
  legacy-secret-key: "${ENCRYPTION_KEY}"  # opcional: datos cifrados antes del llavero
```

Sin una configuración válida el servicio no arranca. La rotación de claves está descrita en `SHARED.md` §4.1.

## Tests

```bash
mvn -pl services/card-shared test
```

| Test | Cubre |
|---|---|
| `CardStatusTest` | Máquina de estados |
| `CardIdTest`, `CardNumberTest`, `ExpirationDateTest`, `MoneyTest`, `PercentageTest` | Reglas de cada Value Object |
| `ValueObjectValidationTest` | Excepción común de los Value Objects |
| `AesEncryptionServiceTest` | Cifrado, rotación, datos legacy y validación de configuración |
