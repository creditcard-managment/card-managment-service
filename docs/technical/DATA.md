# card-data — Documentación técnica funcional

> Microservicio dueño del **Aggregate Root `Card`**: alta de solicitudes de tarjeta, datos del titular, datos sensibles (PAN, CVV, PIN), bloqueo/desbloqueo y consultas.

| Atributo | Valor |
|---|---|
| Artefacto Maven | `com.unisono.card:card-data` (`1.0.0-SNAPSHOT`) |
| Clase principal | `com.unisono.card.data.CardDataApplication` |
| Puerto | `8081` |
| Base path REST | `/api/v1/cards` |
| Base de datos | PostgreSQL `card_management_db`, tabla `cards` |
| Mensajería | Kafka (productor) — topic `card.created` |
| Dependencias internas | `card-shared` (Value Objects, excepciones, cifrado), `card-events` (eventos y topics) |
| Swagger UI | `http://localhost:8081/swagger-ui.html` |

---

## 1. Responsabilidad dentro del sistema

`card-data` es la **fuente de verdad de la tarjeta**. El resto de los módulos no persisten la tarjeta: la consultan o la modifican a través de este servicio.

```mermaid
flowchart LR
    Client[Cliente / Canal] -->|REST| CD[card-data :8081]
    CD -->|card.created| K[(Kafka)]
    K --> CL[card-lifecycle :8083]
    CL -->|PUT /status<br/>PUT /activated-at<br/>PUT /card-data| CD
    PP[payment-processing :8085] -.->|validación de tarjeta| CD
    CD --> DB[(PostgreSQL<br/>tabla cards)]
```

- **Crea** la tarjeta en estado `REQUESTED` y notifica al ecosistema vía Kafka.
- **card-lifecycle** consume `card.created`, gestiona aprobación/emisión/activación y escribe de vuelta en `card-data` el nuevo estado, la fecha de activación y los datos generados en la emisión (PAN, CVV, PIN, vencimiento).
- **Expone** operaciones de seguridad (bloqueo, bloqueo por fraude, desbloqueo) y de PIN (validación y cambio).

---

## 2. Arquitectura

El módulo sigue **arquitectura hexagonal (Ports & Adapters)**:

```
com.unisono.card.data
├── domain
│   ├── model          Card (aggregate root), CardHolder, CardType, CardCategory
│   ├── port.input     Casos de uso (CreateCard, GetCard, BlockCard, UnblockCard,
│   │                  ValidatePIN, ResetPIN, UpdateCardStatus)
│   ├── port.output    CardRepository, EncryptionPort
│   ├── service        CardManagementService, CardSecurityService,
│   │                  PINManagementService, CardValidationService, CreateCardService
│   └── event          CardCreatedDomainEvent (evento interno de Spring)
├── application
│   ├── dto            Requests / Responses de la API
│   └── mapper         CardDtoMapper (DTO ⇄ dominio)
└── infrastructure
    ├── adapter.input.rest           CardController, CardSecurityController,
    │                                PINController, GlobalExceptionHandler
    ├── adapter.output.persistence   CardRepositoryAdapter, JpaCardRepository,
    │                                CardEntity, CardEntityMapper
    ├── adapter.output.encryption    CardEncryptionAdapter
    ├── adapter.output.event         CardEventListener, KafkaEventPublisher
    └── config                       DataSource (Hikari), Kafka producer,
                                     Use cases, Swagger
```

### Mapa de casos de uso → servicios → endpoints

| Caso de uso (port input) | Implementación | Endpoint |
|---|---|---|
| `CreateCardUseCase` | `CardManagementService` | `POST /api/v1/cards` |
| `GetCardUseCase` | `CardManagementService` | `GET /{cardId}`, `GET /client/{clientId}`, `GET /search` |
| `BlockCardUseCase` | `CardSecurityService` | `POST /{cardId}/block`, `POST /{cardId}/block-fraud` |
| `UnblockCardUseCase` | `CardSecurityService` | `POST /{cardId}/unblock` |
| `ValidatePINUseCase` | `PINManagementService` | `POST /{cardId}/validate-pin` |
| `ResetPINUseCase` | `PINManagementService` | `POST /{cardId}/reset-pin` |
| *(métodos directos, sin port)* | `CardManagementService` | `PUT /{cardId}/status`, `PUT /{cardId}/activated-at`, `PUT /{cardId}/card-data` |

