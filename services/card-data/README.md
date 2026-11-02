# 🧱 card-shared

> Módulo fundacional con componentes compartidos y reutilizables

## 📋 Descripción

**card-shared** es el módulo base que contiene los **building blocks** fundamentales utilizados por todos los demás módulos del sistema. Proporciona Value Objects, excepciones del dominio y servicios de seguridad que garantizan consistencia y reutilización en toda la aplicación.

## 🎯 Responsabilidades

- ✅ Definir Value Objects comunes (Money, CardId, ClientId, Percentage)
- ✅ Proveer jerarquía de excepciones del dominio
- ✅ Ofrecer servicios de encriptación y hashing
- ✅ Establecer contratos y validaciones base
- ✅ Garantizar inmutabilidad y validaciones consistentes

## 📦 Componentes Principales

### Value Objects

#### **CardId**
Identificador único de tarjeta basado en UUID.

```java
// Crear
CardId cardId = CardId.generate();

// Desde String
CardId cardId = CardId.of("550e8400-e29b-41d4-a716-446655440000");

// Obtener valor
UUID value = cardId.getValue();
```

**Características:**
- Formato: UUID v4
- Inmutable
- Validación automática de formato

---

#### **ClientId**
Identificador de cliente (DNI/CUIL argentino).

```java
// DNI (8 dígitos)
ClientId clientId = ClientId.of("12345678");

// CUIL (11 dígitos)
ClientId clientId = ClientId.of("20123456789");

// Validación
boolean isValid = ClientId.isValid("12345678"); // true
```

**Características:**
- Formatos válidos: 7-11 dígitos
- Validación de DNI/CUIL argentino
- Solo dígitos numéricos
- Inmutable

---

#### **Money**
Representación de dinero con precisión decimal.

```java
// Crear Money en ARS
Money amount = Money.ofARS(new BigDecimal("50000.00"));
Money amount = Money.ofARS(50000);

// Crear Money con moneda específica
Money amount = Money.of(new BigDecimal("1000.00"), "USD");

// Operaciones aritméticas
Money total = price.add(tax);
Money change = payment.subtract(price);
Money doubled = amount.multiply(BigDecimal.valueOf(2));
Money half = amount.divide(BigDecimal.valueOf(2));

// Comparaciones
boolean isExpensive = price.isGreaterThan(budget);
boolean isCheap = price.isLessThan(maxPrice);
boolean isFree = price.isZero();

// Obtener valores
BigDecimal value = amount.getAmount();
String currencyCode = amount.getCurrencyCode();
Currency currency = amount.getCurrency();
```

**Características:**
- Precisión: 2 decimales (RoundingMode.HALF_UP)
- Soporte multi-moneda
- Operaciones seguras (validación de misma moneda)
- Inmutable
- No permite operaciones entre diferentes monedas

**Invariantes:**
- Amount no nulo
- Currency no nulo
- Operaciones solo entre misma moneda

---

#### **Percentage**
Representación de porcentajes (0-100%).

```java
// Crear porcentaje
Percentage discount = Percentage.of(new BigDecimal("15.50"));

// Validaciones
Percentage valid = Percentage.of(new BigDecimal("0"));     // 0%
Percentage valid = Percentage.of(new BigDecimal("100"));   // 100%

// Error si está fuera de rango
Percentage invalid = Percentage.of(new BigDecimal("150")); // ❌ Exception

// Aplicar a un monto
BigDecimal result = percentage.applyTo(new BigDecimal("1000"));

// Obtener valor
BigDecimal value = percentage.getValue();
```

**Características:**
- Rango: 0 - 100
- Precisión: 4 decimales
- Inmutable
- Validación automática

---

### Jerarquía de Excepciones

```
DomainException (abstract)
├── ValidationException
│   └── Uso: Datos de entrada inválidos
│   └── Ejemplo: "El monto debe ser mayor a cero"
│
├── NotFoundException
│   └── Uso: Entidad no encontrada
│   └── Ejemplo: "Card not found: 550e8400-..."
│
└── BusinessRuleException
    └── Uso: Regla de negocio violada
    └── Ejemplo: "No se puede activar una tarjeta bloqueada"
```

**Uso:**

```java
// ValidationException
if (amount == null) {
    throw new ValidationException("El monto no puede ser nulo");
}

// NotFoundException
Card card = repository.findById(id)
    .orElseThrow(() -> new NotFoundException("Card", id));

// BusinessRuleException
if (!card.isOperational()) {
    throw new BusinessRuleException(
        "La tarjeta no está disponible para operar"
    );
}
```

---

### Servicios de Seguridad

#### **EncryptionService**

Servicio de encriptación simétrica AES-256-GCM.

```java
@Autowired
private EncryptionService encryptionService;

// Encriptar
String plainText = "4532123456789012";
String encrypted = encryptionService.encrypt(plainText);
// Output: "IV:ciphertext" en Base64

// Desencriptar
String decrypted = encryptionService.decrypt(encrypted);
// Output: "4532123456789012"
```

**Características:**
- Algoritmo: AES-256-GCM
- IV aleatorio por cada encriptación
- Encoding: Base64
- IV incluido en el resultado
- Thread-safe

**Configuración:**
```yaml
encryption:
  secret-key: your-256-bit-secret-key-here
  algorithm: AES/GCM/NoPadding
```

**⚠️ IMPORTANTE:**
- En producción, usar un **vault** (Vault, AWS Secrets Manager)
- Nunca hardcodear keys en código
- Rotar keys periódicamente

---

#### **HashingService**

Servicio de hashing BCrypt para contraseñas/PINs.

```java
@Autowired
private HashingService hashingService;

// Hash
String pin = "1234";
String hashed = hashingService.hash(pin);
// Output: "$2a$12$..."

// Verificar
boolean isValid = hashingService.verify(pin, hashed);
// Output: true
```

**Características:**
- Algoritmo: BCrypt
- Strength: 12 (2^12 iteraciones)
- Salt automático por hash
- Resistente a rainbow tables
- Thread-safe

**⚠️ IMPORTANTE:**
- **Nunca** almacenar PINs/passwords en texto plano
- **Nunca** loggear PINs/passwords
- El hash es **irreversible** (one-way)

---

## 🏗️ Estructura del Módulo

