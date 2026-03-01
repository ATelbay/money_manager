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

    private val categoryAliases = mapOf(
        "Перевод" to setOf("перевод", "transfer"),
        "Покупки" to setOf("покупка", "покупки", "purchase", "оплата", "payment"),
        "Пополнение" to setOf("пополнение", "deposit", "зачисление", "credit"),
    )

    suspend operator fun invoke(
        transactions: List<ParsedTransaction>,
        accountId: Long,
        overrides: Map<Int, TransactionOverride>,
    ): Int {
        val expenseCategories = categoryDao.getByType("expense")
        val incomeCategories = categoryDao.getByType("income")

        val fallbackExpense = expenseCategories.find { it.name == "Другое" }?.id
        val fallbackIncome = incomeCategories.find { it.name == "Другое" }?.id

        val entities = transactions.mapIndexed { index, tx ->
            val override = overrides[index]
            val type = override?.type ?: tx.type
            val categories = if (type == TransactionType.EXPENSE) expenseCategories else incomeCategories
            val fallback = if (type == TransactionType.EXPENSE) fallbackExpense else fallbackIncome

            val categoryId = override?.categoryId ?: tx.categoryId ?: run {
                val suggested = tx.suggestedCategoryName?.lowercase()
                if (suggested != null) {
                    val resolvedName = categoryAliases.entries
                        .firstOrNull { (_, aliases) -> suggested in aliases }
                        ?.key
                    if (resolvedName != null) {
                        categories.find { it.name == resolvedName }?.id
                    } else {
                        null
                    }
                } else {
                    null
                } ?: fallback
            } ?: return@mapIndexed null

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