> `UpdateCardStatusUseCase` está declarado pero no tiene implementación; la actualización de estado se hace con `CardManagementService.updateStatus(...)`.

---

## 3. Modelo de dominio

### 3.1 `Card` (Aggregate Root)

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `CardId` (UUID) | Generado en `Card.create(...)` |
| `clientId` | `ClientId` | DNI/CUIL del cliente (7–11 dígitos) |
| `holder` | `CardHolder` | Titular |
| `type` | `CardType` | Red de pago |
| `category` | `CardCategory` | Categoría comercial |
| `status` | `CardStatus` | Estado del ciclo de vida (definido en `card-shared`) |
| `cardNumber` | `CardNumber` | PAN — se completa en la emisión |
| `cvv` | `CVV` | Se completa en la emisión |
| `pin` | `PIN` | **Transitorio**, solo en memoria; nunca se persiste en claro |
| `pinHash` | `String` | Hash BCrypt del PIN (persistido) |
| `expirationDate` | `ExpirationDate` | Mes/año de vencimiento |
| `createdAt`, `updatedAt` | `LocalDateTime` | Auditoría |
| `issuedAt`, `activatedAt`, `blockedAt`, `cancelledAt`, `expiredAt` | `LocalDateTime` | Hitos del ciclo de vida |
| `blockReason`, `cancelledReason` | `String` | Motivos |
| `deleted`, `deletedAt` | `Boolean`, `LocalDateTime` | Soft delete |

**Comportamiento del aggregate**

| Método | Efecto |
|---|---|
| `create(clientId, holder, type, category)` | Valida no-nulos; nueva tarjeta en `REQUESTED` |
| `updateStatus(status)` | Asigna el estado (sin validar transición) |
| `updateCardData(pan, cvv, pin, exp)` | Carga datos de emisión; setea `issuedAt` |
| `block(reason)` | → `BLOCKED`, guarda motivo y `blockedAt` |
| `blockForFraud(reason)` | → `BLOCKED_FRAUD`, guarda motivo y `blockedAt` |
| `unblock()` | Exige estado bloqueado; → `ACTIVE`, limpia motivo y fecha |
| `cancel(reason)` / `expire()` / `delete()` | → `CANCELLED` / `EXPIRED` / soft delete |
| `updatePIN(hash)` / `resetPIN(hash)` | Reemplaza `pinHash` y limpia el PIN en claro |

**Consultas derivadas**

- `isOperational()` → estado `ACTIVE` o `ISSUED` **y** no vencida.
- `isBlocked()` → `BLOCKED` o `BLOCKED_FRAUD`.
- `requiresRenewal()` → el último día del mes de vencimiento cae dentro de los próximos **60 días**.
- `canBeActivated()` → estado `ISSUED`. `canBeIssued()` → estado `APPROVED`.
- `getMaskedCardNumber()` → `**** **** **** NNNN`.
- `isPINSet()` / `hasCompleteCardData()`.

### 3.2 `CardHolder`

Datos personales del titular: `firstName`, `lastName`, `documentNumber`, `email`, `phoneNumber`, `birthDate`, `address` (opcional).

Reglas aplicadas en `CardHolder.create(...)`:

- Todos los campos obligatorios salvo `address`.
- Email con formato válido; se normaliza a minúsculas. Strings con `trim()`.
- Fecha de nacimiento no futura y **titular mayor de 18 años**.
- Igualdad por `documentNumber`.

### 3.3 `CardType`

| Valor | Nombre | Patrón BIN |
|---|---|---|
| `VISA` | Visa | `^4[0-9]{12}(?:[0-9]{3})?$` |
| `MASTERCARD` | Mastercard | `^5[1-5][0-9]{14}$` |
| `AMEX` | American Express | `^3[47][0-9]{13}$` |
| `CABAL` | Cabal | `^(6042\|6043\|5896)[0-9]{12}$` |
| `NARANJA` | Naranja | `^(5895\|5218\|5234)[0-9]{12}$` |

