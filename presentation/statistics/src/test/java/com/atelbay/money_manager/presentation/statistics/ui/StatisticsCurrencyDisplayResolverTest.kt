package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayMode
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.usecase.ConvertAmountUseCase
import com.atelbay.money_manager.domain.statistics.model.CategoryMetadata
import com.atelbay.money_manager.domain.statistics.model.MonthBucket
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
                dayBuckets = listOf(bucketDate),
                categories = listOf(CategoryMetadata(1L, "Food", "restaurant", 0L)),
            ),
            transactions = listOf(
                transaction(amount = 4_750_000L, accountId = 1L, categoryId = 1L, date = bucketDate),
            ),
            accounts = listOf(account(id = 1L, currency = "KZT")),
            baseCurrency = "USD",
            exchangeRate = null,
        )

        assertEquals(AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY, resolution.currencyUiState.displayMode)
        assertEquals("₸", resolution.currencyUiState.moneyDisplay.primaryLabel)
        assertEquals(4_750_000L, resolution.displayedTotalExpenses ?: 0L)
        assertEquals(4_750_000L, resolution.displayedExpensesByCategory.single().displayAmount ?: 0L)
        assertEquals(4_750_000L, resolution.displayedDailyExpenses.single().amount ?: 0L)
    }

    @Test
    fun `fully convertible mixed scope converts into base currency`() {
        val bucketDate = dayStart(1_700_000_000_000L)
        val resolution = resolver.resolve(
            summary = summary(
                dayBuckets = listOf(bucketDate),
                categories = listOf(CategoryMetadata(1L, "Food", "restaurant", 0L)),
            ),
            transactions = listOf(
                transaction(amount = 520_000L, accountId = 1L, categoryId = 1L, date = bucketDate),
                transaction(id = 2L, amount = 5_200L, accountId = 2L, categoryId = 1L, date = bucketDate),
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
        // KZT 5200.00 → EUR: 520000/520 = 1000 minor units (10.00 EUR)
        // USD 52.00 → KZT 24700.00 → EUR: 5200*475/520 = 4750 minor units (47.50 EUR)
        // Total: 5750 minor units (57.50 EUR)
        assertEquals(5750L, resolution.displayedTotalExpenses ?: 0L)
        assertEquals(5750L, resolution.displayedExpensesByCategory.single().displayAmount ?: 0L)
        assertEquals(5750L, resolution.displayedDailyExpenses.single().amount ?: 0L)
    }

    @Test
    fun `mixed scope without required quotes reports unavailable`() {
        val bucketDate = dayStart(1_700_000_000_000L)
        val resolution = resolver.resolve(
            summary = summary(
                dayBuckets = listOf(bucketDate),
                categories = listOf(CategoryMetadata(1L, "Food", "restaurant", 0L)),
            ),
            transactions = listOf(
                transaction(amount = 4_750_000L, accountId = 1L, categoryId = 1L, date = bucketDate),
                transaction(id = 2L, amount = 10_000L, accountId = 2L, categoryId = 1L, date = bucketDate),
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

    @Test
    fun `year monthly buckets aggregate converted amounts per month`() {
        val janDate = midMonth(2025, java.util.Calendar.JANUARY)
        val febDate = midMonth(2025, java.util.Calendar.FEBRUARY)
        val resolution = resolver.resolve(
            summary = summary(
                dayBuckets = emptyList(),
                categories = listOf(CategoryMetadata(1L, "Food", "restaurant", 0L)),
                monthBuckets = listOf(
                    MonthBucket(2025, java.util.Calendar.JANUARY, "Jan"),
                    MonthBucket(2025, java.util.Calendar.FEBRUARY, "Feb"),
                    MonthBucket(2025, java.util.Calendar.MARCH, "Mar"),
                ),
            ),
            transactions = listOf(
                transaction(amount = 475_000L, accountId = 1L, categoryId = 1L, date = janDate),
                transaction(id = 2L, amount = 950_000L, accountId = 1L, categoryId = 1L, date = febDate),
            ),
            accounts = listOf(account(id = 1L, currency = "KZT")),
            baseCurrency = "USD",
            exchangeRate = ExchangeRate(
                quotes = mapOf("KZT" to 1.0, "USD" to 475.0),
                fetchedAt = 1L,
                source = "NBK",
            ),
        )

        assertEquals(AggregateCurrencyDisplayMode.CONVERTED, resolution.currencyUiState.displayMode)
        assertEquals(3, resolution.displayedMonthlyExpenses.size)
        // KZT 4750.00 → USD 10.00 = 1000 minor units; KZT 9500.00 → USD 20.00 = 2000 minor units.
        assertEquals(1000L, resolution.displayedMonthlyExpenses[0].amount ?: -1L)
        assertEquals(2000L, resolution.displayedMonthlyExpenses[1].amount ?: -1L)
        // March has no transactions → zero-filled.
        assertEquals(0L, resolution.displayedMonthlyExpenses[2].amount ?: -1L)
        assertEquals(3000L, resolution.displayedTotalExpenses ?: -1L)
    }

    private fun summary(
        dayBuckets: List<Long>,
        categories: List<CategoryMetadata>,
        monthBuckets: List<MonthBucket> = emptyList(),
    ) = PeriodSummary(
        dateRange = StatisticsDateRange(startMillis = 1L, endMillis = 1L),
        dayBuckets = dayBuckets,
        monthBuckets = monthBuckets,
        categories = categories,
    )

    private fun midMonth(year: Int, month: Int): Long =
        java.util.Calendar.getInstance(java.util.TimeZone.getDefault()).run {
            clear()
            set(year, month, 15, 12, 0, 0)
            timeInMillis
        }

    private fun transaction(
        id: Long = 1L,
        amount: Long,
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
        balance = 0L,
        createdAt = 1L,
    )
}
