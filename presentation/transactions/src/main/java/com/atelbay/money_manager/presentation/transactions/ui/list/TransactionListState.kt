package com.atelbay.money_manager.presentation.transactions.ui.list

import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.core.ui.util.MoneyDisplayPresentation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class TransactionListState(
    val transactionRows: ImmutableList<TransactionRowState> = persistentListOf(),
    val balance: Double? = null,
    val summaryMoneyDisplay: MoneyDisplayPresentation = MoneyDisplayFormatter.unavailable().let(MoneyDisplayFormatter::format),
    val isLoading: Boolean = true,
    val selectedAccountName: String? = null,
    val selectedAccountId: Long? = null,
    val selectedTab: TransactionType? = null,
    val selectedPeriod: Period = Period.CURRENT_MONTH,
    val periodIncome: Double? = null,
    val periodExpense: Double? = null,
    val searchQuery: String = "",
    val accounts: ImmutableList<Account> = persistentListOf(),
    val showAccountPicker: Boolean = false,
    val dailyNetSums: Map<String, Double> = emptyMap(),
    val customDateRange: Pair<Long, Long>? = null,
    val showDatePickerDialog: Boolean = false,
    val customDateLabel: String? = null,
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
    val accountName: String? = null,
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
    CURRENT_MONTH,
    TODAY,
    WEEK,
    MONTH,
    YEAR,
}