`CardType.detectFromNumber(pan)` devuelve `VISA` si ningún patrón coincide.

### 3.4 `CardCategory`

| Valor | Nombre | Límite de crédito por defecto (ARS) |
|---|---|---|
| `CLASSIC` | Clásica | 50.000 |
| `GOLD` | Gold | 150.000 |
| `PLATINUM` | Platinum | 300.000 |
| `BLACK` | Black | 1.000.000 |
| `INFINITE` | Infinite | 5.000.000 |

### 3.5 `CardStatus` (de `card-shared`)

`REQUESTED` → `APPROVED` / `REJECTED` → `ISSUED` → `ACTIVE` ⇄ `BLOCKED` / `BLOCKED_FRAUD` → `CANCELLED` / `EXPIRED`

```mermaid
stateDiagram-v2
    [*] --> REQUESTED: POST /cards
    REQUESTED --> APPROVED: card-lifecycle
    REQUESTED --> REJECTED: card-lifecycle
    APPROVED --> ISSUED: card-lifecycle (PUT /card-data + /status)
    ISSUED --> ACTIVE: card-lifecycle (PUT /status + /activated-at)
    ACTIVE --> BLOCKED: POST /block
    ACTIVE --> BLOCKED_FRAUD: POST /block-fraud
    BLOCKED --> ACTIVE: POST /unblock
    BLOCKED_FRAUD --> ACTIVE: POST /unblock
    ACTIVE --> CANCELLED
    ACTIVE --> EXPIRED
    REJECTED --> [*]
    CANCELLED --> [*]
    EXPIRED --> [*]
```

El detalle de las transiciones y quién las orquesta está en [LIFECYCLE.md](LIFECYCLE.md). En `card-data` las transiciones **no se validan** (ver §10).

---

## 4. API REST

Todos los endpoints cuelgan de `/api/v1/cards`. Formato JSON.

### 4.1 Gestión de tarjetas (`CardController`)

#### `POST /api/v1/cards` — Crear tarjeta

Crea una solicitud en estado `REQUESTED` y dispara el evento `card.created`.

```json
{
  "clientId": "12345678",
  "firstName": "Juan",
  "lastName": "Pérez",
  "documentNumber": "12345678",
  "email": "juan.perez@example.com",
  "phoneNumber": "+5491123456789",
  "birthDate": "1990-01-15",
  "address": "Av. Corrientes 1234, CABA",
  "type": "VISA",
  "category": "CLASSIC"
}
```

| Campo | Validación |
|---|---|
| `clientId` | Obligatorio, 7–11 caracteres |
| `firstName`, `lastName` | Obligatorios, máx. 100 |
| `documentNumber` | Obligatorio |
| `email` | Obligatorio, formato email |
| `phoneNumber` | Obligatorio, `^\+?[0-9]{10,15}$` |
| `birthDate` | Obligatoria, en el pasado (+ mayor de 18 en dominio) |
| `address` | Opcional, máx. 500 |
| `type`, `category` | Obligatorios, valores de los enums |

Respuesta **201** → `CardResponse`.

#### `GET /api/v1/cards/{cardId}` — Detalle

Respuesta **200** → `CardDetailResponse`. **404** si no existe o está eliminada.

#### `GET /api/v1/cards/client/{clientId}` — Tarjetas de un cliente

Respuesta **200** → `CardResponse[]` (lista vacía si no tiene tarjetas; nunca 404).

#### `GET /api/v1/cards/search?lastFourDigits=1234&clientId=12345678`

Busca por últimos 4 dígitos **acotado al cliente**. Respuesta **200** → `CardDetailResponse`, **404** si no hay coincidencia.

#### Endpoints internos (consumidos por `card-lifecycle`)

No tienen DTO ni Bean Validation; reciben un `Map` y responden **200** sin cuerpo.

| Método y ruta | Body | Efecto |
|---|---|---|
| `PUT /{cardId}/status` | `{ "status": "APPROVED" }` | Cambia el estado |
| `PUT /{cardId}/activated-at` | `{ "activatedAt": "2026-11-02T10:15:30" }` | Fecha de activación (ISO-8601) |
| `PUT /{cardId}/card-data` | `{ "cardNumber": "...", "cvv": "...", "pin": "...", "expirationMonth": 12, "expirationYear": 2030 }` | Carga datos de emisión; el PAN y el CVV se cifran y el PIN se hashea al persistir |

