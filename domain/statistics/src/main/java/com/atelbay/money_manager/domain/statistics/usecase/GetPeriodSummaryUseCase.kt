package com.atelbay.money_manager.domain.statistics.usecase

import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.database.entity.TransactionEntity
import com.atelbay.money_manager.domain.statistics.model.CategorySummary
import com.atelbay.money_manager.domain.statistics.model.DailyTotal
import com.atelbay.money_manager.domain.statistics.model.MonthlyTotal
import com.atelbay.money_manager.domain.statistics.model.PeriodSummary
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class GetPeriodSummaryUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
) {
    operator fun invoke(period: StatsPeriod): Flow<PeriodSummary> {
        val (start, end) = periodRange(period)
        return combine(
            transactionDao.observeByDateRange(start, end),
            categoryDao.observeAll(),
        ) { transactions, categories ->
            val categoryMap = categories.associateBy { it.id }

            val expenses = transactions.filter { it.type == "expense" }
            val incomes = transactions.filter { it.type == "income" }

            val totalExpenses = expenses.sumOf { it.amount }
            val totalIncome = incomes.sumOf { it.amount }

            // --- Category summaries with largest-remainder rounding (T023, T024) ---
            val expensesByCategory = buildCategorySummaries(expenses, categoryMap, totalExpenses)
            val incomesByCategory = buildCategorySummaries(incomes, categoryMap, totalIncome)

            // --- Daily totals with zero-fill (T004) ---
            val expenseDayMap = expenses.groupBy { dayStart(it.date) }
                .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            val incomeDayMap = incomes.groupBy { dayStart(it.date) }
                .mapValues { (_, txns) -> txns.sumOf { it.amount } }

            val dailyExpenses = fillDays(start, end, expenseDayMap)
            val dailyIncome = fillDays(start, end, incomeDayMap)

            // --- Monthly totals with zero-fill for YEAR period (T005) ---
            val monthlyExpenses = if (period == StatsPeriod.YEAR) {
                buildMonthlyTotals(start, end, expenses)
            } else {
                emptyList()
            }
            val monthlyIncome = if (period == StatsPeriod.YEAR) {
                buildMonthlyTotals(start, end, incomes)
            } else {
                emptyList()
            }

            PeriodSummary(
                totalExpenses = totalExpenses,
                totalIncome = totalIncome,
                expensesByCategory = expensesByCategory,
                incomesByCategory = incomesByCategory,
                dailyExpenses = dailyExpenses,
                dailyIncome = dailyIncome,
                monthlyExpenses = monthlyExpenses,
                monthlyIncome = monthlyIncome,
            )
        }
    }

    // T003: Fixed periodRange — set start to 00:00 first, then subtract fixed day counts
    private fun periodRange(period: StatsPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getDefault())

        // End of today
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        // Reset to start of today, then subtract
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        when (period) {
            StatsPeriod.WEEK -> cal.add(Calendar.DAY_OF_YEAR, -6)
            StatsPeriod.MONTH -> cal.add(Calendar.DAY_OF_YEAR, -29)
            StatsPeriod.YEAR -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)  // snap to 1st of current month
                cal.add(Calendar.MONTH, -11)        // go back 11 months → exactly 12 buckets
            }
        }
        val start = cal.timeInMillis

        return start to end
    }

    // T004: Fill every day from start to end with zero-default
    private fun fillDays(start: Long, end: Long, dayMap: Map<Long, Double>): List<DailyTotal> {
        val result = mutableListOf<DailyTotal>()
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = start
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val endDay = dayStart(end)
        while (cal.timeInMillis <= endDay) {
            val day = cal.timeInMillis
            result += DailyTotal(date = day, amount = dayMap[day] ?: 0.0)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    // T005: Monthly aggregation for YEAR period with zero-fill
    private fun buildMonthlyTotals(
        start: Long,
        end: Long,
        transactions: List<TransactionEntity>,
    ): List<MonthlyTotal> {
        val sdf = SimpleDateFormat("MMM", Locale.getDefault())

        // Group transactions by year+month
        val monthMap = mutableMapOf<Pair<Int, Int>, Double>()
        for (tx in transactions) {
            val cal = Calendar.getInstance(TimeZone.getDefault())
            cal.timeInMillis = tx.date
            val key = cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
            monthMap[key] = (monthMap[key] ?: 0.0) + tx.amount
        }

        // Iterate every month from start to end
        val result = mutableListOf<MonthlyTotal>()
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = start
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val endCal = Calendar.getInstance(TimeZone.getDefault())
        endCal.timeInMillis = end

        while (cal.get(Calendar.YEAR) < endCal.get(Calendar.YEAR) ||
            (cal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) <= endCal.get(Calendar.MONTH))
        ) {
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val key = year to month
            result += MonthlyTotal(
                year = year,
                month = month,
                amount = monthMap[key] ?: 0.0,
                label = sdf.format(cal.time),
            )
            cal.add(Calendar.MONTH, 1)
        }
        return result
    }

    // T014, T024: Build category summaries with largest-remainder rounding
    private fun buildCategorySummaries(
        transactions: List<TransactionEntity>,
        categoryMap: Map<Long, CategoryEntity>,
        total: Double,
    ): List<CategorySummary> {
        val grouped = transactions.groupBy { it.categoryId }
        val items = grouped.map { (catId, txns) -> catId to txns.sumOf { it.amount } }
        val percentages = largestRemainderRound(items, total)

        return grouped.map { (catId, txns) ->
            val cat = categoryMap[catId]
            CategorySummary(
                categoryId = catId,
                categoryName = cat?.name.orEmpty(),
                categoryIcon = cat?.icon.orEmpty(),
                categoryColor = cat?.color ?: 0xFF90A4AE,
                totalAmount = txns.sumOf { it.amount },
                percentage = percentages[catId] ?: 0,
            )
        }.sortedByDescending { it.totalAmount }
    }

    // T023: Largest-remainder rounding with minimum 1% guarantee for non-zero items
    private fun largestRemainderRound(
        items: List<Pair<Long, Double>>,
        total: Double,
    ): Map<Long, Int> {
        if (total <= 0.0 || items.isEmpty()) {
            return items.associate { it.first to 0 }
        }

        // Compute raw percentages
        val raw = items.map { (id, amount) ->
            val pct = amount / total * 100.0
            val floored = kotlin.math.floor(pct).toInt()
            Triple(id, floored, pct - floored)
        }

        val floors = raw.associate { (id, floored, _) -> id to floored }.toMutableMap()
        var deficit = 100 - floors.values.sum()

        // Distribute deficit by largest fractional remainder
        val sorted = raw.sortedByDescending { it.third }
        for ((id, _, _) in sorted) {
            if (deficit <= 0) break
            floors[id] = floors[id]!! + 1
            deficit--
        }

        // Guarantee minimum 1% for any non-zero item, steal from largest
        val nonZeroIds = items.filter { it.second > 0.0 }.map { it.first }
        for (id in nonZeroIds) {
            if (floors[id]!! < 1) {
                val donor = floors.entries
                    .filter { it.key != id && it.value > 1 }
                    .maxByOrNull { it.value }
                if (donor != null) {
                    floors[id] = 1
                    floors[donor.key] = donor.value - 1
                }
                // If no donor, leave as-is (total integrity > UI guarantee)
            }
        }

        return floors
    }

    private fun dayStart(timestamp: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