```
card-shared/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/unisono/card/shared/
│   │   │       ├── domain/
│   │   │       │   ├── exception/
│   │   │       │   │   ├── DomainException.java
│   │   │       │   │   ├── ValidationException.java
│   │   │       │   │   ├── NotFoundException.java
│   │   │       │   │   └── BusinessRuleException.java
│   │   │       │   │
│   │   │       │   └── valueobject/
│   │   │       │       ├── CardId.java
│   │   │       │       ├── ClientId.java
│   │   │       │       ├── Money.java
│   │   │       │       └── Percentage.java
│   │   │       │
│   │   │       └── infrastructure/
│   │   │           └── security/
│   │   │               ├── EncryptionService.java
│   │   │               └── HashingService.java
│   │   │
│   │   └── resources/
│   │       └── application-card-shared.yml
│   │
│   └── test/
│       └── java/
│           └── com/unisono/card/shared/
│               ├── domain/valueobject/
│               │   ├── CardIdTest.java
│               │   ├── ClientIdTest.java
│               │   ├── MoneyTest.java
│               │   └── PercentageTest.java
│               │
│               └── infrastructure/security/
│                   ├── EncryptionServiceTest.java
│                   └── HashingServiceTest.java
│
└── pom.xml
```

---

## 🧪 Testing

### Ejecutar Tests

```bash
cd card-shared
mvn test
```

### Cobertura

```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

**Target de cobertura:** >90%

### Ejemplos de Tests

```java
@Test
void shouldCreateMoneyWithValidAmount() {
    Money money = Money.ofARS(new BigDecimal("1000.00"));
    
    assertThat(money.getAmount())
        .isEqualByComparingTo(new BigDecimal("1000.00"));
}

@Test
void shouldThrowExceptionWhenAddingDifferentCurrencies() {
    Money ars = Money.ofARS(new BigDecimal("100"));
    Money usd = Money.of(new BigDecimal("100"), "USD");
    
    assertThatThrownBy(() -> ars.add(usd))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("different currencies");
}

@Test
void shouldEncryptAndDecryptSuccessfully() {
    String original = "4532123456789012";
    
    String encrypted = encryptionService.encrypt(original);
    String decrypted = encryptionService.decrypt(encrypted);
    
    assertThat(decrypted).isEqualTo(original);
    assertThat(encrypted).isNotEqualTo(original);
}
```

---

## 📚 Dependencias

### Maven

```xml
<dependency>
    <groupId>com.unisono.card</groupId>
    <artifactId>card-shared</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Dependencias Externas

- **Lombok** - Reducción de boilerplate
- **Spring Boot Starter** - Framework base
- **BCrypt** - Hashing
- **SLF4J** - Logging

---

## 🔒 Consideraciones de Seguridad

### Encriptación

✅ **HACER:**
- Usar EncryptionService para PAN, CVV
- Almacenar keys en vault (Vault, AWS Secrets Manager)
- Rotar keys periódicamente
- Validar inputs antes de encriptar

❌ **NO HACER:**
- Hardcodear keys en código
- Compartir keys entre ambientes
- Loggear datos encriptados sin sanitizar
- Reutilizar IVs

### Hashing

✅ **HACER:**
- Usar HashingService para PINs, passwords
- Verificar con verify(), nunca comparar hashes directamente
- Usar strength >= 12

❌ **NO HACER:**
- Almacenar PINs en texto plano
- Loggear PINs (ni hasheados)
- Usar MD5 o SHA1
- Intentar "desencriptar" un hash

---

## 📖 Patrones Utilizados

### Value Objects

Todos los Value Objects siguen estos principios:

1. **Inmutabilidad** - No tienen setters
2. **Validación en constructor** - Fail fast
3. **Equality por valor** - equals() y hashCode() basados en valor
4. **Self-validation** - Se validan a sí mismos
5. **Encapsulamiento** - Esconden detalles de implementación

### Factory Methods

```java
// Named constructors para claridad
Money.ofARS(amount)
Money.of(amount, currency)
Money.zero()

CardId.generate()
CardId.of(uuid)

ClientId.of(dni)
```

---

## 🚀 Uso en Otros Módulos

### card-data

```java
// Value Objects
private CardId id;
private ClientId clientId;
private Money amount;

// Excepciones
if (!card.isOperational()) {
    throw new BusinessRuleException("Card not operational");
}

// Seguridad
String encryptedPAN = encryptionService.encrypt(pan);
String hashedPIN = hashingService.hash(pin);
```

### card-limits

```java
// Value Objects
private Money purchaseLimit;
private Money availableCredit;
private Percentage cashAdvancePercentage;

// Operaciones
Money newLimit = currentLimit.add(increase);
Money available = totalLimit.subtract(consumed);
```

---

## 🔄 Versionado

