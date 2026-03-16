package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType as ModelTransactionType
import com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayMode
import com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayResolver
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.core.ui.util.normalizeCurrencyCode
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.usecase.ConvertAmountUseCase
import com.atelbay.money_manager.domain.statistics.model.PeriodSummary
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import kotlin.math.floor

data class StatisticsCurrencyResolution(
    val currencyUiState: StatisticsCurrencyUiState,
    val displayedTotalExpenses: Double?,
    val displayedTotalIncome: Double?,
    val displayedExpensesByCategory: List<StatisticsCategoryDisplayItem>,
    val displayedIncomesByCategory: List<StatisticsCategoryDisplayItem>,
    val displayedDailyExpenses: List<StatisticsDisplayDailyTotal>,
    val displayedDailyIncome: List<StatisticsDisplayDailyTotal>,
    val displayedMonthlyExpenses: List<StatisticsDisplayMonthlyTotal>,
    val displayedMonthlyIncome: List<StatisticsDisplayMonthlyTotal>,
)

class StatisticsCurrencyDisplayResolver @Inject constructor(
    private val convertAmountUseCase: ConvertAmountUseCase,
) {

    fun resolve(
        summary: PeriodSummary,
        transactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: String,
        exchangeRate: ExchangeRate?,
    ): StatisticsCurrencyResolution {
        val accountCurrencyById = accounts.associate { it.id to it.currency.normalizeCurrencyCode() }
        val scopedCurrencies = transactions
            .mapNotNull { accountCurrencyById[it.accountId] }
            .toSet()
        val normalizedBaseCurrency = baseCurrency.normalizeCurrencyCode()
        val canConvertAll = transactions.all { transaction ->
            val currency = accountCurrencyById[transaction.accountId] ?: return@all false
            canConvert(currency = currency, baseCurrency = normalizedBaseCurrency, exchangeRate = exchangeRate)
        }
        val aggregateResolution = AggregateCurrencyDisplayResolver.resolve(
            baseCurrency = normalizedBaseCurrency,
            scopedCurrencies = scopedCurrencies,
            canDisplayInBaseCurrency = canConvertAll,
        )

        if (aggregateResolution.displayMode == AggregateCurrencyDisplayMode.UNAVAILABLE) {
            return StatisticsCurrencyResolution(
                currencyUiState = StatisticsCurrencyUiState(
                    moneyDisplay = MoneyDisplayFormatter.format(MoneyDisplayFormatter.unavailable()),
                    displayMode = AggregateCurrencyDisplayMode.UNAVAILABLE,
                ),
                displayedTotalExpenses = null,
                displayedTotalIncome = null,
                displayedExpensesByCategory = summary.expensesByCategory.map {
                    StatisticsCategoryDisplayItem(category = it, displayAmount = null)
                },
                displayedIncomesByCategory = summary.incomesByCategory.map {
                    StatisticsCategoryDisplayItem(category = it, displayAmount = null)
                },
                displayedDailyExpenses = summary.dailyExpenses.map {
                    StatisticsDisplayDailyTotal(date = it.date, amount = null)
                },
                displayedDailyIncome = summary.dailyIncome.map {
                    StatisticsDisplayDailyTotal(date = it.date, amount = null)
                },
                displayedMonthlyExpenses = summary.monthlyExpenses.map {
                    StatisticsDisplayMonthlyTotal(
                        year = it.year,
                        month = it.month,
                        label = it.label,
                        amount = null,
                    )
                },
                displayedMonthlyIncome = summary.monthlyIncome.map {
                    StatisticsDisplayMonthlyTotal(
                        year = it.year,
                        month = it.month,
                        label = it.label,
                        amount = null,
                    )
                },
            )
        }

        val displayCurrency = aggregateResolution.displayCurrency ?: normalizedBaseCurrency
        val displayMoney = MoneyDisplayFormatter.resolveAndFormat(displayCurrency)
        val displayTransactions = transactions.mapNotNull { transaction ->
            val sourceCurrency = accountCurrencyById[transaction.accountId] ?: return@mapNotNull null
            val displayAmount = when (aggregateResolution.displayMode) {
                AggregateCurrencyDisplayMode.CONVERTED -> convert(
                    amount = transaction.amount,
                    sourceCurrency = sourceCurrency,
                    baseCurrency = normalizedBaseCurrency,
                    exchangeRate = exchangeRate,
                )

                AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY -> transaction.amount
                AggregateCurrencyDisplayMode.UNAVAILABLE -> null
            } ?: return@mapNotNull null

            DisplayTransaction(
                transaction = transaction,
                amount = displayAmount,
            )
        }

        val expenseTransactions = displayTransactions.filter { it.transaction.type == ModelTransactionType.EXPENSE }
        val incomeTransactions = displayTransactions.filter { it.transaction.type == ModelTransactionType.INCOME }
        val displayedTotalExpenses = expenseTransactions.sumOf(DisplayTransaction::amount)
        val displayedTotalIncome = incomeTransactions.sumOf(DisplayTransaction::amount)

        return StatisticsCurrencyResolution(
            currencyUiState = StatisticsCurrencyUiState(
                moneyDisplay = displayMoney,
                displayMode = aggregateResolution.displayMode,
            ),
            displayedTotalExpenses = displayedTotalExpenses,
            displayedTotalIncome = displayedTotalIncome,
            displayedExpensesByCategory = buildCategoryItems(
                source = summary.expensesByCategory,
                transactions = expenseTransactions,
                total = displayedTotalExpenses,
            ),
            displayedIncomesByCategory = buildCategoryItems(
                source = summary.incomesByCategory,
                transactions = incomeTransactions,
                total = displayedTotalIncome,
            ),
            displayedDailyExpenses = summary.dailyExpenses.map { total ->
                StatisticsDisplayDailyTotal(
                    date = total.date,
                    amount = expenseTransactions
                        .filter { startOfDay(it.transaction.date) == total.date }
                        .sumOf(DisplayTransaction::amount),
                )
            },
            displayedDailyIncome = summary.dailyIncome.map { total ->
                StatisticsDisplayDailyTotal(
                    date = total.date,
                    amount = incomeTransactions
                        .filter { startOfDay(it.transaction.date) == total.date }
                        .sumOf(DisplayTransaction::amount),
                )
            },
            displayedMonthlyExpenses = summary.monthlyExpenses.map { total ->
                StatisticsDisplayMonthlyTotal(
                    year = total.year,
                    month = total.month,
                    label = total.label,
                    amount = expenseTransactions
                        .filter {
                            val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
                                timeInMillis = it.transaction.date
                            }
                            calendar.get(Calendar.YEAR) == total.year &&
                                calendar.get(Calendar.MONTH) == total.month
                        }
                        .sumOf(DisplayTransaction::amount),
                )
            },
            displayedMonthlyIncome = summary.monthlyIncome.map { total ->
                StatisticsDisplayMonthlyTotal(
                    year = total.year,
                    month = total.month,
                    label = total.label,
                    amount = incomeTransactions
                        .filter {
                            val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
                                timeInMillis = it.transaction.date
                            }
                            calendar.get(Calendar.YEAR) == total.year &&
                                calendar.get(Calendar.MONTH) == total.month
                        }
                        .sumOf(DisplayTransaction::amount),
                )
            },
        )
    }

    private fun buildCategoryItems(
        source: List<com.atelbay.money_manager.domain.statistics.model.CategorySummary>,
        transactions: List<DisplayTransaction>,
        total: Double,
    ): List<StatisticsCategoryDisplayItem> {
        val amountByCategoryId = transactions
            .groupBy { it.transaction.categoryId }
            .mapValues { (_, items) -> items.sumOf(DisplayTransaction::amount) }
        val percentageByCategoryId = buildPercentages(amountByCategoryId, total)

        return source.map { category ->
            StatisticsCategoryDisplayItem(
                category = category,
                displayAmount = amountByCategoryId[category.categoryId] ?: 0.0,
                displayPercentage = percentageByCategoryId[category.categoryId] ?: 0,
            )
        }
    }

    private fun buildPercentages(
        amountByCategoryId: Map<Long, Double>,
        total: Double,
    ): Map<Long, Int> {
        if (total <= 0.0 || amountByCategoryId.isEmpty()) {
            return amountByCategoryId.keys.associateWith { 0 }
        }

        val raw = amountByCategoryId.map { (id, amount) ->
            val percentage = amount / total * 100.0
            val floored = floor(percentage).toInt()
            Triple(id, floored, percentage - floored)
        }
        val percentages = raw.associate { (id, floored, _) -> id to floored }.toMutableMap()
        var deficit = 100 - percentages.values.sum()

        raw.sortedByDescending { it.third }.forEach { (id, _, _) ->
            if (deficit <= 0) return@forEach
            percentages[id] = (percentages[id] ?: 0) + 1
            deficit--
        }

        return percentages
    }

    private fun canConvert(
        currency: String,
        baseCurrency: String,
        exchangeRate: ExchangeRate?,
    ): Boolean = convert(
        amount = 1.0,
        sourceCurrency = currency,
        baseCurrency = baseCurrency,
        exchangeRate = exchangeRate,
    ) != null

    private fun convert(
        amount: Double,
        sourceCurrency: String,
        baseCurrency: String,
        exchangeRate: ExchangeRate?,
    ): Double? {
        if (sourceCurrency == baseCurrency) return amount
        val quotes = exchangeRate?.quotes ?: return null

        return runCatching {
            convertAmountUseCase(
                amount = amount,
                sourceCurrency = sourceCurrency,
                targetCurrency = baseCurrency,
                quotes = quotes,
            )
        }.getOrNull()
    }

    private fun startOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private data class DisplayTransaction(
        val transaction: Transaction,
        val amount: Double,
    )
}
