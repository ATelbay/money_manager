# Data Model: 018-debts-v1-readiness

**Created**: 2026-04-09

---

## Entities

### DebtEntity (Room — `debts` table)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, autoGenerate | |
| contactName | String | NOT NULL | Name of debtor/creditor |
| direction | String | NOT NULL | "LENT" or "BORROWED" |
| totalAmount | Double | NOT NULL | Always positive |
| currency | String | NOT NULL | Matches account currency |
| accountId | Long | NOT NULL, FK → accounts.id ON DELETE CASCADE | |
| note | String? | nullable | |
| createdAt | Long | NOT NULL | Epoch millis |
| remoteId | String? | nullable, indexed | Firestore document ID |
| updatedAt | Long | NOT NULL, default 0 | Last modification timestamp |
| isDeleted | Boolean | NOT NULL, default false | Soft delete flag |

**Indexes**: `accountId`, `remoteId`

### DebtPaymentEntity (Room — `debt_payments` table)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, autoGenerate | |
| debtId | Long | NOT NULL, FK → debts.id ON DELETE CASCADE | |
| amount | Double | NOT NULL | Always positive |
| date | Long | NOT NULL | Payment date, epoch millis |
| note | String? | nullable | |
| transactionId | Long? | nullable, FK → transactions.id ON DELETE SET NULL | Linked transaction |
| createdAt | Long | NOT NULL | Epoch millis |
| remoteId | String? | nullable, indexed | Firestore document ID |
| updatedAt | Long | NOT NULL, default 0 | |
| isDeleted | Boolean | NOT NULL, default false | |

**Indexes**: `debtId`, `remoteId`

---

## Domain Models (core/model)

### Debt

```
data class Debt(
    id: Long = 0,
    contactName: String,
    direction: DebtDirection,
    totalAmount: Double,
    paidAmount: Double = 0.0,       // computed: sum of payments
    remainingAmount: Double,         // computed: totalAmount - paidAmount
    currency: String,
    accountId: Long,
    accountName: String = "",        // denormalized for display
    note: String? = null,
    createdAt: Long,
    status: DebtStatus,              // derived: PAID_OFF if remainingAmount <= 0
)
```

### DebtPayment

```
data class DebtPayment(
    id: Long = 0,
    debtId: Long,
    amount: Double,
    date: Long,
    note: String? = null,
    transactionId: Long? = null,
    createdAt: Long,
)
```

### DebtDirection (enum)

```
enum class DebtDirection { LENT, BORROWED }
```

### DebtStatus (enum — derived, not stored)

```
enum class DebtStatus { ACTIVE, PAID_OFF }
```

---

## Firestore DTOs (core/firestore)

### DebtDto

| Field | Firestore Type | Notes |
|-------|---------------|-------|
| remoteId | String | @DocumentId |
| contactName | String | Encrypted via FieldCipher |
| direction | String | "LENT" or "BORROWED" (not encrypted) |
| totalAmount | String | Encrypted double |
| currency | String | Not encrypted |
| accountRemoteId | String | FK resolved via account's remoteId |
| note | String | Encrypted, empty if null |
| createdAt | Long | |
| updatedAt | Long | |
| isDeleted | Boolean | |
| encryptionVersion | Int | |

**Firestore path**: `users/{userId}/debts/{remoteId}`

### DebtPaymentDto

| Field | Firestore Type | Notes |
|-------|---------------|-------|
| remoteId | String | @DocumentId |
| debtRemoteId | String | FK resolved via debt's remoteId |
| amount | String | Encrypted double |
| date | Long | |
| note | String | Encrypted, empty if null |
| transactionRemoteId | String | Nullable FK via transaction's remoteId |
| createdAt | Long | |
| updatedAt | Long | |
| isDeleted | Boolean | |
| encryptionVersion | Int | |

**Firestore path**: `users/{userId}/debt_payments/{remoteId}`

---

## Relationships

```
Account 1──*  Debt
Debt    1──*  DebtPayment
Transaction 1──0..* DebtPayment (nullable FK, ON DELETE SET NULL)
```

- Deleting an Account cascades to its Debts, which cascade to DebtPayments
- Deleting a Debt cascades to its DebtPayments
- Deleting a Transaction sets DebtPayment.transactionId to null
- Deleting a DebtPayment does NOT affect the linked Transaction

---

## State Transitions

```
Debt Lifecycle:

  ACTIVE ──[payments sum >= totalAmount]──> PAID_OFF
  PAID_OFF ──[payment deleted, sum < totalAmount]──> ACTIVE

  Status is derived at query time, not stored.
  Direction change (LENT↔BORROWED) with existing payments:
    → Warning dialog → user accepts → delete all payments → apply change
    → User declines → cancel direction change
```

---

## New Default Categories

| Name (RU) | Name (EN) | Name (KZ) | Type | Icon | Color |
|-----------|-----------|-----------|------|------|-------|
| Долги | Debts | Қарыздар | EXPENSE | MoneyOff | 0xFFEF4444 (red) |
| Возврат долга | Debt Repayment | Қарыз қайтару | INCOME | Payments | 0xFF22C55E (green) |

---

## Migration SQL (v7 → v8)

```sql
CREATE TABLE IF NOT EXISTS debts (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    contactName TEXT NOT NULL,
    direction TEXT NOT NULL,
    totalAmount REAL NOT NULL,
    currency TEXT NOT NULL,
    accountId INTEGER NOT NULL,
    note TEXT,
    createdAt INTEGER NOT NULL,
    remoteId TEXT,
    updatedAt INTEGER NOT NULL DEFAULT 0,
    isDeleted INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS index_debts_accountId ON debts(accountId);
CREATE INDEX IF NOT EXISTS index_debts_remoteId ON debts(remoteId);

CREATE TABLE IF NOT EXISTS debt_payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    debtId INTEGER NOT NULL,
    amount REAL NOT NULL,
    date INTEGER NOT NULL,
    note TEXT,
    transactionId INTEGER,
    createdAt INTEGER NOT NULL,
    remoteId TEXT,
    updatedAt INTEGER NOT NULL DEFAULT 0,
    isDeleted INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(debtId) REFERENCES debts(id) ON DELETE CASCADE,
    FOREIGN KEY(transactionId) REFERENCES transactions(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS index_debt_payments_debtId ON debt_payments(debtId);
CREATE INDEX IF NOT EXISTS index_debt_payments_remoteId ON debt_payments(remoteId);
```
