package com.atelbay.money_manager.domain.importstatement.usecase

import androidx.room.withTransaction
import com.atelbay.money_manager.core.database.MoneyManagerDatabase
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.database.entity.TransactionEntity
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionOverride
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.domain.categories.usecase.SaveCategoryUseCase
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import javax.inject.Inject

class ImportTransactionsUseCase @Inject constructor(
    private val database: MoneyManagerDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val saveCategoryUseCase: SaveCategoryUseCase,
) {

    suspend operator fun invoke(
        transactions: List<ParsedTransaction>,
        accountId: Long,
        overrides: Map<Int, TransactionOverride>,
    ): Int {
        // Mutable per-type caches of existing categories; newly-created ones are appended so the
        // same name is created at most once per batch.
        val categoriesByType = mapOf(
            TransactionType.EXPENSE to categoryDao.getByType(TYPE_EXPENSE).toMutableList(),
            TransactionType.INCOME to categoryDao.getByType(TYPE_INCOME).toMutableList(),
        )

        suspend fun findOrCreate(name: String, type: TransactionType): Long {
            val cache = categoriesByType.getValue(type)
            cache.firstOrNull { it.name == name }?.let { return it.id }
            val id = saveCategoryUseCase(
                Category(
                    name = name,
                    icon = IMPORT_CATEGORY_ICON,
                    color = DEFAULT_IMPORT_CATEGORY_COLOR,
                    type = type,
                    isDefault = false,
                ),
            )
            cache.add(
                CategoryEntity(
                    id = id,
                    name = name,
                    icon = IMPORT_CATEGORY_ICON,
                    color = DEFAULT_IMPORT_CATEGORY_COLOR,
                    type = type.value,
                    isDefault = false,
                ),
            )
            return id
        }

        // Resolution order: user override → parser-assigned id → pending name (create) →
        // AI-suggested name matched to an existing category → "Other" fallback (create if missing).
        // Every transaction always resolves to a non-null category, so none are silently dropped.
        suspend fun resolveCategoryId(
            tx: ParsedTransaction,
            override: TransactionOverride?,
            type: TransactionType,
        ): Long {
            override?.categoryId?.let { return it }
            tx.categoryId?.let { return it }
            tx.pendingCategoryName?.let { return findOrCreate(it, type) }
            tx.suggestedCategoryName?.let { name ->
                categoriesByType.getValue(type).firstOrNull { it.name == name }?.let { return it.id }
            }
            return findOrCreate(FALLBACK_CATEGORY_NAME, type)
        }

        val entities = transactions.mapIndexed { index, tx ->
            val override = overrides[index]
            val type = override?.type ?: tx.type
            val categoryId = resolveCategoryId(tx, override, type)
            val date = override?.date ?: tx.date
            val amount = override?.amount ?: tx.amount
            val details = override?.details ?: tx.details
            val dateMillis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            TransactionEntity(
                amount = amount,
                type = type.value,
                categoryId = categoryId,
                accountId = accountId,
                note = details,
                date = dateMillis,
                createdAt = System.currentTimeMillis(),
                uniqueHash = tx.uniqueHash,
            )
        }

        // Chunk to stay under SQLite's bound-parameter limit (~999) on large statements.
        val existingHashes = entities.mapNotNull { it.uniqueHash }
            .chunked(SQLITE_VAR_CHUNK)
            .flatMap { transactionDao.getExistingHashes(it) }
            .toSet()
        val toInsert = entities.filter { it.uniqueHash == null || it.uniqueHash !in existingHashes }

        var actuallyInserted = 0
        database.withTransaction {
            val insertedIds = transactionDao.insertOrIgnore(toInsert)
            val now = System.currentTimeMillis()
            insertedIds.forEachIndexed { index, rowId ->
                if (rowId != -1L) {
                    val entity = toInsert[index]
                    val delta = if (entity.type == TYPE_INCOME) entity.amount else -entity.amount
                    accountDao.updateBalance(entity.accountId, delta, now)
                    actuallyInserted++
                }
            }
        }

        return actuallyInserted
    }

    companion object {
        private const val TYPE_EXPENSE = "expense"
        private const val TYPE_INCOME = "income"
        private const val FALLBACK_CATEGORY_NAME = "Другое"
        private const val IMPORT_CATEGORY_ICON = "label"
        private const val DEFAULT_IMPORT_CATEGORY_COLOR = 0xFFB0BEC5
        private const val SQLITE_VAR_CHUNK = 900
    }
}
