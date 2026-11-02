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
```java// Crear
CardId cardId = CardId.generate();// Desde String
CardId cardId = CardId.of("550e8400-e29b-41d4-a716-446655440000");// Obtener valor
UUID value = cardId.getValue();

**Características:**
- Formato: UUID v4
- Inmutable
- Validación automática de formato

---

#### **ClientId**
Identificador de cliente (DNI/CUIL argentino).
```java// DNI (8 dígitos)
ClientId clientId = ClientId.of("12345678");// CUIL (11 dígitos)
ClientId clientId = ClientId.of("20123456789");// Validación
boolean isValid = ClientId.isValid("12345678"); // true

**Características:**
- Formatos válidos: 7-11 dígitos
- Validación de DNI/CUIL argentino
- Solo dígitos numéricos
- Inmutable

---

#### **Money**
Representación de dinero con precisión decimal.
```java// Crear Money en ARS
Money amount = Money.ofARS(new BigDecimal("50000.00"));
Money amount = Money.ofARS(50000);// Crear Money con moneda específica
Money amount = Money.of(new BigDecimal("1000.00"), "USD");// Operaciones aritméticas
Money total = price.add(tax);
Money change = payment.subtract(price);
Money doubled = amount.multiply(BigDecimal.valueOf(2));
Money half = amount.divide(BigDecimal.valueOf(2));// Comparaciones
boolean isExpensive = price.isGreaterThan(budget);
boolean isCheap = price.isLessThan(maxPrice);
boolean isFree = price.isZero();// Obtener valores
BigDecimal value = amount.getAmount();
String currencyCode = amount.getCurrencyCode();
Currency currency = amount.getCurrency();

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
```java// Crear porcentaje
Percentage discount = Percentage.of(new BigDecimal("15.50"));// Validaciones
Percentage valid = Percentage.of(new BigDecimal("0"));     // 0%
Percentage valid = Percentage.of(new BigDecimal("100"));   // 100%// Error si está fuera de rango
Percentage invalid = Percentage.of(new BigDecimal("150")); // ❌ Exception// Aplicar a un monto
BigDecimal result = percentage.applyTo(new BigDecimal("1000"));// Obtener valor
BigDecimal value = percentage.getValue();

**Características:**
- Rango: 0 - 100
- Precisión: 4 decimales
- Inmutable
- Validación automática

---

### Jerarquía de ExcepcionesDomainException (abstract)
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

**Uso:**
```java// ValidationException
if (amount == null) {
throw new ValidationException("El monto no puede ser nulo");
}// NotFoundException
Card card = repository.findById(id)
.orElseThrow(() -> new NotFoundException("Card", id));// BusinessRuleException
if (!card.isOperational()) {
throw new BusinessRuleException(
"La tarjeta no está disponible para operar"
);
}

---

### Servicios de Seguridad

#### **EncryptionService**

Servicio de encriptación simétrica AES-256-GCM.
```java@Autowired
private EncryptionService encryptionService;// Encriptar
String plainText = "4532123456789012";
String encrypted = encryptionService.encrypt(plainText);
// Output: "IV:ciphertext" en Base64// Desencriptar
String decrypted = encryptionService.decrypt(encrypted);
// Output: "4532123456789012"

**Características:**
- Algoritmo: AES-256-GCM
- IV aleatorio por cada encriptación
- Encoding: Base64
- IV incluido en el resultado
- Thread-safe

**Configuración:**
```yamlencryption:
secret-key: your-256-bit-secret-key-here
algorithm: AES/GCM/NoPadding

**⚠️ IMPORTANTE:**
- En producción, usar un **vault** (Vault, AWS Secrets Manager)
- Nunca hardcodear keys en código
- Rotar keys periódicamente

---

#### **HashingService**

Servicio de hashing BCrypt para contraseñas/PINs.
```java@Autowired
private HashingService hashingService;// Hash
String pin = "1234";
String hashed = hashingService.hash(pin);
// Output: "2a$12
..."
// Verificar
boolean isValid = hashingService.verify(pin, hashed);
// Output: true

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

## 🏗️ Estructura del Módulocard-shared/
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

---

## 🧪 Testing

### Ejecutar Tests
```bashcd card-shared
mvn test

### Cobertura
```bashmvn clean test jacoco:report
open target/site/jacoco/index.html

**Target de cobertura:** >90%

### Ejemplos de Tests
```java@Test
void shouldCreateMoneyWithValidAmount() {
Money money = Money.ofARS(new BigDecimal("1000.00"));assertThat(money.getAmount())
    .isEqualByComparingTo(new BigDecimal("1000.00"));
}@Test
void shouldThrowExceptionWhenAddingDifferentCurrencies() {
Money ars = Money.ofARS(new BigDecimal("100"));
Money usd = Money.of(new BigDecimal("100"), "USD");assertThatThrownBy(() -> ars.add(usd))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("different currencies");
}@Test
void shouldEncryptAndDecryptSuccessfully() {
String original = "4532123456789012";String encrypted = encryptionService.encrypt(original);
String decrypted = encryptionService.decrypt(encrypted);assertThat(decrypted).isEqualTo(original);
assertThat(encrypted).isNotEqualTo(original);
}

---

## 📚 Dependencias

### Maven
```xml<dependency>
    <groupId>com.unisono.card</groupId>
    <artifactId>card-shared</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
````
Dependencias Externas

Lombok - Reducción de boilerplate
Spring Boot Starter - Framework base
BCrypt - Hashing
SLF4J - Logging


🔒 Consideraciones de Seguridad
Encriptación
✅ HACER:

Usar EncryptionService para PAN, CVV
Almacenar keys en vault (Vault, AWS Secrets Manager)
Rotar keys periódicamente
Validar inputs antes de encriptar

❌ NO HACER:

Hardcodear keys en código
Compartir keys entre ambientes
Loggear datos encriptados sin sanitizar
Reutilizar IVs

Hashing
✅ HACER:

Usar HashingService para PINs, passwords
Verificar con verify(), nunca comparar hashes directamente
Usar strength >= 12

❌ NO HACER:

Almacenar PINs en texto plano
Loggear PINs (ni hasheados)
Usar MD5 o SHA1
Intentar "desencriptar" un hash


📖 Patrones Utilizados
Value Objects
Todos los Value Objects siguen estos principios:

Inmutabilidad - No tienen setters
Validación en constructor - Fail fast
Equality por valor - equals() y hashCode() basados en valor
Self-validation - Se validan a sí mismos
Encapsulamiento - Esconden detalles de implementación

Factory Methods
// Named constructors para claridad
Money.ofARS(amount)
Money.of(amount, currency)
Money.zero()CardId.generate()
CardId.of(uuid)ClientId.of(dni)

---

## 🚀 Uso en Otros Módulos

### card-data
```java// Value Objects
private CardId id;
private ClientId clientId;
private Money amount;// Excepciones
if (!card.isOperational()) {
throw new BusinessRuleException("Card not operational");
}// Seguridad
String encryptedPAN = encryptionService.encrypt(pan);
String hashedPIN = hashingService.hash(pin);

### card-limits
```java// Value Objects
private Money purchaseLimit;
private Money availableCredit;
private Percentage cashAdvancePercentage;// Operaciones
Money newLimit = currentLimit.add(increase);
Money available = totalLimit.subtract(consumed);

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
