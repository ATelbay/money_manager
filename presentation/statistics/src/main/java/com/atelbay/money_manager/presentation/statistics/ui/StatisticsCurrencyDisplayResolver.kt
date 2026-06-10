package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.core.common.startOfDay
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType as ModelTransactionType
import com.atelbay.money_manager.core.model.money.toMajorDouble
import com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayMode
import com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayResolver
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.core.ui.util.normalizeCurrencyCode
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.usecase.ConvertAmountUseCase
import com.atelbay.money_manager.domain.statistics.model.CategoryMetadata
import com.atelbay.money_manager.domain.statistics.model.PeriodSummary
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import kotlin.math.floor

data class StatisticsCurrencyResolution(
    val currencyUiState: StatisticsCurrencyUiState,
    val displayedTotalExpenses: Long?,
    val displayedTotalIncome: Long?,
    val displayedExpensesByCategory: List<StatisticsCategoryDisplayItem>,
    val displayedIncomesByCategory: List<StatisticsCategoryDisplayItem>,
    val displayedDailyExpenses: List<StatisticsDisplayDailyTotal>,
    val displayedDailyIncome: List<StatisticsDisplayDailyTotal>,
    val displayedMonthlyExpenses: List<StatisticsDisplayMonthlyTotal>,
    val displayedMonthlyIncome: List<StatisticsDisplayMonthlyTotal>,
)

