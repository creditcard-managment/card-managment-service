# 💳 Card Management Service

Sistema de gestión de tarjetas de crédito bancarias construido con arquitectura hexagonal y Domain-Driven Design (DDD).

## 📋 Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Arquitectura](#arquitectura)
- [Módulos](#módulos)
    - [card-shared](#card-shared)
    - [card-data](#card-data)
    - [card-limits](#card-limits)
- [Tecnologías](#tecnologías)
- [Requisitos Previos](#requisitos-previos)
- [Instalación](#instalación)
- [Configuración](#configuración)
- [Ejecución](#ejecución)
- [Documentación API](#documentación-api)
- [Base de Datos](#base-de-datos)
- [Testing](#testing)
- [Roadmap](#roadmap)
- [Contribución](#contribución)

---

## 🎯 Descripción General

**Card Management Service** es una plataforma bancaria moderna para la gestión completa del ciclo de vida de tarjetas de crédito. El sistema está diseñado siguiendo principios de Clean Architecture, DDD y SOLID, permitiendo escalabilidad, mantenibilidad y facilidad de testing.

### Características Principales

- ✅ Gestión completa del ciclo de vida de tarjetas
- ✅ Control de límites de crédito en tiempo real
- ✅ Validación de transacciones contra múltiples límites
- ✅ Historial completo de auditoría
- ✅ Encriptación de datos sensibles (PAN, CVV)
- ✅ API REST documentada con OpenAPI/Swagger
- ✅ Arquitectura modular y desacoplada
- ✅ Soft delete para preservar datos históricos

---

## 🏗️ Arquitectura

### Patrón Arquitectónico

El sistema implementa **Hexagonal Architecture (Ports & Adapters)** combinada con **Domain-Driven Design**:

¡Perfecto! Te genero un README.md completo y profesional para el proyecto 🚀

README.md
Ubicación: card-management-service/README.md
markdown# 💳 Card Management Service

Sistema de gestión de tarjetas de crédito bancarias construido con arquitectura hexagonal y Domain-Driven Design (DDD).

## 📋 Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Arquitectura](#arquitectura)
- [Módulos](#módulos)
    - [card-shared](#card-shared)
    - [card-data](#card-data)
    - [card-limits](#card-limits)
- [Tecnologías](#tecnologías)
- [Requisitos Previos](#requisitos-previos)
- [Instalación](#instalación)
- [Configuración](#configuración)
- [Ejecución](#ejecución)
- [Documentación API](#documentación-api)
- [Base de Datos](#base-de-datos)
- [Testing](#testing)
- [Roadmap](#roadmap)
- [Contribución](#contribución)

---

## 🎯 Descripción General

**Card Management Service** es una plataforma bancaria moderna para la gestión completa del ciclo de vida de tarjetas de crédito. El sistema está diseñado siguiendo principios de Clean Architecture, DDD y SOLID, permitiendo escalabilidad, mantenibilidad y facilidad de testing.

### Características Principales

- ✅ Gestión completa del ciclo de vida de tarjetas
- ✅ Control de límites de crédito en tiempo real
- ✅ Validación de transacciones contra múltiples límites
- ✅ Historial completo de auditoría
- ✅ Encriptación de datos sensibles (PAN, CVV)
- ✅ API REST documentada con OpenAPI/Swagger
- ✅ Arquitectura modular y desacoplada
- ✅ Soft delete para preservar datos históricos

---

## 🏗️ Arquitectura

### Patrón Arquitectónico

El sistema implementa **Hexagonal Architecture (Ports & Adapters)** combinada con **Domain-Driven Design**:
```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                     │
│  (REST Controllers, DTOs, Mappers, Exception Handlers)  │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                     Domain Layer                         │
│   (Aggregates, Entities, Value Objects, Use Cases)      │
│   (Business Logic, Domain Services, Domain Events)      │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                 Infrastructure Layer                     │
│   (JPA Entities, Repositories, Adapters, Persistence)   │
└─────────────────────────────────────────────────────────┘
```

### Vertical Slicing

El proyecto está organizado por **capacidades de negocio** (vertical slicing) en lugar de capas técnicas:
```
card-management-service/
├── card-shared/          # Foundation (compartido)
├── card-data/           # Gestión de tarjetas
├── card-limits/         # Límites de crédito
├── card-lifecycle/      # Ciclo de vida (futuro)
├── card-relationships/  # Tarjetas adicionales (futuro)
├── card-events/         # Eventos y Kafka (futuro)
└── card-boot/          # Ensamblado final (futuro)
```

---

## 📦 Módulos

### **card-shared**

> **Módulo fundacional con componentes compartidos**

#### Descripción Funcional

Contiene los **building blocks** reutilizables por todos los demás módulos. Proporciona los elementos básicos del dominio que no cambian entre contextos.

#### Componentes Principales

**Value Objects:**
- `CardId` - Identificador único de tarjeta (UUID)
- `ClientId` - Identificador de cliente (DNI/CUIL argentino, 7-11 dígitos)
- `Money` - Representación de dinero con precisión decimal
    - Soporte multi-moneda (Currency)
    - Operaciones aritméticas seguras
    - Validaciones de precisión (2 decimales)
- `Percentage` - Porcentajes (0-100%, 4 decimales)

**Jerarquía de Excepciones:**
```
DomainException
├── ValidationException      (datos inválidos)
├── NotFoundException        (entidad no encontrada)
└── BusinessRuleException    (regla de negocio violada)
```

**Servicios de Seguridad:**
- `EncryptionService`
    - Algoritmo: AES-256-GCM
    - IV aleatorio por encriptación
    - Encoding: Base64
- `HashingService`
    - Algoritmo: BCrypt
    - Strength: 12
    - Para almacenamiento seguro de PINs

#### Casos de Uso

- ✅ Validación de montos monetarios
- ✅ Encriptación de PAN y CVV
- ✅ Hashing de PINs
- ✅ Validación de identificadores argentinos
- ✅ Manejo consistente de errores

#### Tests

- Cobertura: >90%
- Tests unitarios de Value Objects
- Tests de servicios de seguridad
- Validación de reglas de negocio

---

### **card-data**

> **Gestión del ciclo de vida de tarjetas de crédito**

#### Descripción Funcional

Módulo core que gestiona la **creación, activación, bloqueo y cancelación** de tarjetas de crédito. Implementa el aggregate `Card` que protege todas las invariantes del dominio relacionadas con el estado y los datos de la tarjeta.

#### Modelo de Dominio

**Aggregate Root: `Card`**

Atributos principales:
- Identificación: `CardId`, `ClientId`
- Datos del titular: `CardHolder` (entity)
- Datos de la tarjeta: `CardNumber`, `CVV`, `PIN`, `ExpirationDate`
- Clasificación: `CardType` (VISA, MASTERCARD, AMEX, CABAL, NARANJA)
- Categoría: `CardCategory` (CLASSIC, GOLD, PLATINUM, BLACK, INFINITE)
- Estado: `CardStatus` (REQUESTED, APPROVED, ISSUED, ACTIVE, BLOCKED, EXPIRED, CANCELLED)

**Value Objects Especializados:**
- `CardNumber`
    - 13-19 dígitos
    - Validación Luhn
    - Detección automática de tipo (BIN)
    - Máscara: `**** **** **** 1234`
- `CVV`
    - 3-4 dígitos
    - Siempre encriptado
- `PIN`
    - 4 dígitos
    - Validación anti-secuencial (no permite 1234, 1111)
    - Hasheado con BCrypt
- `ExpirationDate`
    - Formato MM/YY
    - Validación no vencida
    - Máximo 10 años al futuro

**Enums:**
- `CardType` - Tipo de red de pago (con detección por BIN)
- `CardCategory` - Categoría con límites sugeridos
- `CardStatus` - Estados del ciclo de vida

#### Máquina de Estados
```
REQUESTED → APPROVE/REJECT
    ↓
APPROVED → ISSUE
    ↓
ISSUED → ACTIVATE
    ↓
ACTIVE ⇄ BLOCKED ⇄ BLOCKED_FRAUD
    ↓         ↓
EXPIRED   CANCELLED (estado final)
```

#### Funcionalidades

**Creación y Emisión:**
- `create()` - Crea tarjeta en estado REQUESTED
- `approve()` - Aprueba solicitud
- `reject()` - Rechaza solicitud
- `issue()` - Asigna PAN, CVV, PIN tras aprobación

**Gestión del Ciclo de Vida:**
- `activate()` - Activa tarjeta emitida
- `block()` - Bloquea temporalmente
- `blockForFraud()` - Bloqueo por fraude
- `unblock()` - Desbloquea tarjeta
- `cancel()` - Cancela permanentemente
- `markAsExpired()` - Marca como vencida

**Gestión de PIN:**
- `resetPIN()` - Cambia PIN (requiere validar PIN anterior)
- `isPINSet()` - Verifica si tiene PIN
- Validación con BCrypt

**Validaciones:**
- `isOperational()` - Puede operar
- `isBlocked()` - Está bloqueada
- `isNearExpiration()` - Por vencer (30 días)
- `requiresRenewal()` - Requiere renovación

#### Endpoints REST

**Base URL:** `/api/v1/cards`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/` | Crear tarjeta |
| GET | `/{cardId}` | Obtener por ID |
| GET | `/client/{clientId}` | Obtener todas del cliente |
| GET | `/search?lastFourDigits=1234&clientId=12345678` | Buscar |
| POST | `/{cardId}/block` | Bloquear |
| POST | `/{cardId}/block-fraud` | Bloquear por fraude |
| POST | `/{cardId}/unblock` | Desbloquear |
| POST | `/{cardId}/validate-pin` | Validar PIN |
| POST | `/{cardId}/reset-pin` | Resetear PIN |

#### Base de Datos

**Tabla: `cards`**
- Soft delete
- Índices en: `client_id`, `card_number_encrypted`, `last_four_digits`, `status`
- Trigger: `update_updated_at_column`
- Auditoría completa

**Tabla: `card_audit`** (opcional)
- Registra cambios de estado automáticamente
- Metadata JSONB
- Trazabilidad completa

#### Seguridad

- **PAN y CVV**: Encriptados con AES-256-GCM
- **PIN**: Hasheado con BCrypt (nunca almacenado en texto plano)
- **Visualización**: Máscara `**** **** **** 1234`
- **Validación**: PIN no secuencial ni repetido

#### Casos de Uso Reales

1. **Solicitud de tarjeta nueva**
```
   POST /api/v1/cards
   → Card en estado REQUESTED
```

2. **Aprobación y emisión**
```
   Backend: card.approve() → APPROVED
   Backend: card.issue(pan, cvv, pin) → ISSUED
```

3. **Activación por cliente**
```
   POST /api/v1/cards/{id}/activate
   → Card en estado ACTIVE
```

4. **Bloqueo temporal**
```
   POST /api/v1/cards/{id}/block
   Body: { "reason": "Tarjeta perdida" }
   → Card en estado BLOCKED
```

5. **Cambio de PIN**
```
   POST /api/v1/cards/{id}/reset-pin
   Body: { "oldPin": "1234", "newPin": "5678" }
   → PIN actualizado
```

#### Tests

- Tests unitarios de Value Objects
- Tests de reglas de negocio
- Tests de transiciones de estado
- Cobertura objetivo: >80%

---

### **card-limits**

> **Gestión de límites de crédito y validación de transacciones**

#### Descripción Funcional

Módulo responsable de **gestionar límites de crédito** de las tarjetas y **validar transacciones** en tiempo real contra múltiples tipos de límites (compras, adelantos, diarios, mensuales). Mantiene un **historial completo** de todos los cambios realizados.

#### Modelo de Dominio

**Aggregate Root: `CreditLimit`**

Atributos principales:
- Identificación: `id`, `cardId`
- Límites principales:
    - `PurchaseLimit` - Límite total de compras
    - `CashAdvanceLimit` - Límite de adelantos (típicamente 30% del límite de compras)
- Estado actual:
    - `ConsumedCredit` - Crédito consumido
    - `AvailableCredit` - Crédito disponible (calculado)
- Límites temporales (auto-reset):
    - `DailyLimit` - Límite diario (resetea a las 00:00)
    - `MonthlyLimit` - Límite mensual (resetea el día 1)
- Historial: Collection de `CreditLimitHistory`

**Value Objects:**
- `PurchaseLimit` - Límite de compras ($1,000 - $10,000,000)
- `CashAdvanceLimit` - Límite de adelantos ($500 - $5,000,000)
- `AvailableCredit` - Crédito disponible (calculado = Total - Consumido)
- `ConsumedCredit` - Crédito usado actual
- `DailyLimit` - Límite diario con auto-reset
    - Resetea automáticamente cada día
    - Default: $100,000
- `MonthlyLimit` - Límite mensual con auto-reset
    - Resetea automáticamente cada mes
    - Default: $500,000

**Entity:**
- `CreditLimitHistory` - Registro de cambio
    - `LimitType` - PURCHASE, CASH_ADVANCE, DAILY, MONTHLY
    - `action` - ASSIGNED, INCREASED, DECREASED, UPDATED
    - Valores: previous, new, difference
    - Contexto: reason, changedBy, timestamp

#### Invariantes Protegidas

1. ✅ Adelantos ≤ Compras
2. ✅ Consumido ≤ Límite Total
3. ✅ Disponible = Total - Consumido
4. ✅ No reducir límite por debajo del consumido

#### Funcionalidades

**Asignación de Límites:**
- `assign()` - Asigna límite inicial a tarjeta
- Validación: tarjeta debe existir y estar en estado válido
- Porcentaje de adelantos configurable (default 30%)
- Límites temporales opcionales

**Actualización de Límites:**
- `increasePurchaseLimit()` - Aumenta límite de compras
- `decreasePurchaseLimit()` - Reduce límite (validación vs consumido)
- `updateCashAdvanceLimit()` - Actualiza adelantos (validación vs compras)
- `updateDailyLimit()` - Actualiza límite diario
- `updateMonthlyLimit()` - Actualiza límite mensual

**Consumo y Liberación:**
- `consumeForPurchase()` - Registra compra
    - Valida límite de compras
    - Valida crédito disponible
    - Valida límite diario
    - Valida límite mensual
- `consumeForCashAdvance()` - Registra adelanto
    - Validaciones específicas de adelantos
- `releaseCredit()` - Libera crédito (pago)

**Validación Pre-Transaccional:**
- `canPurchase()` - ¿Puede realizar compra?
- `canCashAdvance()` - ¿Puede realizar adelanto?
- Retorna true/false sin modificar estado

**Historial:**
- Registro automático de todos los cambios
- Auditoría completa: quién, cuándo, qué cambió
- Consulta por tipo de límite
- Análisis temporal

#### Endpoints REST

**Base URL:** `/api/v1/credit-limits`

**Gestión de Límites:**
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/` | Asignar límite |
| GET | `/{id}` | Obtener por ID |
| GET | `/card/{cardId}` | Obtener por tarjeta |
| GET | `/card/{cardId}/info` | Info detallada |

**Actualización:**
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| PUT | `/{id}/increase-purchase-limit` | Aumentar compras |
| PUT | `/{id}/decrease-purchase-limit` | Reducir compras |
| PUT | `/{id}/update-cash-advance-limit` | Actualizar adelantos |
| PUT | `/{id}/update-temporal-limits` | Actualizar diario/mensual |

**Consumo y Liberación:**
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/{id}/consume-purchase` | Consumir (compra) |
| POST | `/{id}/consume-cash-advance` | Consumir (adelanto) |
| POST | `/{id}/release-credit` | Liberar (pago) |

**Validación:**
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/validate/purchase` | Validar compra |
| POST | `/validate/cash-advance` | Validar adelanto |

**Historial:**
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/{id}/history` | Historial completo |

#### Base de Datos

**Tabla: `credit_limits`**
- Un límite por tarjeta (UNIQUE en `card_id`)
- Almacena todos los límites y consumos
- Índices en: `card_id`, temporal dates
- Soft delete
- Trigger: `update_updated_at_column`

**Tabla: `credit_limit_history`**
- Auditoría de cambios
- FK a `credit_limits`
- Índices en: `credit_limit_id`, `changed_at`, `action`, `limit_type`
- Trigger automático para auditar cambios en purchase_limit

#### Auto-Reset de Límites Temporales

**DailyLimit:**
```java
// Resetea automáticamente si cambió el día
if (!resetDate.equals(LocalDate.now())) {
    return new DailyLimit(amount, Money.zero(), LocalDate.now());
}
```

**MonthlyLimit:**
```java
// Resetea automáticamente si cambió el mes
if (!resetMonth.equals(YearMonth.now())) {
    return new MonthlyLimit(amount, Money.zero(), YearMonth.now());
}
```

#### Casos de Uso Reales

1. **Asignación de límite a nueva tarjeta**
```
   POST /api/v1/credit-limits
   Body: {
     "cardId": "550e8400-e29b-41d4-a716-446655440000",
     "purchaseLimitAmount": 50000.00,
     "currency": "ARS",
     "cashAdvancePercentage": 0.30,  // 30%
     "assignedBy": "admin@bank.com"
   }
   
   → PurchaseLimit: $50,000
   → CashAdvanceLimit: $15,000 (30%)
   → DailyLimit: $100,000 (default)
   → MonthlyLimit: $500,000 (default)
```

2. **Aumento de límite por buen comportamiento**
```
   PUT /api/v1/credit-limits/{id}/increase-purchase-limit
   Body: {
     "amount": 25000.00,
     "currency": "ARS",
     "reason": "Buen historial crediticio",
     "changedBy": "risk@bank.com"
   }
   
   → Límite anterior: $50,000
   → Límite nuevo: $75,000
   → Registro en historial automático
```

3. **Validación pre-compra**
```
   POST /api/v1/credit-limits/validate/purchase
   Body: {
     "cardId": "550e8400-e29b-41d4-a716-446655440000",
     "amount": 5000.00,
     "currency": "ARS"
   }
   
   Response: {
     "approved": true,
     "reason": "Transacción aprobada",
     "availableCredit": 45000.00,
     "requestedAmount": 5000.00
   }
```

4. **Consumo de crédito (compra)**
```
   POST /api/v1/credit-limits/{id}/consume-purchase
   Body: {
     "amount": 5000.00,
     "currency": "ARS",
     "description": "Compra en Mercado Libre"
   }
   
   → ConsumedCredit: $5,000
   → AvailableCredit: $45,000
   → DailyLimit consumed: $5,000
   → MonthlyLimit consumed: $5,000
```

5. **Liberación de crédito (pago)**
```
   POST /api/v1/credit-limits/{id}/release-credit
   Body: {
     "paymentAmount": 3000.00,
     "currency": "ARS",
     "description": "Pago de resumen"
   }
   
   → ConsumedCredit: $2,000
   → AvailableCredit: $48,000
```

6. **Consulta de historial**
```
   GET /api/v1/credit-limits/{id}/history
   
   Response: [
     {
       "limitType": "PURCHASE",
       "action": "ASSIGNED",
       "previousValue": 0,
       "newValue": 50000.00,
       "changedBy": "admin@bank.com",
       "changedAt": "2025-01-15T10:30:00"
     },
     {
       "limitType": "PURCHASE",
       "action": "INCREASED",
       "previousValue": 50000.00,
       "newValue": 75000.00,
       "difference": 25000.00,
       "reason": "Buen historial crediticio",
       "changedBy": "risk@bank.com",
       "changedAt": "2025-03-20T14:15:00"
     }
   ]
```

#### Integración con card-data

`card-limits` se integra con `card-data` mediante:
- `CardValidationPort` - Valida que tarjeta existe y puede recibir límite
- Estados válidos: APPROVED, ISSUED, ACTIVE

#### Tests

- Tests unitarios de Value Objects
- Tests de auto-reset (DailyLimit, MonthlyLimit)
- Tests de validaciones múltiples
- Tests de historial
- Cobertura objetivo: >80%

---

## 🛠️ Tecnologías

### Backend
- **Java 17** - LTS version
- **Spring Boot 3.3.5** - Framework principal (Spring Framework 6.1, Spring Data JPA 3.3, Hibernate 6.5, Spring Kafka 3.2, Micrometer 1.13)
- **Spring Data JPA** - Persistencia
- **Hibernate** - ORM
- **Maven** - Gestión de dependencias

> **Versiones:** el `spring-boot-starter-parent` del `pom.xml` raíz es la única fuente de versiones de Spring Boot y de todo lo que gestiona (Spring, Hibernate, Kafka, Micrometer, Jackson, PostgreSQL, Lombok, JUnit, Mockito, Testcontainers). No se importa otro BOM de Boot ni se redefinen sus propiedades (`spring-kafka.version`, `mockito.version`, …), porque pisan la versión gestionada. Solo se fijan versiones de lo que Boot no gestiona: MapStruct, springdoc, Bouncy Castle y Spring State Machine.

### Base de Datos
- **PostgreSQL 16** - Base de datos relacional
- **Flyway 11.1.0** - Migraciones versionadas
- **HikariCP** - Connection pooling

### Seguridad
- **AES-256-GCM** - Encriptación de PAN/CVV
- **BCrypt** - Hashing de PINs
- **Spring Security** (futuro)

### Documentación
- **SpringDoc OpenAPI 3** - Documentación API
- **Swagger UI** - Interfaz interactiva

### Testing
- **JUnit 5** - Framework de testing
- **Mockito** - Mocking
- **AssertJ** - Assertions fluidas
- **Testcontainers** - Tests de integración

### Herramientas
- **Lombok** - Reducción de boilerplate
- **MapStruct** - Mapeo de objetos
- **SLF4J + Logback** - Logging

---

## 📋 Requisitos Previos

- **Java 17** o superior
- **Maven 3.8+**
- **Docker** y **Docker Compose** (para PostgreSQL)
- **Git**
- **IDE** recomendado: IntelliJ IDEA, Eclipse, VS Code

---

## 🚀 Instalación

### 1. Clonar el repositorio
```bash
git clone https://github.com/tu-usuario/card-management-service.git
cd card-management-service
```

### 2. Compilar el proyecto
```bash
mvn clean install
```

Esto compilará todos los módulos:
- ✅ card-shared
- ✅ card-data
- ✅ card-limits

---

## ⚙️ Configuración

### Base de Datos

El proyecto incluye `docker-compose.yml` con PostgreSQL configurado:
```yaml
services:
  postgres:
    image: postgresql:16-alpine
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: card_management_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
```

### Variables de Entorno

Para **producción**, configura estas variables:
```bash
# Database
export DB_URL=jdbc:postgresql://localhost:5432/card_management_db
export DB_USERNAME=postgres
export DB_PASSWORD=your-secure-password

# Encryption (usar un vault en producción)
export ENCRYPTION_KEY=your-256-bit-key
export ENCRYPTION_SALT=your-salt
```

### Perfiles de Spring

- `dev` - Desarrollo (logs DEBUG, show-sql)
- `test` - Testing (H2 en memoria)
- `prod` - Producción (logs WARN, sin show-sql)

Cambiar perfil:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## 🏃 Ejecución

### 1. Levantar PostgreSQL
```bash
docker-compose up -d postgres
```

Verificar:
```bash
docker-compose ps
docker-compose logs postgres
```

### 2. Ejecutar Flyway (Migraciones)

**card-data:**
```bash
cd card-data
mvn flyway:migrate
```

**card-limits:**
```bash
cd card-limits
mvn flyway:migrate
```

### 3. Levantar las aplicaciones

**card-data** (puerto 8081):
```bash
cd card-data
mvn spring-boot:run
```

**card-limits** (puerto 8082):
```bash
cd card-limits
mvn spring-boot:run
```

### 4. Verificar Health
```bash
# card-data
curl http://localhost:8081/actuator/health

# card-limits
curl http://localhost:8082/actuator/health
```

---

## 📚 Documentación API

### Swagger UI

**card-data:**
- URL: http://localhost:8081/swagger-ui.html
- API Docs: http://localhost:8081/api-docs

**card-limits:**
- URL: http://localhost:8082/swagger-ui.html
- API Docs: http://localhost:8082/api-docs

### Ejemplos de Requests

#### Crear Tarjeta
```bash
POST http://localhost:8081/api/v1/cards
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

#### Asignar Límite
```bash
POST http://localhost:8082/api/v1/credit-limits
Content-Type: application/json

{
  "cardId": "550e8400-e29b-41d4-a716-446655440000",
  "purchaseLimitAmount": 50000.00,
  "currency": "ARS",
  "cashAdvancePercentage": 0.30,
  "assignedBy": "admin@bank.com"
}
```

#### Validar Compra
```bash
POST http://localhost:8082/api/v1/credit-limits/validate/purchase
Content-Type: application/json

{
  "cardId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 5000.00,
  "currency": "ARS"
}
```

---

## 🗄️ Base de Datos

### Esquema Principal

**Módulo card-data:**
- `cards` - Datos de tarjetas
- `card_audit` - Auditoría de cambios

**Módulo card-limits:**
- `credit_limits` - Límites de crédito
- `credit_limit_history` - Historial de cambios

### Diagrama ER (Simplificado)
```
┌──────────────┐          ┌──────────────────┐
│    cards     │ 1     1  │  credit_limits   │
│              │──────────│                  │
│ id (PK)      │          │ id (PK)          │
│ client_id    │          │ card_id (FK)     │
│ card_number  │          │ purchase_limit   │
│ status       │          │ available_credit │
│ ...          │          │ ...              │
└──────────────┘          └──────────────────┘
       │                          │
       │ 1                        │ 1
       │                          │
       │ N                        │ N
       ↓                          ↓
┌──────────────┐          ┌───────────────────────┐
│  card_audit  │          │ credit_limit_history  │
│              │          │                       │
│ id (PK)      │          │ id (PK)               │
│ card_id (FK) │          │ credit_limit_id (FK)  │
│ action       │          │ limit_type            │
│ ...          │          │ action                │
└──────────────┘          └───────────────────────┘
```

### Conectarse a PostgreSQL
```bash
# Desde Docker
docker exec -it card-management-postgres psql -U postgres -d card_management_db

# Queries útiles
\dt                        -- Listar tablas
\d cards                   -- Describir tabla
SELECT * FROM cards;       -- Ver datos
```

---

## 🧪 Testing

### Ejecutar Tests

**Todos los módulos:**
```bash
mvn test
```

**Módulo específico:**
```bash
cd card-data
mvn test
```

### Cobertura
```bash
# Generar reporte JaCoCo
mvn clean test jacoco:report

# Ver reporte
open target/site/jacoco/index.html
```

### Tests de Integración
```bash
# Con Testcontainers (requiere Docker)
mvn verify
```

---

## 🗺️ Roadmap

### ✅ Completado (v1.0.0)
- [x] card-shared - Componentes fundacionales
- [x] card-data - Gestión de tarjetas
- [x] card-limits - Límites de crédito

### 🚧 En Progreso
- [ ] card-lifecycle - State Machine con Spring State Machine
- [ ] card-relationships - Tarjetas adicionales
- [ ] card-events - Kafka integration

### 📅 Futuro (v2.0.0)
- [ ] card-transactions - Gestión de transacciones
- [ ] card-payments - Procesamiento de pagos
- [ ] card-rewards - Sistema de recompensas
- [ ] card-notifications - Notificaciones en tiempo real
- [ ] card-analytics - Analytics y reportes
- [ ] card-boot - Aplicación ensamblada

### 🔐 Mejoras de Seguridad
- [ ] Integrar Spring Security
- [ ] OAuth2/JWT
- [ ] Rate limiting
- [ ] Vault para secrets

### 🚀 Infraestructura
- [ ] Kubernetes deployment
- [ ] CI/CD pipeline
- [ ] Monitoring con Prometheus/Grafana
- [ ] Distributed tracing

---

## 🤝 Contribución

### Guía de Contribución

1. Fork el proyecto
2. Crea una branch para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la branch (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

### Convenciones de Código

- **Java**: Google Java Style Guide
- **Commits**: Conventional Commits
- **Branches**:
    - `feature/` - Nuevas funcionalidades
    - `bugfix/` - Corrección de bugs
    - `hotfix/` - Fixes urgentes
    - `refactor/` - Refactorización

### Code Review

Todos los PRs requieren:
- ✅ Tests pasando
- ✅ Cobertura >80%
- ✅ Code review aprobado
- ✅ Sin conflictos

---
