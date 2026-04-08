# Data Model Changes: 017-bugfixes-and-sync

## Modified Entities

### AccountDao (new query only)
```
getByNameAndCurrency(name: String, currency: String) → AccountEntity?
  WHERE name = :name AND currency = :currency AND isDeleted = 0 AND remoteId IS NULL LIMIT 1
```

### BudgetDao (new sync queries)
```
getByRemoteId(remoteId: String) → BudgetEntity?
getPendingSync() → List<BudgetEntity>          // remoteId IS NULL AND isDeleted = 0
getDeletedWithRemoteId() → List<BudgetEntity>  // isDeleted = 1 AND remoteId IS NOT NULL
upsertSync(budgets: List<BudgetEntity>)        // INSERT OR REPLACE
clearRemoteIds()                                // UPDATE budgets SET remoteId = NULL
softDeleteById(id: Long, updatedAt: Long)      // UPDATE SET isDeleted = 1 (alias for pull use)
```

### RecurringTransactionDao (new sync queries)
```
getByRemoteId(remoteId: String) → RecurringTransactionEntity?
getPendingSync() → List<RecurringTransactionEntity>
getDeletedWithRemoteId() → List<RecurringTransactionEntity>
upsertSync(entities: List<RecurringTransactionEntity>)
clearRemoteIds()
softDeleteById(id: Long, updatedAt: Long)
```

## New DTOs

### BudgetDto (Firestore: users/{userId}/budgets)
```
@DocumentId remoteId: String = ""
categoryRemoteId: String = ""       // FK → categories collection
monthlyLimit: String = ""           // encrypted
createdAt: Long = 0
updatedAt: Long = 0
isDeleted: Boolean = false
encryptionVersion: Int = 0
```

### RecurringTransactionDto (Firestore: users/{userId}/recurring_transactions)
```
@DocumentId remoteId: String = ""
amount: String = ""                 // encrypted
type: String = ""
categoryRemoteId: String = ""      // FK → categories collection
accountRemoteId: String = ""       // FK → accounts collection
note: String? = null
frequency: String = ""
startDate: Long = 0
endDate: Long? = null
dayOfMonth: Int? = null
dayOfWeek: Int? = null
lastGeneratedDate: Long? = null
isActive: Boolean = true
createdAt: Long = 0
updatedAt: Long = 0
isDeleted: Boolean = false
encryptionVersion: Int = 0
```

## Renamed Classes (no schema change)

| Old Name | New Name | Serialization Name (unchanged) |
|----------|----------|-------------------------------|
| ParserConfig | RegexParserProfile | `@SerialName("ParserConfig")` |
| ParserConfigList | RegexParserProfileList | preserve JSON key |
| TableParserConfig | TableParserProfile | `@SerialName("TableParserConfig")` |
| ParserConfigEntity | RegexParserProfileEntity | `@Entity(tableName = "parser_configs")` |
| ParserConfigDao | RegexParserProfileDao | — |
| ParserConfigProvider | RegexParserProfileProvider | — |
| ParserConfigSyncer | RegexParserProfileSyncer | — |
| FirebaseParserConfigProvider | FirebaseRegexParserProfileProvider | — |
| ParserConfigFirestoreDto | RegexParserProfileFirestoreDto | Firestore collection name unchanged |
| ParserConfigTestFactory | RegexParserProfileTestFactory | — |

## State Changes

### StatisticsChartState (new field)
```
allAmountsZero: Boolean = false    // set when all chart bar amounts are 0
```

### PendingNavigationManager (new field)
```
launchedFromExternal: Boolean = false  // set when intent action is SEND or VIEW
```

## Sync Pull Order (updated)
```
Before: pull(accounts → categories → transactions) → pushPending → pushAll
After:  pushPending → pull(accounts → categories → budgets → recurring → transactions) → pushAll
```