### 4.2 Seguridad (`CardSecurityController`)

| Método y ruta | Body | Resultado |
|---|---|---|
| `POST /{cardId}/block` | `{ "reason": "Tarjeta perdida" }` (obligatorio) | → `BLOCKED`, **200** `CardResponse` |
| `POST /{cardId}/block-fraud` | `{ "reason": "..." }` (obligatorio) | → `BLOCKED_FRAUD`, **200** `CardResponse` |
| `POST /{cardId}/unblock` | — | → `ACTIVE`, **200** `CardResponse`. Si no estaba bloqueada → `IllegalStateException` (**500**, ver §10) |

### 4.3 PIN (`PINController`)

#### `POST /{cardId}/validate-pin`

```json
{ "pin": "1234" }
```

Respuesta **200** `{ "valid": true | false }`. Devuelve `false` también si la tarjeta no tiene PIN configurado. PIN de exactamente 4 dígitos.

#### `POST /{cardId}/reset-pin`

```json
{ "oldPin": "1234", "newPin": "5678" }
```

- Si la tarjeta tiene PIN, `oldPin` debe coincidir; si no → **422** `"El PIN actual es incorrecto"`.
- `newPin` se valida con `PIN.of(...)`, se hashea con BCrypt y se guarda.
- Respuesta **200** `CardResponse`.

### 4.4 DTOs de respuesta

**`CardResponse`** (vista resumida): `id`, `clientId`, `maskedCardNumber`, `lastFourDigits`, `type`, `category`, `status`, `expirationDate` (MM/YY), `holderName`, `createdAt`, `activatedAt`, `isBlocked`, `blockReason`.

**`CardDetailResponse`** (vista detallada): todo lo anterior más `isExpired`, `requiresRenewal`, `holderEmail`, `holderPhone`, `holderDocument`, `holderBirthDate`, `holderAddress`, `updatedAt`, `blockedAt`, `isOperational`, `hasPinSet`.

Ninguna respuesta expone el PAN completo, el CVV ni el hash del PIN.

### 4.5 Errores (`GlobalExceptionHandler`)

```json
{
  "timestamp": "2026-11-02T10:15:30",
  "status": 400,
  "error": "Validation Error",
  "message": "Invalid request parameters",
  "fieldErrors": { "email": "El formato del email es inválido" }
}
```

| Excepción | HTTP | `error` |
|---|---|---|
| `NotFoundException` | 404 | `Not Found` |
| `ValidationException` (dominio / Value Objects) | 400 | `Validation Error` |
| `MethodArgumentNotValidException` (Bean Validation) | 400 | `Validation Error` + `fieldErrors` |
| `BusinessRuleException` | 422 | `Business Rule Violation` |
| Cualquier otra | 500 | `Internal Server Error` |

---

## 5. Flujos funcionales

### 5.1 Alta de tarjeta

```mermaid
sequenceDiagram
    participant C as Cliente
    participant API as CardController
    participant S as CardManagementService
    participant R as CardRepositoryAdapter
    participant DB as PostgreSQL
    participant L as CardEventListener
    participant K as Kafka

    C->>API: POST /api/v1/cards
    API->>API: Bean Validation + CardDtoMapper.toCommand
    API->>S: execute(CreateCardCommand)
    S->>S: Card.create(...) → REQUESTED
    S->>R: save(card)
    R->>DB: INSERT cards
    S->>S: publishEvent(CardCreatedDomainEvent)
    S-->>API: Card
    API-->>C: 201 CardResponse
    Note over S,L: COMMIT de la transacción
    L->>K: card.created (key = cardId)
```

El evento a Kafka se emite con `@TransactionalEventListener(phase = AFTER_COMMIT)`: **solo se publica si la tarjeta quedó persistida**. Si Kafka falla, el error se loguea y la tarjeta queda creada igual (no hay reintento ni outbox).

