package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import com.atelbay.money_manager.core.ui.util.MoneyDisplayPresentation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CategoryTransactionsState(
    val categoryId: Long = 0,
    val categoryName: String = "",
    val categoryIcon: String = "",
    val categoryColor: Long = 0L,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val period: StatsPeriod = StatsPeriod.MONTH,
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
    val transactions: ImmutableList<CategoryTransactionItem> = persistentListOf(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && error == null && transactions.isEmpty()
}

data class CategoryTransactionItem(
    val transactionId: Long,
    val description: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val amount: Double,
    val currency: String,
    val moneyDisplay: MoneyDisplayPresentation,
    val date: Long,
    val isIncome: Boolean,
)