Este módulo sigue [Semantic Versioning](https://semver.org/):

- **MAJOR**: Cambios incompatibles en API
- **MINOR**: Nueva funcionalidad compatible
- **PATCH**: Bug fixes compatibles

**Versión actual:** 1.0.0-SNAPSHOT

---

## 📞 Soporte

**Equipo:** Shared Components Team  
**Email:** shared@unisono.com  
**Slack:** #card-shared

---

## 📝 Changelog

### [1.0.0] - 2025-12-27

#### Added
- ✅ CardId value object
- ✅ ClientId value object
- ✅ Money value object con multi-moneda
- ✅ Percentage value object
- ✅ Jerarquía de excepciones del dominio
- ✅ EncryptionService (AES-256-GCM)
- ✅ HashingService (BCrypt)
- ✅ Tests unitarios (>90% cobertura)

---

**Última actualización:** Diciembre 2025  
**Mantenido por:** Unisono Development Team
```

---

# 📄 **README.md - card-data**

**Ubicación:** `card-data/README.md`

```markdown
# 💳 card-data

> Módulo de gestión del ciclo de vida de tarjetas de crédito

## 📋 Descripción

**card-data** es el módulo core que gestiona la **creación, emisión, activación, bloqueo y cancelación** de tarjetas de crédito. Implementa el aggregate `Card` que protege todas las invariantes del dominio relacionadas con el estado y los datos de la tarjeta.

## 🎯 Responsabilidades

- ✅ Gestionar el ciclo de vida completo de tarjetas
- ✅ Proteger datos sensibles (PAN, CVV, PIN)
- ✅ Validar transiciones de estado
- ✅ Almacenar datos del titular
- ✅ Auditar cambios de estado
- ✅ Exponer API REST para operaciones

## 🏗️ Arquitectura

### Modelo de Dominio

```
Card (Aggregate Root)
├── CardId (Value Object)
├── ClientId (Value Object)
├── CardHolder (Entity)
├── CardNumber (Value Object)
├── CVV (Value Object)
├── PIN (Value Object)
├── ExpirationDate (Value Object)
├── CardType (Enum)
├── CardCategory (Enum)
└── CardStatus (Enum)
```

### Máquina de Estados

```
REQUESTED
↓
approve() / reject()
↓
APPROVED ──reject()──→ CANCELLED
↓
issue()
↓
ISSUED ──cancel()──→ CANCELLED
↓
activate()
↓
ACTIVE ⇄ BLOCKED ⇄ BLOCKED_FRAUD
↓         ↓
expire() cancel()
↓         ↓
EXPIRED   CANCELLED
```

---

## 📦 Componentes Principales

### Aggregate Root: Card

**Archivo:** `Card.java`

```java
public class Card {
    // Identificación
    private CardId id;
    private ClientId clientId;
    
    // Titular
    private CardHolder holder;
    
    // Datos de la tarjeta
    private CardNumber cardNumber;
    private CVV cvv;
    private String pinHash;
    private ExpirationDate expirationDate;
    
    // Clasificación
    private CardType type;
    private CardCategory category;
    
    // Estado
    private CardStatus status;
    
    // Auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime blockedAt;
    private String blockReason;
}
```

**Factory Methods:**

```java
// Crear nueva solicitud
Card card = Card.create(clientId, holder, type, category);
// Estado: REQUESTED

// Emitir tarjeta
Card card = Card.issue(cardId, cardNumber, cvv, pin, expirationDate);
// Asigna PAN, CVV, PIN
```

**Métodos de Negocio:**

```java
// Ciclo de vida
card.approve();              // REQUESTED → APPROVED
card.reject(reason);         // REQUESTED → CANCELLED
card.activate();             // ISSUED → ACTIVE
card.block(reason);          // ACTIVE → BLOCKED
card.blockForFraud(reason);  // ACTIVE → BLOCKED_FRAUD
card.unblock();              // BLOCKED → ACTIVE
card.cancel();               // * → CANCELLED
card.markAsExpired();        // ACTIVE → EXPIRED

// PIN
card.resetPIN(oldPin, newPin);
boolean hasPin = card.isPINSet();

// Validaciones
boolean canOperate = card.isOperational();
boolean isBlocked = card.isBlocked();
boolean isExpiring = card.isNearExpiration();  // < 30 días
boolean needsRenewal = card.requiresRenewal();
```

---

### Entity: CardHolder

**Archivo:** `CardHolder.java`

```java
@Getter
@Builder
public class CardHolder {
    private String firstName;
    private String lastName;
    private String documentNumber;
    private String email;
    private String phoneNumber;
    private LocalDate birthDate;
    private String address;
    
    // Métodos
    public String getFullName();
    public String getCardName();  // APELLIDO NOMBRE
    public boolean isAdult();
    public int getAge();
    
    // Actualizaciones
    public void updateEmail(String email);
    public void updatePhoneNumber(String phone);
    public void updateAddress(String address);
}
```

---

### Value Objects

#### **CardNumber**

```java
// Crear
CardNumber cardNumber = CardNumber.of("4532123456789012");

// Validaciones
- 13-19 dígitos
- Validación Luhn
- Detección automática de tipo (BIN)

// Métodos
String lastFour = cardNumber.getLastFourDigits();  // "9012"
String masked = cardNumber.getMaskedNumber();      // "**** **** **** 9012"
CardType type = cardNumber.detectType();           // VISA
```

#### **CVV**

```java
// Crear
CVV cvv = CVV.of("123");

// Validaciones
- 3-4 dígitos
- Solo numérico

// Seguridad
String value = cvv.getValue();  // "123"
// ⚠️ SIEMPRE encriptar antes de almacenar
```

#### **PIN**

```java
// Crear
PIN pin = PIN.of("1234");

// Validaciones
- 4 dígitos
- No secuencial (1234, 4321)
- No repetido (1111, 2222)

// Seguridad
String hashed = pin.getHashedValue();  // BCrypt hash
// ⚠️ NUNCA almacenar en texto plano
```

#### **ExpirationDate**

```java
// Crear
ExpirationDate expDate = ExpirationDate.of(12, 2028);  // MM, YYYY

// Validaciones
- Mes: 1-12
- No vencida
- Máximo 10 años al futuro

// Métodos
boolean isExpired = expDate.isExpired();
boolean isExpiring = expDate.isExpiringSoon();  // < 30 días
YearMonth ym = expDate.getYearMonth();
String formatted = expDate.getFormatted();  // "12/28"
```

---

### Enums

#### **CardType**

```java
public enum CardType {
    VISA("Visa", List.of("4")),
    MASTERCARD("Mastercard", List.of("51", "52", "53", "54", "55")),
    AMEX("American Express", List.of("34", "37")),
    CABAL("Cabal", List.of("60")),
    NARANJA("Naranja", List.of("589562"));
    
    // Detección automática por BIN
    CardType type = CardType.detectFromCardNumber("4532123456789012");
    // Output: VISA
}
```

#### **CardCategory**

```java
public enum CardCategory {
    CLASSIC(Money.ofARS(50_000)),
    GOLD(Money.ofARS(150_000)),
    PLATINUM(Money.ofARS(300_000)),
    BLACK(Money.ofARS(1_000_000)),
    INFINITE(Money.ofARS(5_000_000));
    
    Money suggestedLimit = CardCategory.GOLD.getSuggestedLimit();
    // Output: $150,000
}
```

#### **CardStatus**

```java
public enum CardStatus {
    REQUESTED,        // Solicitada
    APPROVED,         // Aprobada
    ISSUED,           // Emitida
    ACTIVE,           // Activa
    BLOCKED,          // Bloqueada temporalmente
    BLOCKED_FRAUD,    // Bloqueada por fraude
    EXPIRED,          // Vencida
    CANCELLED;        // Cancelada (final)
}
```

---

## 🌐 API REST

### Base URL

```
http://localhost:8081/api/v1/cards
```

### Endpoints

#### **Crear Tarjeta**

```http
POST /api/v1/cards
Content-Type: application/json

{
  "clientId": "20123456789",
  "holder": {
    "firstName": "Juan",
    "lastName": "Pérez",
    "documentNumber": "20123456789",
    "email": "juan.perez@email.com",
    "phoneNumber": "+54 11 1234-5678",
    "birthDate": "1990-05-15",
    "address": "Av. Corrientes 1234, CABA"
  },
  "type": "VISA",
  "category": "GOLD"
}
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "clientId": "20123456789",
  "maskedCardNumber": "**** **** **** 9012",
  "lastFourDigits": "9012",
  "type": "VISA",
  "category": "GOLD",
  "status": "REQUESTED",
  "holderName": "Juan Pérez",
  "createdAt": "2025-12-27T10:30:00"
}
```

---

#### **Obtener Tarjeta por ID**

```http
GET /api/v1/cards/{cardId}
```

---

#### **Obtener Tarjetas por Cliente**

```http
GET /api/v1/cards/client/{clientId}
```

---

#### **Buscar Tarjeta**

```http
GET /api/v1/cards/search?lastFourDigits=9012&clientId=20123456789
```

---

#### **Bloquear Tarjeta**

```http
POST /api/v1/cards/{cardId}/block
Content-Type: application/json

{
  "reason": "Tarjeta reportada como perdida"
}
```

---

#### **Bloquear por Fraude**

```http
POST /api/v1/cards/{cardId}/block-fraud
Content-Type: application/json

{
  "reason": "Transacciones sospechosas detectadas"
}
```

---

#### **Desbloquear Tarjeta**

```http
POST /api/v1/cards/{cardId}/unblock
```

---

#### **Validar PIN**

```http
POST /api/v1/cards/{cardId}/validate-pin
Content-Type: application/json

{
  "pin": "1234"
}
```

**Response:**
```json
{
  "valid": true
}
```

---

#### **Resetear PIN**

```http
POST /api/v1/cards/{cardId}/reset-pin
Content-Type: application/json

{
  "oldPin": "1234",
  "newPin": "5678"
}
```

---

## 🗄️ Base de Datos

### Tabla: `cards`

```sql
CREATE TABLE cards (
    -- Primary Key
    id VARCHAR(36) PRIMARY KEY,
    client_id VARCHAR(11) NOT NULL,
    
    -- Holder (embedded)
    holder_first_name VARCHAR(100) NOT NULL,
    holder_last_name VARCHAR(100) NOT NULL,
    holder_document_number VARCHAR(11) NOT NULL,
    holder_email VARCHAR(255) NOT NULL,
    holder_phone_number VARCHAR(20),
    holder_birth_date DATE NOT NULL,
    holder_address VARCHAR(500),
    
    -- Card data
    card_number_encrypted TEXT NOT NULL,
    last_four_digits VARCHAR(4) NOT NULL,
    cvv_encrypted TEXT NOT NULL,
    pin_hash TEXT,
    
    -- Classification
    type VARCHAR(20) NOT NULL,
    category VARCHAR(20) NOT NULL,
    expiration_month INTEGER NOT NULL,
    expiration_year INTEGER NOT NULL,
    
    -- Status
    status VARCHAR(20) NOT NULL,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP,
    blocked_at TIMESTAMP,
    block_reason VARCHAR(500),
    
    -- Soft delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);
```

**Índices:**
- `idx_client_id` - Búsqueda por cliente
- `idx_card_number_encrypted` - Búsqueda por PAN
- `idx_last_four_digits` - Búsqueda por últimos 4 dígitos
- `idx_status` - Filtrado por estado

---

### Tabla: `card_audit` (opcional)

```sql
CREATE TABLE card_audit (
    id BIGSERIAL PRIMARY KEY,
    card_id VARCHAR(36) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    action VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);
```

---

## 🔒 Seguridad

### Encriptación

**PAN y CVV:**
- Algoritmo: AES-256-GCM
- IV aleatorio por cada encriptación
- Almacenado en Base64

```java
// Antes de guardar
String encryptedPAN = encryptionService.encrypt(cardNumber.getValue());

// Al recuperar
String decryptedPAN = encryptionService.decrypt(entity.getCardNumberEncrypted());
CardNumber cardNumber = CardNumber.of(decryptedPAN);
```

**PIN:**
- Algoritmo: BCrypt
- Strength: 12
- Salt automático

```java
// Al crear/actualizar
String hashedPIN = hashingService.hash(pin.getValue());

// Al validar
boolean valid = hashingService.verify(inputPin, storedHash);
```

### Visualización

```java
// ❌ NUNCA retornar PAN completo
String pan = "4532123456789012";

// ✅ SIEMPRE enmascarar
String masked = "**** **** **** 9012";
```

---

## 🧪 Testing

### Ejecutar Tests

```bash
cd card-data
mvn test
```

### Cobertura

```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

**Target:** >80%

### Ejemplos de Tests

```java
@Test
void shouldCreateCardInRequestedStatus() {
    Card card = Card.create(clientId, holder, CardType.VISA, CardCategory.GOLD);
    
    assertThat(card.getStatus()).isEqualTo(CardStatus.REQUESTED);
    assertThat(card.getCardNumber()).isNull();
}

@Test
void shouldActivateIssuedCard() {
    Card card = createAndIssueCard();
    
    card.activate();
    
    assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
    assertThat(card.getActivatedAt()).isNotNull();
}

@Test
void shouldThrowExceptionWhenActivatingNonIssuedCard() {
    Card card = Card.create(clientId, holder, type, category);
    
    assertThatThrownBy(() -> card.activate())
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Only ISSUED cards can be activated");
}
```

---

## 🚀 Uso

### Crear y Emitir Tarjeta

```java
// 1. Crear solicitud
Card card = Card.create(
    ClientId.of("20123456789"),
    holder,
    CardType.VISA,
    CardCategory.GOLD
);
// Estado: REQUESTED

// 2. Aprobar
card.approve();
// Estado: APPROVED

// 3. Emitir (asignar PAN, CVV, PIN)
card.issue(
    CardNumber.of("4532123456789012"),
    CVV.of("123"),
    PIN.of("1234"),
    ExpirationDate.of(12, 2028)
);
// Estado: ISSUED

// 4. Guardar
cardRepository.save(card);
```

### Activar Tarjeta

```java
Card card = cardRepository.findById(cardId)
    .orElseThrow(() -> new NotFoundException("Card", cardId));

card.activate();

cardRepository.save(card);
```

### Bloquear Tarjeta

```java
card.block("Tarjeta reportada como perdida");
cardRepository.save(card);
```

### Validar PIN

```java
boolean isValid = card.validatePIN("1234", hashingService);
```

---

## 📚 Dependencias

### Internas

```xml
<dependency>
    <groupId>com.unisono.card</groupId>
    <artifactId>card-shared</artifactId>
</dependency>
```

### Externas

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Validation
- PostgreSQL Driver
- Flyway
- Lombok
- SpringDoc OpenAPI

---

## 🔄 Migraciones (Flyway)

### V1__create_card_tables.sql

Crea:
- Tabla `cards`
- Índices
- Trigger `update_updated_at_column`

### V2__add_card_audit_table.sql

Crea:
- Tabla `card_audit`
- Trigger `audit_card_changes`

### Ejecutar

```bash
mvn flyway:migrate
```

---

## 📖 Casos de Uso Reales

### 1. Solicitud Online

```
Cliente → POST /api/v1/cards
Backend → Card.create()
Estado → REQUESTED
```

### 2. Aprobación Manual

```
Analista → card.approve()
Backend → guarda estado APPROVED
```

### 3. Emisión Automática

```
Sistema → card.issue(pan, cvv, pin, exp)
Sistema → envía por correo
Estado → ISSUED
```

### 4. Activación por Cliente

```
Cliente → POST /api/v1/cards/{id}/activate
Backend → card.activate()
Estado → ACTIVE
```

### 5. Bloqueo Temporal

```
Cliente → POST /api/v1/cards/{id}/block
Backend → card.block("Tarjeta perdida")
Estado → BLOCKED
```

### 6. Detección de Fraude

```
Sistema Antifraude → POST /api/v1/cards/{id}/block-fraud
Backend → card.blockForFraud("Transacciones sospechosas")
Estado → BLOCKED_FRAUD
Notificación → SMS/Email al cliente
```

---

## 📞 Soporte

**Equipo:** Card Management Team  
**Email:** cards@unisono.com  
**Slack:** #card-data

---

## 📝 Changelog

### [1.0.0] - 2025-12-27

#### Added
- ✅ Card aggregate con ciclo de vida completo
- ✅ Value Objects (CardNumber, CVV, PIN, ExpirationDate)
- ✅ Enums (CardType, CardCategory, CardStatus)
- ✅ Use Cases y Domain Services
- ✅ REST Controllers con Swagger
- ✅ JPA Persistence con encriptación
- ✅ Flyway migrations
- ✅ Tests unitarios

---

**Última actualización:** Diciembre 2025  
**Puerto:** 8081  
**Mantenido por:** Unisono Development Team
```

---

# 📄 **README.md - card-limits**

**Ubicación:** `card-limits/README.md`

```markdown
# 💰 card-limits

> Módulo de gestión de límites de crédito y validación de transacciones

## 📋 Descripción

**card-limits** es el módulo responsable de **gestionar límites de crédito** de las tarjetas y **validar transacciones** en tiempo real contra múltiples tipos de límites. Mantiene un **historial completo** de todos los cambios realizados para auditoría y análisis.

## 🎯 Responsabilidades

- ✅ Asignar límites de crédito a tarjetas
- ✅ Actualizar límites (aumentar/disminuir)
- ✅ Consumir crédito en compras/adelantos
- ✅ Liberar crédito en pagos
- ✅ Validar transacciones contra límites
- ✅ Auto-resetear límites temporales
- ✅ Auditar todos los cambios

## 🏗️ Arquitectura

### Modelo de Dominio

```
CreditLimit (Aggregate Root)
├── PurchaseLimit (Value Object)
├── CashAdvanceLimit (Value Object)
├── AvailableCredit (Value Object)
├── ConsumedCredit (Value Object)
├── DailyLimit (Value Object) - Auto-reset
├── MonthlyLimit (Value Object) - Auto-reset
└── history: List<CreditLimitHistory> (Entity)
```

### Invariantes

```
✓ CashAdvanceLimit ≤ PurchaseLimit
✓ ConsumedCredit ≤ PurchaseLimit
✓ AvailableCredit = PurchaseLimit - ConsumedCredit
✓ DailyLimit resetea a las 00:00
✓ MonthlyLimit resetea el día 1
```

---

## 📦 Componentes Principales

### Aggregate Root: CreditLimit

**Archivo:** `CreditLimit.java`

```java
public class CreditLimit {
    // Identificación
    private String id;
    private CardId cardId;
    
    // Límites principales
    private PurchaseLimit purchaseLimit;
    private CashAdvanceLimit cashAdvanceLimit;
    
    // Estado actual
    private ConsumedCredit consumedCredit;
    private AvailableCredit availableCredit;
    
    // Límites temporales
    private DailyLimit dailyLimit;
    private MonthlyLimit monthlyLimit;
    
    // Historial
    private List<CreditLimitHistory> history;
    
    // Auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Factory Method:**

```java
// Asignar límite inicial
CreditLimit limit = CreditLimit.assign(
    cardId,
    Money.ofARS(50000),  // Límite de compras
    0.30,                 // 30% para adelantos
    "admin@bank.com"      // Quien asigna
);

// Resultado:
// - PurchaseLimit: $50,000
// - CashAdvanceLimit: $15,000 (30%)
// - AvailableCredit: $50,000
// - ConsumedCredit: $0
// - DailyLimit: $100,000 (default)
// - MonthlyLimit: $500,000 (default)
```

**Métodos de Negocio:**

```java
// Actualizar límites
limit.increasePurchaseLimit(amount, reason, changedBy);
limit.decreasePurchaseLimit(amount, reason, changedBy);
limit.updateCashAdvanceLimit(newAmount, reason, changedBy);
limit.updateDailyLimit(newAmount, reason, changedBy);
limit.updateMonthlyLimit(newAmount, reason, changedBy);

// Consumir crédito
limit.consumeForPurchase(amount);        // Compra
limit.consumeForCashAdvance(amount);     // Adelanto
limit.releaseCredit(paymentAmount);      // Pago

// Validaciones
boolean canBuy = limit.canPurchase(amount);
boolean canAdvance = limit.canCashAdvance(amount);

// Historial
List<CreditLimitHistory> history = limit.getHistory();
CreditLimitHistory lastChange = limit.getLastChange();
```

---

### Value Objects

#### **PurchaseLimit**

Límite total de compras.

```java
// Crear
PurchaseLimit limit = PurchaseLimit.of(Money.ofARS(50000));

// Validaciones
- Mínimo: $1,000
- Máximo: $10,000,000

// Operaciones
PurchaseLimit increased = limit.increase(Money.ofARS(25000));
PurchaseLimit decreased = limit.decrease(Money.ofARS(10000));
boolean allows = limit.allows(Money.ofARS(5000));
```

---

#### **CashAdvanceLimit**

Límite de adelantos en efectivo.

```java
// Crear desde porcentaje
CashAdvanceLimit limit = CashAdvanceLimit.fromPurchaseLimit(
    purchaseLimit,
    0.30  // 30%
);

// Crear directo
CashAdvanceLimit limit = CashAdvanceLimit.of(Money.ofARS(15000));

// Validaciones
- Mínimo: $500
- Máximo: $5,000,000
- Debe ser ≤ PurchaseLimit
```

---

#### **AvailableCredit**

Crédito disponible actual (calculado).

```java
// Calcular
AvailableCredit available = AvailableCredit.calculate(
    totalLimit,      // $50,000
    consumedCredit   // $5,000
);
// Resultado: $45,000

// Validaciones
boolean sufficient = available.isSufficientFor(Money.ofARS(10000));
boolean exhausted = available.isExhausted();
```

---

#### **ConsumedCredit**

Crédito consumido actual.

```java
// Crear
ConsumedCredit consumed = ConsumedCredit.zero();

// Consumir
ConsumedCredit newConsumed = consumed.consume(Money.ofARS(5000));

// Liberar (pago)
ConsumedCredit released = consumed.release(Money.ofARS(3000));
```

---

#### **DailyLimit** (Auto-Reset)

Límite diario con reseteo automático.

```java
// Crear
DailyLimit daily = DailyLimit.of(Money.ofARS(100000));

// Auto-reset
if (daily.needsReset()) {
    // Automáticamente resetea a $0 consumido
    // cuando cambió el día
}

// Consumir
DailyLimit consumed = daily.consume(Money.ofARS(5000));

// Disponible hoy
Money available = daily.getAvailable();

// Estado
LocalDate resetDate = daily.getResetDate();
```

**Auto-Reset:**
```java
// Ejemplo: Hoy es 28/12/2025
DailyLimit daily = DailyLimit.of(Money.ofARS(100000));
daily = daily.consume(Money.ofARS(10000));
// Consumido: $10,000, Disponible: $90,000

// Mañana 29/12/2025
daily = daily.consume(Money.ofARS(5000));
// Auto-reset detectado
// Consumido: $5,000, Disponible: $95,000
```

---

#### **MonthlyLimit** (Auto-Reset)

Límite mensual con reseteo automático.

```java
// Crear
MonthlyLimit monthly = MonthlyLimit.of(Money.ofARS(500000));

// Auto-reset
if (monthly.needsReset()) {
    // Automáticamente resetea a $0 consumido
    // cuando cambió el mes
}

// Consumir
MonthlyLimit consumed = monthly.consume(Money.ofARS(50000));

// Disponible este mes
Money available = monthly.getAvailable();

// Estado
YearMonth resetMonth = monthly.getResetMonth();
```

**Auto-Reset:**
```java
// Ejemplo: Diciembre 2025
MonthlyLimit monthly = MonthlyLimit.of(Money.ofARS(500000));
monthly = monthly.consume(Money.ofARS(100000));
// Consumido: $100,000, Disponible: $400,000

// Enero 2026 (nuevo mes)
monthly = monthly.consume(Money.ofARS(50000));
// Auto-reset detectado
// Consumido: $50,000, Disponible: $450,000
```

---

### Entity: CreditLimitHistory

**Archivo:** `CreditLimitHistory.java`

```java
public class CreditLimitHistory {
    private Long id;
    private String creditLimitId;
    
    // Tipo de cambio
    private LimitType limitType;  // PURCHASE, CASH_ADVANCE, DAILY, MONTHLY
    private String action;        // ASSIGNED, INCREASED, DECREASED, UPDATED
    
    // Valores
    private Money previousValue;
    private Money newValue;
    private Money difference;
    
    // Contexto
    private String reason;
    private String changedBy;
    private LocalDateTime changedAt;
    
    // Metadata adicional (JSON)
    private String metadata;
}
```

**Factory Methods:**

```java
// Asignación inicial
CreditLimitHistory.recordAssignment(
    creditLimitId,
    LimitType.PURCHASE,
    Money.ofARS(50000),
    "Asignación inicial",
    "admin@bank.com"
);

// Aumento
CreditLimitHistory.recordIncrease(
    creditLimitId,
    LimitType.PURCHASE,
    Money.ofARS(50000),   // Anterior
    Money.ofARS(75000),   // Nuevo
    "Buen comportamiento crediticio",
    "risk@bank.com"
);

// Disminución
CreditLimitHistory.recordDecrease(
    creditLimitId,
    LimitType.PURCHASE,
    Money.ofARS(75000),   // Anterior
    Money.ofARS(50000),   // Nuevo
    "Ajuste por mora",
    "risk@bank.com"
);
```

---

## 🌐 API REST

### Base URL

```
http://localhost:8082/api/v1/credit-limits
```

### Endpoints Principales

#### **Asignar Límite**

```http
POST /api/v1/credit-limits
Content-Type: application/json

{
  "cardId": "550e8400-e29b-41d4-a716-446655440000",
  "purchaseLimitAmount": 50000.00,
  "currency": "ARS",
  "cashAdvancePercentage": 0.30,
  "dailyLimitAmount": 100000.00,
  "monthlyLimitAmount": 500000.00,
  "assignedBy": "admin@bank.com"
}
```

**Response:**
```json
{
  "id": "limit-123",
  "cardId": "550e8400-...",
  "purchaseLimit": 50000.00,
  "cashAdvanceLimit": 15000.00,
  "availableCredit": 50000.00,
  "consumedCredit": 0.00,
  "dailyLimit": 100000.00,
  "dailyAvailable": 100000.00,
  "monthlyLimit": 500000.00,
  "monthlyAvailable": 500000.00,
  "currency": "ARS",
  "createdAt": "2025-12-27T10:30:00"
}
```

---

#### **Obtener por Tarjeta**

```http
GET /api/v1/credit-limits/card/{cardId}
```

---

#### **Información Detallada**

```http
GET /api/v1/credit-limits/card/{cardId}/info
```

**Response:**
```json
{
  "purchaseLimit": 50000.00,
  "cashAdvanceLimit": 15000.00,
  "availableCredit": 45000.00,
  "consumedCredit": 5000.00,
  "utilizationPercentage": 10.00,
  "dailyAvailable": 95000.00,
  "monthlyAvailable": 495000.00,
  "currency": "ARS",
  "isExhausted": false
}
```

---

#### **Aumentar Límite**

```http
PUT /api/v1/credit-limits/{id}/increase-purchase-limit
Content-Type: application/json

{
  "amount": 25000.00,
  "currency": "ARS",
  "reason": "Buen historial crediticio - 12 meses sin mora",
  "changedBy": "risk@bank.com"
}
```

---

#### **Consumir Crédito (Compra)**

```http
POST /api/v1/credit-limits/{id}/consume-purchase
Content-Type: application/json

{
  "amount": 5000.00,
  "currency": "ARS",
  "description": "Compra en Mercado Libre - Notebook"
}
```

---

#### **Liberar Crédito (Pago)**

```http
POST /api/v1/credit-limits/{id}/release-credit
Content-Type: application/json

{
  "paymentAmount": 3000.00,
  "currency": "ARS",
  "description": "Pago de resumen - Débito automático"
}
```

---

#### **Validar Compra**

```http
POST /api/v1/credit-limits/validate/purchase
Content-Type: application/json

{
  "cardId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 5000.00,
  "currency": "ARS"
}
```

**Response (Aprobada):**
```json
{
  "approved": true,
  "reason": "Transacción aprobada",
  "availableCredit": 45000.00,
  "requestedAmount": 5000.00,
  "currency": "ARS"
}
```

**Response (Rechazada):**
```json
{
  "approved": false,
  "reason": "Se excede el límite diario. Disponible hoy: $3,000",
  "availableCredit": 45000.00,
  "requestedAmount": 5000.00,
  "currency": "ARS"
}
```

---

#### **Historial de Cambios**

```http
GET /api/v1/credit-limits/{id}/history
```

**Response:**
```json
[
  {
    "id": 1,
    "limitType": "PURCHASE",
    "action": "ASSIGNED",
    "previousValue": 0.00,
    "newValue": 50000.00,
    "difference": 50000.00,
    "currency": "ARS",
    "reason": "Asignación inicial de límite",
    "changedBy": "admin@bank.com",
    "changedAt": "2025-01-15T10:30:00",
    "description": "Límite de Compras: ASSIGNED cambió de $0.00 a $50,000.00 (diferencia: $50,000.00) - Razón: Asignación inicial"
  },
  {
    "id": 2,
    "limitType": "PURCHASE",
    "action": "INCREASED",
    "previousValue": 50000.00,
    "newValue": 75000.00,
    "difference": 25000.00,
    "currency": "ARS",
    "reason": "Buen historial crediticio",
    "changedBy": "risk@bank.com",
    "changedAt": "2025-03-20T14:15:00",
    "description": "Límite de Compras: INCREASED cambió de $50,000.00 a $75,000.00 (diferencia: $25,000.00)"
  }
]
```

---

## 🗄️ Base de Datos

### Tabla: `credit_limits`

```sql
CREATE TABLE credit_limits (
    id VARCHAR(36) PRIMARY KEY,
    card_id VARCHAR(36) NOT NULL UNIQUE,
    
    -- Purchase limit
    purchase_limit_amount DECIMAL(15, 2) NOT NULL,
    purchase_limit_currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
    
    -- Cash advance limit
    cash_advance_limit_amount DECIMAL(15, 2) NOT NULL,
    cash_advance_limit_currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
    
    -- Consumed/Available
    consumed_credit_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    consumed_credit_currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
    available_credit_amount DECIMAL(15, 2) NOT NULL,
    available_credit_currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
    
    -- Daily limit
    daily_limit_amount DECIMAL(15, 2) NOT NULL,
    daily_limit_currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
    daily_limit_consumed DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    daily_limit_reset_date DATE NOT NULL,
    
    -- Monthly limit
    monthly_limit_amount DECIMAL(15, 2) NOT NULL,
    monthly_limit_currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
    monthly_limit_consumed DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    monthly_limit_reset_year INTEGER NOT NULL,
    monthly_limit_reset_month INTEGER NOT NULL,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);
```

---

### Tabla: `credit_limit_history`

```sql
CREATE TABLE credit_limit_history (
    id BIGSERIAL PRIMARY KEY,
    credit_limit_id VARCHAR(36) NOT NULL,
    
    limit_type VARCHAR(20) NOT NULL,  -- PURCHASE, CASH_ADVANCE, DAILY, MONTHLY
    action VARCHAR(20) NOT NULL,      -- ASSIGNED, INCREASED, DECREASED, UPDATED
    
    previous_value_amount DECIMAL(15, 2),
    previous_value_currency VARCHAR(3),
    new_value_amount DECIMAL(15, 2) NOT NULL,
    new_value_currency VARCHAR(3) NOT NULL,
    difference_amount DECIMAL(15, 2) NOT NULL,
    difference_currency VARCHAR(3) NOT NULL,
    
    reason VARCHAR(500),
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT
);
```

---

## 🔄 Flujo de Validación

### Validación Multi-Nivel

Cuando se valida una compra, se verifican **4 límites** en orden:

```
1. PurchaseLimit
   ├─ ✓ $5,000 ≤ $50,000
   
2. AvailableCredit
   ├─ ✓ $5,000 ≤ $45,000
   
3. DailyLimit
   ├─ ✓ $5,000 ≤ $95,000 (disponible hoy)
   
4. MonthlyLimit
   └─ ✓ $5,000 ≤ $495,000 (disponible este mes)

→ APPROVED
```

Si **cualquiera** falla:
```
3. DailyLimit
   └─ ✗ $10,000 > $3,000 (disponible hoy)

→ REJECTED: "Se excede el límite diario. Disponible hoy: $3,000"
```

---

## 🧪 Testing

### Ejecutar Tests

```bash
cd card-limits
mvn test
```

### Cobertura

```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

**Target:** >80%

### Ejemplos de Tests

```java
@Test
void shouldAssignCreditLimit() {
    CreditLimit limit = CreditLimit.assign(
        cardId,
        Money.ofARS(50000),
        0.30,
        "admin"
    );
    
    assertThat(limit.getPurchaseLimit().getAmount().getAmount())
        .isEqualByComparingTo("50000.00");
    assertThat(limit.getCashAdvanceLimit().getAmount().getAmount())
        .isEqualByComparingTo("15000.00");
}

@Test
void shouldAutoResetDailyLimit() {
    // Día 1
    DailyLimit daily = DailyLimit.of(Money.ofARS(100000));
    daily = daily.consume(Money.ofARS(10000));
    
    // Simular cambio de día
    DailyLimit nextDay = DailyLimit.reconstituteFromPersistence(
        daily.getAmount(),
        daily.getConsumed(),
        LocalDate.now().minusDays(1)  // Fecha anterior
    );
    
    // Consumir nuevo día
    DailyLimit consumed = nextDay.consume(Money.ofARS(5000));
    
    assertThat(consumed.getConsumed().getAmount())
        .isEqualByComparingTo("5000.00");  // Reset automático
}
```

---

## 🚀 Uso

### Asignar Límite a Nueva Tarjeta

```java
// 1. Validar que tarjeta existe
if (!cardValidationPort.canAssignCreditLimit(cardId)) {
    throw new BusinessRuleException("Card cannot receive credit limit");
}

// 2. Asignar límite
CreditLimit limit = CreditLimit.assign(
    cardId,
    Money.ofARS(50000),
    0.30,
    "admin@bank.com"
);

// 3. Guardar
creditLimitRepository.save(limit);

// 4. Guardar historial
limit.getHistory().forEach(creditLimitRepository::saveHistory);
```

### Procesar Compra

```java
// 1. Obtener límite
CreditLimit limit = creditLimitRepository.findByCardId(cardId)
    .orElseThrow(() -> new NotFoundException("CreditLimit", cardId));

// 2. Validar
if (!limit.canPurchase(amount)) {
    throw new BusinessRuleException("Purchase cannot be approved");
}

// 3. Consumir
limit.consumeForPurchase(amount);

// 4. Guardar
creditLimitRepository.save(limit);
```

### Procesar Pago

```java
// 1. Obtener límite
CreditLimit limit = creditLimitRepository.findByCardId(cardId)
    .orElseThrow(() -> new NotFoundException("CreditLimit", cardId));

// 2. Liberar crédito
limit.releaseCredit(paymentAmount);

// 3. Guardar
creditLimitRepository.save(limit);
```

---

## 📚 Dependencias

### Internas

```xml
<dependency>
    <groupId>com.unisono.card</groupId>
    <artifactId>card-shared</artifactId>
</dependency>

<dependency>
    <groupId>com.unisono.card</groupId>
    <artifactId>card-data</artifactId>
</dependency>
```

### Integración con card-data

```java
@Component
public class CardValidationAdapter implements CardValidationPort {
    
    @Autowired
    private GetCardUseCase getCardUseCase;
    
    @Override
    public boolean canAssignCreditLimit(CardId cardId) {
        return getCardUseCase.findById(cardId)
            .map(card -> card.getStatus() == CardStatus.APPROVED
                      || card.getStatus() == CardStatus.ISSUED
                      || card.getStatus() == CardStatus.ACTIVE)
            .orElse(false);
    }
}
```

---

## 🔄 Migraciones (Flyway)

### V1__create_credit_limit_tables.sql

Crea:
- Tabla `credit_limits`
- Índices
- Trigger `update_credit_limit_updated_at`
- Function `update_credit_limit_updated_at()`

### V2__create_credit_limit_history_table.sql

Crea:
- Tabla `credit_limit_history`
- Índices
- Trigger `trigger_audit_credit_limit_changes`
- Function `audit_credit_limit_changes()`

### Ejecutar

```bash
mvn flyway:migrate
```

---

## 📖 Casos de Uso Reales

### 1. Cliente Nuevo - Asignación Inicial

```
Analista de Riesgo → Evalúa solicitud
Sistema → POST /api/v1/credit-limits
Backend → CreditLimit.assign()
Resultado → Límite $50,000 asignado
Historial → Registro ASSIGNED
```

### 2. Cliente VIP - Aumento de Límite

```
12 meses sin mora detectados
Sistema → PUT /api/v1/credit-limits/{id}/increase-purchase-limit
Backend → limit.increasePurchaseLimit()
Resultado → $50,000 → $75,000
Historial → Registro INCREASED
Email → Notificación al cliente
```

### 3. Compra Online

```
Cliente → Compra $5,000 en e-commerce
E-commerce → POST /api/v1/credit-limits/validate/purchase
Backend → limit.canPurchase()
Validación → ✓ PurchaseLimit, ✓ Available, ✓ Daily, ✓ Monthly
Response → APPROVED
E-commerce → Procesa compra
Backend → POST /api/v1/credit-limits/{id}/consume-purchase
Resultado → Consumido $5,000
```

### 4. Pago de Resumen

```
Cliente → Paga $3,000
Sistema → POST /api/v1/credit-limits/{id}/release-credit
Backend → limit.releaseCredit()
Resultado → Disponible aumenta $3,000
```

### 5. Exceso de Límite Diario

```
Cliente → Intenta comprar $10,000
Ya consumió → $95,000 hoy
Backend → POST /api/v1/credit-limits/validate/purchase
Validación → ✓ Purchase, ✓ Available, ✗ Daily ($10K > $5K disponible)
Response → REJECTED: "Se excede el límite diario"
E-commerce → Muestra error al cliente
```

---

## 📞 Soporte

**Equipo:** Credit Limits Team  
**Email:** limits@unisono.com  
**Slack:** #card-limits

---

## 📝 Changelog

### [1.0.0] - 2025-12-27

#### Added
- ✅ CreditLimit aggregate
- ✅ Value Objects con auto-reset (DailyLimit, MonthlyLimit)
- ✅ CreditLimitHistory entity
- ✅ Use Cases y Domain Services
- ✅ REST Controllers con Swagger
- ✅ Validación multi-nivel
- ✅ JPA Persistence
- ✅ Flyway migrations
- ✅ Integración con card-data
- ✅ Tests unitarios

---

**Última actualización:** Diciembre 2025  
**Puerto:** 8082  
**Mantenido por:** Unisono Development Team
```

---

## ✅ **ARCHIVOS CREADOS:**

```
card-management-service/
├── README.md                           ← Principal (completo)
├── card-shared/
│   └── README.md                       ← Específico de card-shared
├── card-data/
│   └── README.md                       ← Específico de card-data
└── card-limits/
└── README.md                       ← Específico de card-limits
```

---

¡Listo! 🎉 Ahora tenés **documentación completa y profesional** para cada módulo del proyecto.

Cada README incluye:
- ✅ Descripción funcional
- ✅ Componentes principales
- ✅ Ejemplos de código
- ✅ API endpoints
- ✅ Base de datos
- ✅ Testing
- ✅ Casos de uso reales
- ✅ Changelog