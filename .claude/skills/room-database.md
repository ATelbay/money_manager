---
description: "Room Database и DataStore в Money Manager: entities (Account, Category, Transaction), DAO, миграции, ForeignKeys, prepopulation, Preferences DataStore"
---

# Room Database & DataStore

## Context

Проект использует Room (2.8.4) для локального хранения и Preferences DataStore (1.1.7) для настроек пользователя.

**Ключевые файлы:**
- `core/database/src/.../MoneyManagerDatabase.kt` — @Database класс
- `core/database/src/.../entity/` — AccountEntity.kt, CategoryEntity.kt, TransactionEntity.kt
- `core/database/src/.../dao/` — AccountDao.kt, CategoryDao.kt, TransactionDao.kt
- `core/database/src/.../DefaultCategories.kt` — 15 предустановленных категорий
- `core/database/src/.../di/DatabaseModule.kt` — Hilt-модуль для Room
- `core/datastore/src/.../UserPreferences.kt` — DataStore обёртка
- `core/datastore/src/.../di/DataStoreModule.kt` — Hilt-модуль для DataStore

## Entities

3 entities с ForeignKeys:

- **AccountEntity** — банковские счета (name, currency, balance)
- **CategoryEntity** — категории транзакций (name, icon, type: INCOME/EXPENSE)
- **TransactionEntity** — транзакции (amount, date, description, type, accountId FK, categoryId FK)

ForeignKeys: TransactionEntity → AccountEntity, TransactionEntity → CategoryEntity

## Prepopulation

15 предустановленных категорий (10 расход + 5 доход) определены в `DefaultCategories.kt`.
Загружаются через `RoomDatabase.Callback.onCreate` — вставляются ОДИН раз при первом создании БД.

## DataStore (UserPreferences)

| Ключ | Тип | Описание |
|------|-----|----------|
| `onboarding_completed` | Boolean | Пройден ли онбординг |
| `selected_account_id` | Long? | Текущий выбранный счёт |
| `theme_mode` | String | Тема: `"system"`, `"light"`, `"dark"` |

## Process

### Добавление новой Entity
1. Создать `{Name}Entity.kt` в `core/database/src/.../entity/`
2. Добавить `@Entity` аннотацию с tableName, indices, foreignKeys
3. Создать `{Name}Dao.kt` в `core/database/src/.../dao/`
4. Зарегистрировать entity в `@Database(entities = [...])` в `MoneyManagerDatabase.kt`
5. Добавить abstract fun для DAO в `MoneyManagerDatabase.kt`
6. Провайдить DAO через `DatabaseModule.kt`
7. Увеличить версию БД и **создать миграцию**

### Создание миграции
1. Увеличить `version` в `@Database(version = N+1)`
2. Создать `Migration(N, N+1)` с SQL для ALTER TABLE / CREATE TABLE
3. Добавить миграцию в `.addMigrations()` при построении базы в `DatabaseModule.kt`
4. Протестировать миграцию

### Добавление нового DAO-метода
1. Добавить метод в соответствующий `*Dao.kt`
2. Для запросов с `Flow` использовать `@Query` + return `Flow<List<Entity>>`
3. Для вставок использовать `@Insert(onConflict = OnConflictStrategy.REPLACE)`
4. Для удаления — `@Delete` или `@Query("DELETE FROM ...")`

## Quality Bar

- Все DAO-методы с `Flow` для реактивных обновлений UI
- `@Insert` с `onConflict = REPLACE` для upsert-поведения
- ForeignKeys с `onDelete = CASCADE` где логически уместно
- Indices на часто фильтруемые колонки (accountId, categoryId, date)
- DataStore операции через `suspend` функции

## Anti-patterns

- НЕ обращайся к Room напрямую из ViewModel — только через Repository
- НЕ используй `allowMainThreadQueries()` — все запросы через Coroutines
- НЕ забывай миграцию при изменении схемы — `fallbackToDestructiveMigration` только для debug
- НЕ хардкодь SQL-строки в нескольких местах — определяй TABLE_NAME как companion const
- НЕ создавай Entity без `@PrimaryKey(autoGenerate = true)` для auto-increment ID