### 5.2 Emisión y activación (orquestadas por `card-lifecycle`)

1. `card-lifecycle` recibe `card.created` y, al aprobar, llama `PUT /{id}/status` con `APPROVED`.
2. En la emisión genera PAN, CVV, PIN y vencimiento → `PUT /{id}/card-data` y luego `PUT /{id}/status` con `ISSUED`.
3. En la activación → `PUT /{id}/status` con `ACTIVE` y `PUT /{id}/activated-at`.

### 5.3 Bloqueo / desbloqueo

`CardSecurityService` carga la tarjeta (404 si no existe), aplica `block` / `blockForFraud` / `unblock` en el aggregate y persiste. El bloqueo por fraude se loguea en nivel `WARN`. El desbloqueo siempre devuelve la tarjeta a `ACTIVE`.

### 5.4 PIN

- **Validación**: `BCrypt.verify(pinPlano, pinHash)`. Los intentos fallidos se loguean en `WARN`; no hay contador de intentos ni bloqueo automático.
- **Cambio**: verifica el PIN actual (si existe), hashea el nuevo y reemplaza `pinHash`.

---

## 6. Persistencia

### 6.1 Tabla `cards`

| Columna | Tipo | Nulo | Notas |
|---|---|---|---|
| `id` | `varchar(36)` | no | PK, UUID |
| `client_id` | `varchar(20)` | no | índice `idx_client_id` |
| `holder_first_name` / `holder_last_name` | `varchar(100)` | no | |
| `holder_document_number` | `varchar(20)` | no | |
| `holder_email` | `varchar(100)` | no | |
| `holder_phone_number` | `varchar(20)` | sí | |
| `holder_birth_date` | `date` | sí | |
| `holder_address` | `varchar(500)` | sí | |
| `card_number_encrypted` | `text` | sí | PAN cifrado AES; índice `idx_card_number_encrypted` |
| `last_four_digits` | `varchar(4)` | sí | En claro, para búsqueda y display; índice `idx_last_four_digits` |
| `cvv_encrypted` | `text` | sí | CVV cifrado AES |
| `pin_hash` | `text` | sí | Hash BCrypt |
| `type` / `category` / `status` | `varchar(20)` | no | Enums como `STRING`; índice `idx_status` |
| `expiration_month` / `expiration_year` | `int` | sí | |
| `created_at` / `updated_at` | `timestamp` | no | `@CreationTimestamp` / `@UpdateTimestamp` |
| `activated_at`, `blocked_at`, `issued_at`, `cancelled_at`, `expired_at` | `timestamp` | sí | |
| `block_reason` / `cancelled_reason` | `varchar(500)` | sí | |
| `deleted` | `boolean` | no | default `false` |
| `deleted_at` | `timestamp` | sí | |

Los datos del titular se guardan en la misma tabla (no hay tabla de titulares).

> **Esquema**: lo crea Flyway desde el repo `card-management-database` (`migrations/V1__create_cards_tables.sql`, ajustada por `V8__align_cards_with_card_entity.sql`). Este módulo usa `ddl-auto: validate` con Flyway deshabilitado, así que la tabla debe existir antes de levantar el servicio. La tabla de arriba describe `CardEntity`; la migración agrega restricciones propias (§6.1.1).

#### 6.1.1 Diferencias entre el esquema y `CardEntity`

La migración **V8** corrige las tres primeras filas (las marcadas *Resuelto*); su rollback está en `rollback/R2__rollback_v8.sql`.

| Columna | Migración V1 | `CardEntity` / API | Efecto |
|---|---|---|---|
| `category` | V1: `CHECK` sin `INFINITE` | El enum incluye `INFINITE` | *Resuelto en V8:* el `CHECK` incluye `INFINITE` |
| `holder_address` | V1: `varchar(200)` | `length 500`; el request acepta hasta 500 | *Resuelto en V8:* `varchar(500)` |
| `block_reason` | V1: `varchar(200)` | `length 500` | *Resuelto en V8:* `varchar(500)` |
| `client_id` | `varchar(11)` | `length 20` | Sin impacto (el dominio limita a 7–11) |
| `holder_birth_date` | `NOT NULL` | Nullable | Sin impacto (el dominio la exige) |
| `pin_hash` | `varchar(100)` | `TEXT` | Sin impacto (un hash BCrypt tiene 60 caracteres) |
| `status`, `type` | `CHECK` con los valores de los enums | — | Coinciden |

