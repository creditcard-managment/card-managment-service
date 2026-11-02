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

