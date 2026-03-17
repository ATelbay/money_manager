package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayMode
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.usecase.ConvertAmountUseCase
import com.atelbay.money_manager.domain.statistics.model.CategorySummary
import com.atelbay.money_manager.domain.statistics.model.DailyTotal
import com.atelbay.money_manager.domain.statistics.model.PeriodSummary
import com.atelbay.money_manager.domain.statistics.model.StatisticsDateRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatisticsCurrencyDisplayResolverTest {

    private val resolver = StatisticsCurrencyDisplayResolver(ConvertAmountUseCase())

    @Test
    fun `single currency scope keeps original currency display`() {
        val bucketDate = dayStart(1_700_000_000_000L)
        val resolution = resolver.resolve(
            summary = summary(
                totalExpenses = 47_500.0,
                dailyExpenses = listOf(DailyTotal(date = bucketDate, amount = 47_500.0)),
                expensesByCategory = listOf(
                    CategorySummary(1L, "Food", "restaurant", 0L, 47_500.0, 100),
                ),
            ),
            transactions = listOf(
                transaction(amount = 47_500.0, accountId = 1L, categoryId = 1L, date = bucketDate),
            ),
            accounts = listOf(account(id = 1L, currency = "KZT")),
            baseCurrency = "USD",
            exchangeRate = null,
        )

        assertEquals(AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY, resolution.currencyUiState.displayMode)
        assertEquals("₸", resolution.currencyUiState.moneyDisplay.primaryLabel)
        assertEquals(47_500.0, resolution.displayedTotalExpenses ?: 0.0, 0.0)
        assertEquals(47_500.0, resolution.displayedExpensesByCategory.single().displayAmount ?: 0.0, 0.0)
        assertEquals(47_500.0, resolution.displayedDailyExpenses.single().amount ?: 0.0, 0.0)
    }

    @Test
    fun `fully convertible mixed scope converts into base currency`() {
        val bucketDate = dayStart(1_700_000_000_000L)
        val resolution = resolver.resolve(
            summary = summary(
                totalExpenses = 5_252.0,
                dailyExpenses = listOf(DailyTotal(date = bucketDate, amount = 5_252.0)),
                expensesByCategory = listOf(
                    CategorySummary(1L, "Food", "restaurant", 0L, 5_252.0, 100),
                ),
            ),
            transactions = listOf(
                transaction(amount = 5_200.0, accountId = 1L, categoryId = 1L, date = bucketDate),
                transaction(id = 2L, amount = 52.0, accountId = 2L, categoryId = 1L, date = bucketDate),
            ),
            accounts = listOf(
                account(id = 1L, currency = "KZT"),
                account(id = 2L, currency = "USD"),
            ),
            baseCurrency = "EUR",
            exchangeRate = ExchangeRate(
                quotes = mapOf(
                    "KZT" to 1.0,
                    "USD" to 475.0,
                    "EUR" to 520.0,
                ),
                fetchedAt = 1L,
                source = "NBK",
            ),
        )

        assertEquals(AggregateCurrencyDisplayMode.CONVERTED, resolution.currencyUiState.displayMode)
        assertEquals("€", resolution.currencyUiState.moneyDisplay.primaryLabel)
        assertEquals(57.5, resolution.displayedTotalExpenses ?: 0.0, 0.0)
        assertEquals(57.5, resolution.displayedExpensesByCategory.single().displayAmount ?: 0.0, 0.0)
        assertEquals(57.5, resolution.displayedDailyExpenses.single().amount ?: 0.0, 0.0)
    }

    @Test
    fun `mixed scope without required quotes reports unavailable`() {
        val bucketDate = dayStart(1_700_000_000_000L)
        val resolution = resolver.resolve(
            summary = summary(
                totalExpenses = 47_600.0,
                dailyExpenses = listOf(DailyTotal(date = bucketDate, amount = 47_600.0)),
                expensesByCategory = listOf(
                    CategorySummary(1L, "Food", "restaurant", 0L, 47_600.0, 100),
                ),
            ),
            transactions = listOf(
                transaction(amount = 47_500.0, accountId = 1L, categoryId = 1L, date = bucketDate),
                transaction(id = 2L, amount = 100.0, accountId = 2L, categoryId = 1L, date = bucketDate),
            ),
            accounts = listOf(
                account(id = 1L, currency = "KZT"),
                account(id = 2L, currency = "GBP"),
            ),
            baseCurrency = "USD",
            exchangeRate = ExchangeRate(
                quotes = mapOf(
                    "KZT" to 1.0,
                    "USD" to 475.0,
                ),
                fetchedAt = 1L,
                source = "NBK",
            ),
        )

        assertEquals(AggregateCurrencyDisplayMode.UNAVAILABLE, resolution.currencyUiState.displayMode)
        assertNull(resolution.displayedTotalExpenses)
        assertNull(resolution.displayedExpensesByCategory.single().displayAmount)
        assertNull(resolution.displayedDailyExpenses.single().amount)
    }

    private fun summary(
        totalExpenses: Double,
        dailyExpenses: List<DailyTotal>,
        expensesByCategory: List<CategorySummary>,
    ) = PeriodSummary(
        dateRange = StatisticsDateRange(startMillis = 1L, endMillis = 1L),
        totalExpenses = totalExpenses,
        totalIncome = 0.0,
        expensesByCategory = expensesByCategory,
        incomesByCategory = emptyList(),
        dailyExpenses = dailyExpenses,
        dailyIncome = emptyList(),
        monthlyExpenses = emptyList(),
        monthlyIncome = emptyList(),
    )

    private fun transaction(
        id: Long = 1L,
        amount: Double,
        accountId: Long,
        categoryId: Long,
        date: Long = 1L,
    ) = Transaction(
        id = id,
        amount = amount,
        type = TransactionType.EXPENSE,
        categoryId = categoryId,
        categoryName = "Food",
        categoryIcon = "restaurant",
        categoryColor = 0L,
        accountId = accountId,
        note = null,
        date = date,
        createdAt = 1L,
    )

    private fun dayStart(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getDefault())
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun account(
        id: Long,
        currency: String,
    ) = Account(
        id = id,
        name = "Cash",
        currency = currency,
        balance = 0.0,
        createdAt = 1L,
    )
}
