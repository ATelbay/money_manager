# Data Model: App Audit Phase 1

## Entity Changes

### AccountEntity (modified)

Add indexes for `isDeleted` and `remoteId` columns.

```
@Entity(
    tableName = "accounts",
    indices = [
        Index("isDeleted"),
        Index("remoteId"),
    ]
)
```

**Fields**: No changes. Existing fields: `id`, `name`, `currency`, `balance`, `createdAt`, `remoteId`, `updatedAt`, `isDeleted`.

**Queries served by new indexes**:
- `observeAll()`: WHERE isDeleted = 0
- `getAllForSync()`: WHERE isDeleted = 0
- `getPendingSync()`: WHERE remoteId IS NULL AND isDeleted = 0
- `getByRemoteId()`: WHERE remoteId = :remoteId

---

### CategoryEntity (modified)

Add composite index for `(type, isDeleted)` and single index for `remoteId`.

```
@Entity(
    tableName = "categories",
    indices = [
        Index("type", "isDeleted"),
        Index("remoteId"),
    ]
)
```

**Fields**: No changes. Existing fields: `id`, `name`, `icon`, `color`, `type`, `isDefault`, `remoteId`, `updatedAt`, `isDeleted`.

**Queries served by new indexes**:
- `observeByType()`: WHERE isDeleted = 0 AND type = :type
- `getByType()`: WHERE isDeleted = 0 AND type = :type
- `observeAll()`: WHERE isDeleted = 0 (uses type_isDeleted index prefix? No — separate isDeleted index not added, but composite covers isDeleted as second column)
- `getByRemoteId()`: WHERE remoteId = :remoteId

---

### TransactionEntity (modified)

Add composite index for statistics query and `isDeleted` index.

```
@Entity(
    tableName = "transactions",
    indices = [
        Index("accountId"),         // existing
        Index("categoryId"),        // existing
        Index("date"),              // existing
        Index("uniqueHash", unique = true),  // existing
        Index("categoryId", "type", "date"), // NEW — statistics
        Index("isDeleted"),         // NEW
    ]
)
```

**Fields**: No changes.

**Queries served by new composite index**:
- `observeByCategoryTypeAndDateRange()`: WHERE isDeleted = 0 AND categoryId = :categoryId AND type = :type AND date BETWEEN :startDate AND :endDate

---

### SyncStatus (moved from data:sync → core:model)

```
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Synced(val lastSyncedAt: Long) : SyncStatus
    data class Failed(val lastSyncedAt: Long?) : SyncStatus
}
```

**Package change**: `com.atelbay.money_manager.data.sync.SyncStatus` → `com.atelbay.money_manager.core.model.SyncStatus`

---

### SyncRepository (new — domain:sync)

```
interface SyncRepository {
    val syncStatus: Flow<SyncStatus>
    fun retrySync()
}
```

**Package**: `com.atelbay.money_manager.domain.sync`

---

## Database Migration: Version 3 → 4

**Migration file**: `core/database/.../migration/Migration_3_4.kt`

```sql
CREATE INDEX IF NOT EXISTS index_accounts_isDeleted ON accounts (isDeleted)
CREATE INDEX IF NOT EXISTS index_accounts_remoteId ON accounts (remoteId)
CREATE INDEX IF NOT EXISTS index_categories_type_isDeleted ON categories (type, isDeleted)
CREATE INDEX IF NOT EXISTS index_categories_remoteId ON categories (remoteId)
CREATE INDEX IF NOT EXISTS index_transactions_categoryId_type_date ON transactions (categoryId, type, date)
CREATE INDEX IF NOT EXISTS index_transactions_isDeleted ON transactions (isDeleted)
```

**Backwards compatible**: Yes. `CREATE INDEX IF NOT EXISTS` is safe for all scenarios.

---

## AppStrings Keys (new)

### Validation Errors (ViewModels)
| Key | Russian | English | Kazakh |
|-----|---------|---------|--------|
| `errorEnterAccountName` | Введите название счёта | Enter account name | Шот атауын енгізіңіз |
| `errorEnterCategoryName` | Введите название категории | Enter category name | Санат атауын енгізіңіз |
| `errorEnterValidAmount` | Введите корректную сумму | Enter a valid amount | Дұрыс сома енгізіңіз |
| `errorSelectCategory` | Выберите категорию | Select a category | Санатты таңдаңыз |
| `errorReadingPdf` | Ошибка чтения PDF | Error reading PDF | PDF оқу қатесі |
| `errorUnknown` | Неизвестная ошибка | Unknown error | Белгісіз қате |
| `errorNoTransactionsFound` | Не удалось найти транзакции в документе | No transactions found in document | Құжатта транзакциялар табылмады |
| `errorSelectAccountForImport` | Выберите счёт для импорта | Select an account for import | Импортқа шот таңдаңыз |
| `errorImport` | Ошибка импорта | Import error | Импорт қатесі |
| `errorNoInternet` | Нет подключения к интернету | No internet connection | Интернет байланысы жоқ |
| `errorSignInFailed` | Не удалось войти. Попробуйте снова | Sign-in failed. Please try again | Кіру сәтсіз. Қайта көріңіз |
| `errorLoadCategoryTransactions` | Не удалось загрузить транзакции | Unable to load transactions | Транзакцияларды жүктеу мүмкін болмады |
| `errorUpdateRate` | Не удалось обновить курс | Failed to update rate | Бағамды жаңарту сәтсіз |

### Import Preview (Composable)
| Key | Russian | English | Kazakh |
|-----|---------|---------|--------|
| `importFound` (lambda: total) | Найдено: $total | Found: $total | Табылды: $total |
| `importDuplicates` (lambda: count) | Дубликаты: $count | Duplicates: $count | Көшірмелер: $count |
| `importReadyCount` (lambda: ready, total) | К импорту: $ready из $total | Ready to import: $ready of $total | Импортқа: $ready / $total |
| `importNoCategory` (lambda: count) | Без категории: $count | No category: $count | Санатсыз: $count |
| `importButton` (lambda: count) | Импорт ($count) | Import ($count) | Импорт ($count) |
| `importNeedsReview` (lambda: count) | Требуют проверки ($count) | Need review ($count) | Тексеруді қажет етеді ($count) |
| `importRecognized` (lambda: count) | Распознаны ($count) | Recognized ($count) | Танылған ($count) |
| `importAccountLabel` | Счёт для импорта | Import account | Импорт шоты |
| `importSelectAccount` | Выберите счёт | Select account | Шотты таңдаңыз |
