package com.atelbay.money_manager.presentation.transactions.ui.list

import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.core.ui.util.MoneyDisplayPresentation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDate

data class TransactionListState(
    val transactionRows: ImmutableList<TransactionRowState> = persistentListOf(),
    val balance: Double? = null,
    val displayCurrency: String? = null,
    val summaryMoneyDisplay: MoneyDisplayPresentation = MoneyDisplayFormatter.unavailable().let(MoneyDisplayFormatter::format),
    val summaryDisplayMode: SummaryDisplayMode = SummaryDisplayMode.UNAVAILABLE,
    val isLoading: Boolean = true,
    val selectedAccountName: String? = null,
    val selectedTab: TransactionType? = null,
    val selectedPeriod: Period = Period.MONTH,
    val customDateRange: Pair<LocalDate, LocalDate>? = null,
    val periodIncome: Double? = null,
    val periodExpense: Double? = null,
    val searchQuery: String = "",
)

data class TransactionRowState(
    val transaction: Transaction,
    val originalAmount: Double,
    val originalCurrency: String,
    val convertedAmount: Double? = null,
    val convertedCurrency: String? = null,
    val conversionStatus: ConversionStatus = ConversionStatus.UNAVAILABLE,
    val displayMoneyDisplay: MoneyDisplayPresentation,
    val secondaryMoneyDisplay: MoneyDisplayPresentation? = null,
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

enum class SummaryDisplayMode {
    CONVERTED,
    ORIGINAL_SINGLE_CURRENCY,
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
