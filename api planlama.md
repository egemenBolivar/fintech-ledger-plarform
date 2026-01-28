# FinTech Ledger Platform — API & Implementation Planlama

> **Proje Durumu:** Backend + Frontend tamamlandı, Event Sourcing & Deployment hazırlanıyor
> **Son Güncelleme:** 28 Ocak 2026

---

## 🏁 TAMAMLANAN İŞLER

### ✅ Backend (Tamamen Tamamlandı)
- [x] Money Value Object, Currency Enum, BaseEntity
- [x] Wallet Entity + WalletStatus Enum
- [x] Transaction Entity (Immutable) + Direction/GroupType/ReferenceType Enums
- [x] WalletRepository, TransactionRepository
- [x] BalanceCalculator (real-time SUM)
- [x] WalletService, LedgerService, LedgerUseCaseService
- [x] FxService + FxRateProvider (mock rates)
- [x] WalletController, TransactionController, UseCaseController
- [x] Tüm DTO'lar (Request/Response)
- [x] GlobalExceptionHandler (RFC 7807 ProblemDetail)
- [x] @ResponseStatus tüm exception'lara eklendi
- [x] Idempotency mekanizması (IdempotencyKey entity + service)
- [x] Optimistic Locking (@Version on Wallet)
- [x] Swagger/OpenAPI annotations
- [x] Controller testleri (@WebMvcTest)
- [x] Domain unit testleri (Money, Wallet, Transaction)
- [x] FX Rate Preview endpoint (GET /api/v1/fx/rate)
- [x] JWT Authentication + Spring Security
- [x] User-Wallet ilişkisi (her kullanıcı sadece kendi wallet'larını görür)
- [x] Access Control (AccessDeniedException - 403)
- [x] Balance Snapshot mekanizması (performans için)

### ✅ Frontend (Tamamen Tamamlandı)
- [x] Angular 20 proje yapısı
- [x] WalletListComponent - wallet listesi, create, suspend/activate
- [x] WalletDetailComponent - detay, deposit, withdraw, transfer, FX convert
- [x] Transaction history with pagination
- [x] WalletApiService - tüm API çağrıları
- [x] HTTP interceptor (base URL, auth, error handling)
- [x] ToastService + ToastComponent (custom, no dependency)
- [x] FX Preview (debounce ile 500ms gecikme)
- [x] Login/Register sayfaları
- [x] Auth Guard + Token management
- [x] Loading states (skeleton loaders)
- [x] Owner ID alanı kaldırıldı (authenticated user'dan alınıyor)

---

## 🚧 DEVAM EDEN / SONRAKI ADIMLAR

### Event Sourcing & Audit Logging (Tasarım Aşamasında)
- [ ] Domain Event'ler tanımlanacak
- [ ] Event Store tasarımı
- [ ] Audit Log entity ve service
- [ ] Event Publisher mekanizması

### Deployment Hazırlığı
- [ ] Dockerfile (backend)
- [ ] Dockerfile (frontend)
- [ ] docker-compose.yml (production)
- [ ] CI/CD pipeline (GitHub Actions)
- [ ] Environment configuration
- [ ] DigitalOcean deployment

---

## 📁 Proje Yapısı (Mevcut)

```
fintech-ledger-platform/
├── backend/
│   └── fintech-ledger/          # Spring Boot 4.0.2, Java 21
│       ├── src/main/java/com/ekup/fintech/
│       └── pom.xml
├── frontend/
│   └── fintech-ui/              # Angular 19
└── docs/
    ├── api planlama.md          # Bu dosya
    └── Spring Boot + JPA + Angular ile vitrinlik, gerçekç
```

---

## 🎯 Implementation Fazları

### ✅ Faz 0: Proje İskeleti (TAMAMLANDI)
- [x] Spring Boot projesi oluşturuldu
- [x] Angular projesi oluşturuldu
- [x] Temel bağımlılıklar eklendi

---

## 📦 FAZ 1: Shared Kernel & Core Domain

### 1.1 Package Yapısı

```
com.ekup.fintech/
├── shared/
│   ├── domain/
│   │   ├── Money.java                 # Value Object
│   │   ├── Currency.java              # Enum (ISO-4217)
│   │   └── BaseEntity.java            # Audit fields
│   ├── exception/
│   │   ├── DomainException.java
│   │   ├── InsufficientBalanceException.java
│   │   ├── WalletSuspendedException.java
│   │   └── CurrencyMismatchException.java
│   └── util/
│       └── IdGenerator.java           # UUID/ULID generator
│
├── ledger/
│   ├── domain/
│   │   ├── Wallet.java                # Aggregate Root
│   │   ├── WalletStatus.java          # Enum
│   │   ├── Transaction.java           # Immutable Entity
│   │   ├── TransactionDirection.java  # CREDIT/DEBIT
│   │   ├── TransactionGroupType.java  # Business meaning
│   │   └── ReferenceType.java         # Source/reason
│   ├── application/
│   │   ├── WalletService.java
│   │   ├── LedgerService.java
│   │   ├── BalanceCalculator.java
│   │   └── command/
│   │       ├── CreateWalletCommand.java
│   │       ├── RecordTransactionCommand.java
│   │       └── TransferCommand.java
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── WalletRepository.java
│   │   │   ├── TransactionRepository.java
│   │   │   └── JpaWalletRepository.java
│   │   └── config/
│   │       └── LedgerConfig.java
│   └── api/
│       ├── WalletController.java
│       ├── TransactionController.java
│       └── dto/
│           ├── WalletResponse.java
│           ├── BalanceResponse.java
│           ├── TransactionResponse.java
│           └── CreateWalletRequest.java
│
├── payment/                           # Placeholder
├── kyc/                               # Placeholder
└── risk/                              # Placeholder
```

---

### 1.2 Domain Models

#### Money (Value Object)
```java
// Immutable, thread-safe
public record Money(BigDecimal amount, Currency currency) {
    public Money add(Money other);
    public Money subtract(Money other);
    public Money negate();
    public boolean isPositive();
    public boolean isNegative();
    public boolean isZero();
}
```

#### Wallet (Aggregate Root)
```java
@Entity
public class Wallet {
    private UUID id;
    private UUID ownerId;
    private Currency baseCurrency;
    private WalletStatus status;  // ACTIVE, SUSPENDED, CLOSED
    private Instant createdAt;
    
    // NO balance field!
    // Balance = SUM(transactions)
}
```

#### Transaction (Immutable Entity)
```java
@Entity
@Immutable  // Hibernate annotation
public class Transaction {
    private UUID id;
    private UUID walletId;
    private BigDecimal amount;
    private Currency currency;
    private TransactionDirection direction;  // CREDIT, DEBIT
    private TransactionGroupType groupType;  // USER_ACTION, SYSTEM_ADJUSTMENT, etc.
    private ReferenceType referenceType;     // DEPOSIT, WITHDRAWAL, TRANSFER, etc.
    private UUID referenceId;                // Related entity ID
    private String description;
    private Instant occurredAt;
    
    // NO setters, NO update methods
}
```

---

### 1.3 Enums

```java
public enum TransactionDirection {
    CREDIT,  // Increases wallet value
    DEBIT    // Decreases wallet value
}

public enum TransactionGroupType {
    USER_ACTION,        // User-initiated
    SYSTEM_ADJUSTMENT,  // System corrections
    FX_CONVERSION,      // Currency exchange
    PAYMENT,            // Payment processing
    REVERSAL,           // Undo/refund
    FEE                 // Service fees
}

public enum ReferenceType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    FX_EXCHANGE,
    CARD_PAYMENT
}

public enum WalletStatus {
    ACTIVE,
    SUSPENDED,
    CLOSED
}

public enum Currency {
    TRY, USD, EUR, GBP  // ISO-4217
}
```

---

## 🔌 FAZ 2: REST API Endpoints

### 2.1 Wallet API

| Method | Endpoint | Açıklama | Request | Response |
|--------|----------|----------|---------|----------|
| `POST` | `/api/v1/wallets` | Wallet oluştur | `CreateWalletRequest` | `WalletResponse` |
| `GET` | `/api/v1/wallets/{id}` | Wallet detay | - | `WalletResponse` |
| `GET` | `/api/v1/wallets/{id}/balance` | Bakiye hesapla | - | `BalanceResponse` |
| `PATCH` | `/api/v1/wallets/{id}/suspend` | Wallet dondur | - | `WalletResponse` |
| `PATCH` | `/api/v1/wallets/{id}/activate` | Wallet aktifle | - | `WalletResponse` |
| `GET` | `/api/v1/owners/{ownerId}/wallets` | Owner walletları | - | `List<WalletResponse>` |

#### Request/Response DTOs

```java
// Request
public record CreateWalletRequest(
    @NotNull UUID ownerId,
    @NotNull Currency baseCurrency
) {}

// Response
public record WalletResponse(
    UUID id,
    UUID ownerId,
    Currency baseCurrency,
    WalletStatus status,
    Instant createdAt
) {}

public record BalanceResponse(
    UUID walletId,
    Money balance,
    Instant calculatedAt
) {}
```

---

### 2.2 Transaction API

| Method | Endpoint | Açıklama | Request | Response |
|--------|----------|----------|---------|----------|
| `GET` | `/api/v1/wallets/{walletId}/transactions` | İşlem listesi | Query params | `Page<TransactionResponse>` |
| `GET` | `/api/v1/transactions/{id}` | İşlem detay | - | `TransactionResponse` |

> ⚠️ **NOT:** Transaction'lar doğrudan `POST` ile oluşturulmaz!
> Use-case endpoint'leri üzerinden oluşturulur (deposit, withdraw, transfer).

#### Query Parameters (Filtreleme)

```
GET /api/v1/wallets/{walletId}/transactions
    ?page=0
    &size=20
    &sort=occurredAt,desc
    &direction=CREDIT
    &groupType=USER_ACTION
    &referenceType=DEPOSIT
    &from=2026-01-01T00:00:00Z
    &to=2026-01-31T23:59:59Z
```

#### Response DTO

```java
public record TransactionResponse(
    UUID id,
    UUID walletId,
    Money amount,
    TransactionDirection direction,
    TransactionGroupType groupType,
    ReferenceType referenceType,
    UUID referenceId,
    String description,
    Instant occurredAt
) {}
```

---

### 2.3 Use-Case API (Domain Operations)

Bu endpoint'ler gerçek fintech davranışını modelliyor. User doğrudan bakiye değiştirmiyor, **request** oluşturuyor.

| Method | Endpoint | Açıklama | Request | Response |
|--------|----------|----------|---------|----------|
| `POST` | `/api/v1/deposits` | Para yatırma talebi | `DepositRequest` | `DepositResponse` |
| `POST` | `/api/v1/withdrawals` | Para çekme talebi | `WithdrawalRequest` | `WithdrawalResponse` |
| `POST` | `/api/v1/transfers` | Transfer | `TransferRequest` | `TransferResponse` |
| `POST` | `/api/v1/fx/convert` | Döviz çevirme | `FxConvertRequest` | `FxConvertResponse` |

#### Deposit Flow (Örnek)

```
1. User POST /api/v1/deposits
2. System creates DepositRequest (PENDING)
3. System processes via mock payment provider
4. System creates Transaction (CREDIT, USER_ACTION, DEPOSIT)
5. Return DepositResponse with transaction details
```

#### Request/Response DTOs

```java
// Deposit
public record DepositRequest(
    @NotNull UUID walletId,
    @NotNull @Positive BigDecimal amount,
    @NotNull Currency currency,
    UUID idempotencyKey  // Duplicate prevention
) {}

public record DepositResponse(
    UUID depositId,
    UUID transactionId,
    UUID walletId,
    Money amount,
    String status,  // COMPLETED, PENDING, FAILED
    Instant processedAt
) {}

// Withdrawal
public record WithdrawalRequest(
    @NotNull UUID walletId,
    @NotNull @Positive BigDecimal amount,
    @NotNull Currency currency,
    UUID idempotencyKey
) {}

// Transfer
public record TransferRequest(
    @NotNull UUID sourceWalletId,
    @NotNull UUID targetWalletId,
    @NotNull @Positive BigDecimal amount,
    @NotNull Currency currency,
    String description,
    UUID idempotencyKey
) {}

public record TransferResponse(
    UUID transferId,
    UUID sourceTransactionId,
    UUID targetTransactionId,
    UUID sourceWalletId,
    UUID targetWalletId,
    Money amount,
    String status,
    Instant processedAt
) {}

// FX Conversion
public record FxConvertRequest(
    @NotNull UUID walletId,
    @NotNull @Positive BigDecimal amount,
    @NotNull Currency sourceCurrency,
    @NotNull Currency targetCurrency,
    UUID idempotencyKey
) {}

public record FxConvertResponse(
    UUID conversionId,
    UUID debitTransactionId,   // Source currency DEBIT
    UUID creditTransactionId,  // Target currency CREDIT
    Money sourceAmount,
    Money targetAmount,
    BigDecimal exchangeRate,
    Instant processedAt
) {}
```

---

## 🗄️ FAZ 3: Database Schema

### PostgreSQL Tables

```sql
-- Wallet table
CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_wallet_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT chk_currency CHECK (base_currency IN ('TRY', 'USD', 'EUR', 'GBP'))
);

CREATE INDEX idx_wallets_owner ON wallets(owner_id);

-- Transaction table (APPEND-ONLY, NO UPDATE, NO DELETE)
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    group_type VARCHAR(30) NOT NULL,
    reference_type VARCHAR(30) NOT NULL,
    reference_id UUID,
    description TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_direction CHECK (direction IN ('CREDIT', 'DEBIT')),
    CONSTRAINT chk_group_type CHECK (group_type IN (
        'USER_ACTION', 'SYSTEM_ADJUSTMENT', 'FX_CONVERSION', 
        'PAYMENT', 'REVERSAL', 'FEE'
    )),
    CONSTRAINT chk_reference_type CHECK (reference_type IN (
        'DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'FX_EXCHANGE', 'CARD_PAYMENT'
    ))
);

CREATE INDEX idx_transactions_wallet ON transactions(wallet_id);
CREATE INDEX idx_transactions_occurred ON transactions(occurred_at DESC);
CREATE INDEX idx_transactions_reference ON transactions(reference_type, reference_id);

-- Idempotency table (duplicate request prevention)
CREATE TABLE idempotency_keys (
    key UUID PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    response_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);

-- Balance calculation view (for reporting, not authoritative)
CREATE VIEW wallet_balances AS
SELECT 
    wallet_id,
    currency,
    SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE -amount END) as balance,
    COUNT(*) as transaction_count,
    MAX(occurred_at) as last_transaction_at
FROM transactions
GROUP BY wallet_id, currency;
```

---

## 🔄 FAZ 4: Business Rules & Validations

### Wallet Rules

| Rule | Açıklama | Exception |
|------|----------|-----------|
| W001 | Suspended wallet'tan para çekilemez | `WalletSuspendedException` |
| W002 | Closed wallet'a işlem yapılamaz | `WalletClosedException` |
| W003 | Owner başına currency başına tek wallet | `DuplicateWalletException` |

### Transaction Rules

| Rule | Açıklama | Exception |
|------|----------|-----------|
| T001 | Bakiye negatife düşemez | `InsufficientBalanceException` |
| T002 | Currency mismatch kontrolü | `CurrencyMismatchException` |
| T003 | Amount sıfır veya negatif olamaz | `InvalidAmountException` |
| T004 | Transaction update/delete yasak | N/A (Hibernate @Immutable) |

### Transfer Rules

| Rule | Açıklama | Exception |
|------|----------|-----------|
| TR001 | Aynı wallet'a transfer yapılamaz | `SameWalletTransferException` |
| TR002 | Farklı currency'ler için FX gerekli | `CurrencyMismatchException` |

---

## 📊 FAZ 5: Balance Calculation Strategy

### Başlangıç: Real-time Calculation

```java
@Service
public class BalanceCalculator {
    
    public Money calculateBalance(UUID walletId, Currency currency) {
        // SELECT SUM(CASE WHEN direction='CREDIT' THEN amount ELSE -amount END)
        // FROM transactions
        // WHERE wallet_id = ? AND currency = ?
    }
}
```

### Gelecek: Snapshot + Incremental

```java
// 1. Nightly job creates balance snapshots
// 2. Real-time: snapshot + delta transactions
public Money calculateBalanceOptimized(UUID walletId, Currency currency) {
    BalanceSnapshot snapshot = snapshotRepository.findLatest(walletId, currency);
    List<Transaction> delta = transactionRepository.findAfter(
        walletId, 
        currency, 
        snapshot.getSnapshotAt()
    );
    return snapshot.getBalance().add(sumTransactions(delta));
}
```

---

## 🧪 FAZ 6: Test Strategy

### Unit Tests
- `MoneyTest` — Arithmetic operations, immutability
- `WalletTest` — Status transitions, validation
- `TransactionTest` — Immutability constraints
- `BalanceCalculatorTest` — Calculation accuracy

### Integration Tests
- `WalletRepositoryTest` — JPA operations
- `TransactionRepositoryTest` — Append-only behavior
- `LedgerServiceTest` — Full flow tests

### API Tests
- `WalletControllerTest` — @WebMvcTest
- `DepositFlowTest` — End-to-end deposit
- `TransferFlowTest` — End-to-end transfer

---

## 🚀 Implementation Checklist

### ✅ Sprint 1: Core Domain (TAMAMLANDI)
- [x] `Money` Value Object
- [x] `Currency` Enum
- [x] `BaseEntity` with audit fields
- [x] Domain exceptions
- [x] Unit tests

### ✅ Sprint 2: Ledger Domain (TAMAMLANDI)
- [x] `Wallet` entity
- [x] `Transaction` entity
- [x] All enums (Direction, GroupType, ReferenceType)
- [x] Repositories
- [x] `BalanceCalculator`

### ✅ Sprint 3: Application Layer (TAMAMLANDI)
- [x] `WalletService`
- [x] `LedgerService`
- [x] Commands (CreateWallet, RecordTransaction)
- [x] Use-cases (Deposit, Withdrawal, Transfer)

### ✅ Sprint 4: REST API (TAMAMLANDI)
- [x] `WalletController`
- [x] `TransactionController`
- [x] Use-case controllers
- [x] DTOs (Request/Response)
- [x] Global exception handler
- [x] OpenAPI documentation

### ✅ Sprint 5: Advanced Features (TAMAMLANDI)
- [x] Idempotency mechanism
- [x] FX conversion (mock rates)
- [x] Transaction filtering/pagination
- [x] Validation improvements

---

## 📝 Notlar

### Neden Balance Stored Değil?

1. **Audit Trail:** Her değişiklik izlenebilir
2. **Reconciliation:** Tutarsızlık tespit edilebilir
3. **Compliance:** Regulatörler transaction history ister
4. **Debugging:** "Bu bakiye nereden geldi?" sorusu cevaplanabilir

### Neden Direct Balance Update Yok?

```java
// ❌ YANLIŞ - Tipik CRUD yaklaşımı
wallet.setBalance(wallet.getBalance().add(amount));

// ✅ DOĞRU - Ledger-based yaklaşım
Transaction tx = Transaction.credit(wallet, amount, DEPOSIT);
transactionRepository.save(tx);
// Balance = SUM(transactions) ile hesaplanır
```

### Event Sourcing'e Geçiş Yolu

Şu anki yapı event sourcing'e geçişe hazır:
- Transaction = Event
- Wallet Balance = Projection
- Gelecekte: Event Store (MongoDB Atlas) + CQRS

---

## 🔗 İlgili Dökümanlar

- [Mimari Spesifikasyon](Spring%20Boot%20+%20JPA%20+%20Angular%20ile%20vitrinlik,%20gerçekç)
- UI Analiz (ui analiz.md)



Idempotency key (aynı isteği 2 kez yollayınca çift kayıt olmasın)
Optimistic locking (concurrency için)
Transfer = 2 transaction atomik (from debit + to credit tek DB transaction) kurulacak. 