/**
 * The single owner of statistics numeric aggregation. It takes the period [PeriodSummary] *skeleton*
 * (ordered day/month buckets + category catalog) plus one transaction stream, converts every
 * transaction into the display currency (or reports UNAVAILABLE for an unconvertible mixed scope),
 * and fills the skeleton buckets in a single pass each. The domain layer no longer aggregates, so
 * the transactions table is read exactly once (in the ViewModel) and nothing is summed twice.
 */
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
        val categoryMetaById = summary.categories.associateBy { it.categoryId }
        val accountCurrencyById = accounts.associate { it.id to it.currency.normalizeCurrencyCode(fallback = baseCurrency) }
        val scopedCurrencies = transactions
            .mapNotNull { accountCurrencyById[it.accountId] }
            .toSet()
        val normalizedBaseCurrency = baseCurrency.normalizeCurrencyCode(fallback = baseCurrency)
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
            val expenseTransactions = transactions.filter { it.type == ModelTransactionType.EXPENSE }
            val incomeTransactions = transactions.filter { it.type == ModelTransactionType.INCOME }
            return StatisticsCurrencyResolution(
                currencyUiState = StatisticsCurrencyUiState(
                    moneyDisplay = MoneyDisplayFormatter.format(MoneyDisplayFormatter.unavailable()),
                    displayMode = AggregateCurrencyDisplayMode.UNAVAILABLE,
                ),
                displayedTotalExpenses = null,
                displayedTotalIncome = null,
                // Category presence + ordering still reflect the (unconvertible) raw sums so the list
                // is stable; amounts are hidden by the UI lock.
                displayedExpensesByCategory = unavailableCategoryItems(expenseTransactions, categoryMetaById),
                displayedIncomesByCategory = unavailableCategoryItems(incomeTransactions, categoryMetaById),
                displayedDailyExpenses = summary.dayBuckets.map {
                    StatisticsDisplayDailyTotal(date = it, amount = null)
                },
                displayedDailyIncome = summary.dayBuckets.map {
                    StatisticsDisplayDailyTotal(date = it, amount = null)
                },
                displayedMonthlyExpenses = summary.monthBuckets.map {
                    StatisticsDisplayMonthlyTotal(year = it.year, month = it.month, label = it.label, amount = null)
                },
                displayedMonthlyIncome = summary.monthBuckets.map {
                    StatisticsDisplayMonthlyTotal(year = it.year, month = it.month, label = it.label, amount = null)
                },
            )
        }

        val displayCurrency = aggregateResolution.displayCurrency ?: normalizedBaseCurrency
        val displayMoney = MoneyDisplayFormatter.resolveAndFormat(displayCurrency)
        val displayTransactions = transactions.mapNotNull { transaction ->
            val sourceCurrency = accountCurrencyById[transaction.accountId] ?: return@mapNotNull null
            val displayAmount = when (aggregateResolution.displayMode) {
                AggregateCurrencyDisplayMode.CONVERTED -> convert(
                    amountMinor = transaction.amount,
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

        // Pre-group in a single pass each to avoid O(N×D) nested iteration.
        val expenseByDay = expenseTransactions.groupBy { startOfDay(it.transaction.date) }
            .mapValues { (_, txns) -> txns.sumOf(DisplayTransaction::amount) }
        val incomeByDay = incomeTransactions.groupBy { startOfDay(it.transaction.date) }
            .mapValues { (_, txns) -> txns.sumOf(DisplayTransaction::amount) }

        val cal = Calendar.getInstance(TimeZone.getDefault())
        val expenseByMonth = expenseTransactions.groupBy {
            cal.timeInMillis = it.transaction.date
            cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
        }.mapValues { (_, txns) -> txns.sumOf(DisplayTransaction::amount) }
        val incomeByMonth = incomeTransactions.groupBy {
            cal.timeInMillis = it.transaction.date
            cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
        }.mapValues { (_, txns) -> txns.sumOf(DisplayTransaction::amount) }

        return StatisticsCurrencyResolution(
            currencyUiState = StatisticsCurrencyUiState(
                moneyDisplay = displayMoney,
                displayMode = aggregateResolution.displayMode,
            ),
            displayedTotalExpenses = displayedTotalExpenses,
            displayedTotalIncome = displayedTotalIncome,
            displayedExpensesByCategory = buildCategoryItems(
                transactions = expenseTransactions,
                total = displayedTotalExpenses,
                categoryMetaById = categoryMetaById,
            ),
            displayedIncomesByCategory = buildCategoryItems(
                transactions = incomeTransactions,
                total = displayedTotalIncome,
                categoryMetaById = categoryMetaById,
            ),
            displayedDailyExpenses = summary.dayBuckets.map { date ->
                StatisticsDisplayDailyTotal(date = date, amount = expenseByDay[date] ?: 0L)
            },
            displayedDailyIncome = summary.dayBuckets.map { date ->
                StatisticsDisplayDailyTotal(date = date, amount = incomeByDay[date] ?: 0L)
            },
            displayedMonthlyExpenses = summary.monthBuckets.map { bucket ->
                StatisticsDisplayMonthlyTotal(
                    year = bucket.year,
                    month = bucket.month,
                    label = bucket.label,
                    amount = expenseByMonth[bucket.year to bucket.month] ?: 0L,
                )
            },
            displayedMonthlyIncome = summary.monthBuckets.map { bucket ->
                StatisticsDisplayMonthlyTotal(
                    year = bucket.year,
                    month = bucket.month,
                    label = bucket.label,
                    amount = incomeByMonth[bucket.year to bucket.month] ?: 0L,
                )
            },
        )
    }

    private fun buildCategoryItems(
        transactions: List<DisplayTransaction>,
        total: Long,
        categoryMetaById: Map<Long, CategoryMetadata>,
    ): List<StatisticsCategoryDisplayItem> {
        val amountByCategoryId = transactions
            .groupBy { it.transaction.categoryId }
            .mapValues { (_, items) -> items.sumOf(DisplayTransaction::amount) }
        val percentageByCategoryId = buildPercentages(amountByCategoryId, total)

        return amountByCategoryId.entries
            .sortedByDescending { it.value }
            .map { (categoryId, amount) ->
                StatisticsCategoryDisplayItem(
                    category = categoryMetadataFor(categoryId, categoryMetaById),
                    displayAmount = amount,
                    displayPercentage = percentageByCategoryId[categoryId] ?: 0,
                )
            }
    }

    private fun unavailableCategoryItems(
        transactions: List<Transaction>,
        categoryMetaById: Map<Long, CategoryMetadata>,
    ): List<StatisticsCategoryDisplayItem> =
        transactions
            .groupBy { it.categoryId }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .map { (categoryId, _) ->
                StatisticsCategoryDisplayItem(
                    category = categoryMetadataFor(categoryId, categoryMetaById),
                    displayAmount = null,
                    displayPercentage = 0,
                )
            }

    private fun categoryMetadataFor(
        categoryId: Long,
        categoryMetaById: Map<Long, CategoryMetadata>,
    ): CategoryMetadata = categoryMetaById[categoryId] ?: CategoryMetadata(
        categoryId = categoryId,
        categoryName = "",
        categoryIcon = "",
        categoryColor = 0xFF90A4AE,
    )

    private fun buildPercentages(
        amountByCategoryId: Map<Long, Long>,
        total: Long,
    ): Map<Long, Int> {
        if (total <= 0L || amountByCategoryId.isEmpty()) {
            return amountByCategoryId.keys.associateWith { 0 }
        }

        val totalDouble = total.toMajorDouble()
        val raw = amountByCategoryId.map { (id, amount) ->
            val percentage = amount.toMajorDouble() / totalDouble * 100.0
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
        amountMinor = 100L, // 1.00 in minor units
        sourceCurrency = currency,
        baseCurrency = baseCurrency,
        exchangeRate = exchangeRate,
    ) != null

    private fun convert(
        amountMinor: Long,
        sourceCurrency: String,
        baseCurrency: String,
        exchangeRate: ExchangeRate?,
    ): Long? {
        if (sourceCurrency == baseCurrency) return amountMinor
        val quotes = exchangeRate?.quotes ?: return null

        return runCatching {
            convertAmountUseCase(
                amountMinor = amountMinor,
                sourceCurrency = sourceCurrency,
                targetCurrency = baseCurrency,
                quotes = quotes,
            )
        }.getOrNull()
    }

    private data class DisplayTransaction(
        val transaction: Transaction,
        val amount: Long,
    )
}