La migración también define índices parciales sobre `deleted = false` y la tabla `card_audit` (V3), que ningún módulo usa.

### 6.2 Soft delete

Todas las consultas de `JpaCardRepository` (incluido el `findById` sobrescrito) filtran `deleted = false`. `CardRepository.delete(...)` marca `deleted = true` y `deleted_at`; no hay borrado físico.

### 6.3 Consultas disponibles

| Método | Uso |
|---|---|
| `findById` | Detalle (excluye eliminadas) |
| `findByClientId` | Listado por cliente |
| `findByLastFourDigitsAndClientId` | Búsqueda acotada a cliente |
| `findByCardNumberEncrypted` | Búsqueda por PAN cifrado |
| `findByStatus` | Por estado (sin endpoint) |
| `findCardsRequiringRenewal` | `ACTIVE` que vencen este mes o el siguiente (sin endpoint) |

---

## 7. Seguridad de datos sensibles

| Dato | Tratamiento | Dónde |
|---|---|---|
| PAN | Cifrado AES (`AesEncryptionService` de `card-shared`) al guardar; descifrado al leer | `CardRepositoryAdapter` vía `EncryptionPort` |
| Últimos 4 dígitos | En claro | `CardEntityMapper` |
| CVV | Cifrado AES | `CardRepositoryAdapter` |
| PIN | Hash BCrypt (`BCryptHashingService`); el PIN en claro solo vive en memoria y está excluido de `toString()` | `CardEntityMapper` / `PINManagementService` |

- La clave AES se lee de `encryption.secret-key` (por defecto un valor de desarrollo de 32 caracteres). **Debe sobrescribirse en producción.**
- Las respuestas REST solo devuelven el número enmascarado.
- Los endpoints no tienen autenticación ni autorización (no hay Spring Security en el módulo).

---

## 8. Eventos

| Evento | Topic | Key | Cuándo | Consumidor |
|---|---|---|---|---|
| `CardCreatedEvent` | `card.created` | `cardId` | Tras el commit del alta | `card-lifecycle` (`CardEventConsumer`) |

Payload (JSON, sin headers de tipo):

```json
{
  "eventId": "…uuid…",
  "occurredOn": "2026-11-02T10:15:30",
  "eventType": "CardCreatedEvent",
  "cardId": "550e8400-e29b-41d4-a716-446655440000",
  "clientId": "12345678",
  "holderFullName": "Juan Pérez",
  "cardType": "VISA",
  "cardCategory": "CLASSIC",
  "createdBy": "system"
}
```

Productor (`KafkaProducerConfig`): `acks=all`, `retries=3`, idempotencia habilitada, `max.in.flight=5`, compresión `snappy`, `linger.ms=10`, `batch.size=16384`. `KafkaEventPublisher` ofrece `publish` (asíncrono, solo loguea el resultado) y `publishSync` (espera confirmación y lanza `EventPublishingException`).

Las clases `CardBlockedEvent`, `CardUnblockedEvent` y `PINResetEvent` existen vacías: hoy **no se publican eventos** de bloqueo, desbloqueo ni cambio de PIN, aunque `card-events` ya define los topics `card.blocked` y `card.unblocked`. Ver [EVENTS.md](EVENTS.md).

---

## 9. Configuración y despliegue

### 9.1 Propiedades principales (`application.yml`)

