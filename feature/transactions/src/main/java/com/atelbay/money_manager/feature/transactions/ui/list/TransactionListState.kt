package com.atelbay.money_manager.feature.transactions.ui.list

import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDate

data class TransactionListState(
    val transactions: ImmutableList<Transaction> = persistentListOf(),
    val balance: Double = 0.0,
    val currency: String = "",
    val isLoading: Boolean = true,
    val selectedAccountName: String? = null,
    val selectedTab: TransactionType? = null,
    val selectedPeriod: Period = Period.ALL,
    val customDateRange: Pair<LocalDate, LocalDate>? = null,
    val periodIncome: Double = 0.0,
    val periodExpense: Double = 0.0,
    val searchQuery: String = "",
)

enum class Period {
    ALL,
    TODAY,
    WEEK,
    MONTH,
    YEAR,
    CUSTOM,
}
