package com.atelbay.money_manager.domain.importstatement.usecase

import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.database.entity.TransactionEntity
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionOverride
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import javax.inject.Inject

class ImportTransactionsUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) {

    suspend operator fun invoke(
        transactions: List<ParsedTransaction>,
        accountId: Long,
        overrides: Map<Int, TransactionOverride>,
    ): Int {
        val fallbackExpense = categoryDao.getByType("expense")
            .find { it.name == "Другое" }?.id
        val fallbackIncome = categoryDao.getByType("income")
            .find { it.name == "Другое" }?.id

        val entities = transactions.mapIndexed { index, tx ->
            val override = overrides[index]
            val type = override?.type ?: tx.type
            val fallback = if (type == TransactionType.EXPENSE) fallbackExpense else fallbackIncome
            val categoryId = override?.categoryId ?: tx.categoryId ?: fallback ?: return@mapIndexed null
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
        }.filterNotNull()

        transactionDao.insertOrIgnore(entities)

        entities.forEach { entity ->
            val delta = if (entity.type == "income") entity.amount else -entity.amount
            accountDao.updateBalance(entity.accountId, delta)
        }

        return entities.size
    }
}