| Propiedad | Valor por defecto |
|---|---|
| `server.port` | `8081` |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/card_management_db` |
| `spring.datasource.username` / `password` | `card_user` / `card_pass` |
| `spring.datasource.hikari.*` | pool 10, min idle 5, pool `CardManagementHikariCP` |
| `spring.jpa.hibernate.ddl-auto` | `validate` |
| `spring.flyway.enabled` | `false` |
| `spring.kafka.bootstrap-servers` | `localhost:9092` |
| `encryption.secret-key` | clave de desarrollo (32 caracteres) |
| `management.endpoints.web.exposure.include` | `health,info,metrics` |

El perfil `card-data` (`application-card-data.yml`) agrega logging a archivo (`logs/card-data.log`), métricas Prometheus y la configuración de SpringDoc. Es el perfil que activa `docker-compose`.

### 9.2 Docker

- `services/card-data/Dockerfile`: build multi-stage (Maven 3.9.6 + Temurin 17 → `eclipse-temurin:17-jre-alpine`), usuario no root, `JAVA_OPTS` con G1GC y `MaxRAMPercentage=75`. El contexto de build es la raíz del repo.
- `deploy/docker/docker-compose.yml`: servicio `card-data-service`, puerto `8081`, perfil `card-data`, conectado a `postgres` y Kafka en la red `card-network`.

```bash
docker compose -f deploy/docker/docker-compose.yml up -d card-data-service
```

### 9.3 Observabilidad

- Health: `GET /actuator/health` (incluye estado de la base).
- Métricas: `GET /actuator/metrics` (y `/actuator/prometheus` con el perfil `card-data`).
- Logs: `com.unisono.card` en `DEBUG`; SQL de Hibernate en `DEBUG`.

---

## 10. Limitaciones conocidas y deuda técnica

Comportamientos actuales del código que conviene tener presentes:

1. **Transiciones de estado sin validar.** `PUT /status` acepta cualquier `CardStatus`, y `block`/`blockForFraud` no consultan `CardStatus.canBlock()`: se puede bloquear una tarjeta `REQUESTED` o `CANCELLED`. Un valor de estado inexistente produce `IllegalArgumentException` → **500**.
2. **`unblock` sobre una tarjeta no bloqueada** lanza `IllegalStateException`, que el handler mapea a **500** en lugar de 422.
3. **Endpoints internos con `Map`** (`/status`, `/activated-at`, `/card-data`): sin Bean Validation, sin documentación OpenAPI de payload, y con casteos directos (`(Integer)`), por lo que un tipo inesperado termina en 500.
4. **Longitud de PIN inconsistente.** `ValidatePINRequest` y `ResetPINRequest` exigen 4 dígitos; `ActivateCardRequest` (sin uso) admite 4–6.
5. **Sin control de intentos de PIN** ni bloqueo automático tras fallos.
6. **Publicación a Kafka sin garantía.** Si falla tras el commit, el evento se pierde (no hay patrón outbox); `card-lifecycle` no se entera del alta.
7. **Código duplicado o sin uso:** `CreateCardService` (camino alternativo de alta que publica directo a Kafka, no lo usa ningún controller), `CardHolderEmbeddable` (con longitudes distintas a `CardEntity`), `CardHolderEntity`, `CardJpaConfig`, `application/config/CardUseCaseConfig`, `UpdateCardStatusRequest`, `UpdateCardStatusUseCase` y las excepciones de `domain/exception` (clases vacías). `CardEntityMapper` tiene además las sobrecargas `toEntity(Card)` / `toDomain(CardEntity)` que cifran por su cuenta y no se usan.
8. **Clave de cifrado en docker-compose ignorada.** El compose define `ENCRYPTION_KEY`, pero la propiedad que lee `AesEncryptionService` es `encryption.secret-key` (variable de entorno equivalente: `ENCRYPTION_SECRET_KEY`), así que el contenedor usa la clave por defecto.
9. **URL de card-data en payment-processing.** Su default es `http://localhost:8080`, mientras que card-data escucha en `8081`.
10. **El esquema vive en otro repo.** Las migraciones están en `card-management-database` y no se versionan con el código: cualquier cambio en `CardEntity` requiere una migración nueva allá. Las diferencias que provocaban errores (`INFINITE`, longitudes de `holder_address` y `block_reason`) se corrigen con la V8 (§6.1.1).
11. **Sin autenticación/autorización** en la API, incluidos los endpoints que modifican datos sensibles.
12. **Tests**: solo `CardTest` y `CardManagementServiceTest`; no hay tests de controllers, de persistencia ni de integración, aunque Testcontainers ya está declarado en el `pom.xml`.
13. `services/card-data/README.md` contiene la documentación de `card-shared`, no la de este módulo.
