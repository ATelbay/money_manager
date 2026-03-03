package com.atelbay.money_manager.presentation.transactions.ui.list

import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDate

data class TransactionListState(
    val transactions: ImmutableList<Transaction> = persistentListOf(),
    val transactionRows: ImmutableList<TransactionRowState> = persistentListOf(),
    val balance: Double = 0.0,
    val currency: String = "",
    val displayCurrency: String = "",
    val isUsingConvertedTotals: Boolean = false,
    val isUsingFallbackCurrency: Boolean = false,
    val isLoading: Boolean = true,
    val selectedAccountName: String? = null,
    val selectedTab: TransactionType? = null,
    val selectedPeriod: Period = Period.ALL,
    val customDateRange: Pair<LocalDate, LocalDate>? = null,
    val periodIncome: Double = 0.0,
    val periodExpense: Double = 0.0,
    val searchQuery: String = "",
)

data class TransactionRowState(
    val transaction: Transaction,
    val originalAmount: Double,
    val originalCurrency: String,
    val convertedAmount: Double? = null,
    val convertedCurrency: String? = null,
    val conversionStatus: ConversionStatus = ConversionStatus.UNAVAILABLE,
) {
    val displayAmount: Double
        get() = convertedAmount ?: originalAmount

    val displayCurrency: String
        get() = convertedCurrency ?: originalCurrency
}

enum class ConversionStatus {
    AVAILABLE,
    UNAVAILABLE,
}

enum class Period {
    ALL,
    TODAY,
    WEEK,
    MONTH,
    YEAR,
    CUSTOM,
}
