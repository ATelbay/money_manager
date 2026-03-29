---
description: "Room Database and DataStore in Money Manager: entities (Account, Category, Transaction), DAO, migrations, ForeignKeys, prepopulation, Preferences DataStore"
---

# Room Database & DataStore

## Context

The project uses Room (2.8.4) for local storage and Preferences DataStore (1.1.7) for user settings.

**Key files:**
- `core/database/src/.../MoneyManagerDatabase.kt` — @Database class
- `core/database/src/.../entity/` — AccountEntity.kt, CategoryEntity.kt, TransactionEntity.kt
- `core/database/src/.../dao/` — AccountDao.kt, CategoryDao.kt, TransactionDao.kt
- `core/database/src/.../DefaultCategories.kt` — 15 pre-installed categories
- `core/database/src/.../di/DatabaseModule.kt` — Hilt module for Room
- `core/datastore/src/.../UserPreferences.kt` — DataStore wrapper
- `core/datastore/src/.../di/DataStoreModule.kt` — Hilt module for DataStore

## Entities

3 entities with ForeignKeys:

- **AccountEntity** — bank accounts (name, currency, balance)
- **CategoryEntity** — transaction categories (name, icon, type: INCOME/EXPENSE)
- **TransactionEntity** — transactions (amount, date, description, type, accountId FK, categoryId FK)

ForeignKeys: TransactionEntity → AccountEntity, TransactionEntity → CategoryEntity

## Prepopulation

15 pre-installed categories (10 expense + 5 income) are defined in `DefaultCategories.kt`.
Loaded via `RoomDatabase.Callback.onCreate` — inserted ONCE on first database creation.

## DataStore (UserPreferences)

| Key | Type | Description |
|-----|------|-------------|
| `onboarding_completed` | Boolean | Whether onboarding has been completed |
| `selected_account_id` | Long? | Currently selected account |
| `theme_mode` | String | Theme: `"system"`, `"light"`, `"dark"` |

## Cloud Sync (core:firestore + data:sync)

The project contains additional modules for cloud synchronization:

- **`core:firestore`** — wrapper over Firebase Firestore SDK (Firestore instance, query helpers)
- **`data:sync`** — SyncManager: bidirectional synchronization Room ↔ Firestore

Dependency: `data:sync` → `core:firestore` + `core:database`

These modules are NOT part of the standard CRUD layer — they work in parallel with Room and do not change the DAO pattern. There is no need to add `core:firestore` to domain or presentation modules.

## Process

### Adding a New Entity
1. Create `{Name}Entity.kt` in `core/database/src/.../entity/`
2. Add `@Entity` annotation with tableName, indices, foreignKeys
3. Create `{Name}Dao.kt` in `core/database/src/.../dao/`
4. Register the entity in `@Database(entities = [...])` in `MoneyManagerDatabase.kt`
5. Add abstract fun for DAO in `MoneyManagerDatabase.kt`
6. Provide DAO through `DatabaseModule.kt`
7. Increment the DB version and **create a migration**

### Creating a Migration
1. Increment `version` in `@Database(version = N+1)`
2. Create `Migration(N, N+1)` with SQL for ALTER TABLE / CREATE TABLE
3. Add the migration to `.addMigrations()` when building the database in `DatabaseModule.kt`
4. Test the migration

### Adding a New DAO Method
1. Add the method to the corresponding `*Dao.kt`
2. For queries with `Flow` use `@Query` + return `Flow<List<Entity>>`
3. For inserts use `@Insert(onConflict = OnConflictStrategy.REPLACE)`
4. For deletions — `@Delete` or `@Query("DELETE FROM ...")`

## Quality Bar

- All DAO methods with `Flow` for reactive UI updates
- `@Insert` with `onConflict = REPLACE` for upsert behavior
- ForeignKeys with `onDelete = CASCADE` where logically appropriate
- Indices on frequently filtered columns (accountId, categoryId, date)
- DataStore operations via `suspend` functions

## Anti-patterns

- Do NOT access Room directly from ViewModel — only through Repository
- Do NOT use `allowMainThreadQueries()` — all queries via Coroutines
- Do NOT forget migrations when changing the schema — `fallbackToDestructiveMigration` only for debug
- Do NOT hardcode SQL strings in multiple places — define TABLE_NAME as companion const
- Do NOT create Entity without `@PrimaryKey(autoGenerate = true)` for auto-increment ID
