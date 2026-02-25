package com.atelbay.money_manager.domain.statistics.usecase

import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.domain.statistics.model.CategorySummary
import com.atelbay.money_manager.domain.statistics.model.DailyTotal
import com.atelbay.money_manager.domain.statistics.model.PeriodSummary
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
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

            val expensesByCategory = expenses
                .groupBy { it.categoryId }
                .map { (catId, txns) ->
                    val cat = categoryMap[catId]
                    val total = txns.sumOf { it.amount }
                    CategorySummary(
                        categoryId = catId,
                        categoryName = cat?.name.orEmpty(),
                        categoryIcon = cat?.icon.orEmpty(),
                        categoryColor = cat?.color ?: 0xFF90A4AE,
                        totalAmount = total,
                        percentage = if (totalExpenses > 0) (total / totalExpenses * 100).toFloat() else 0f,
                    )
                }
                .sortedByDescending { it.totalAmount }

            val dailyExpenses = expenses
                .groupBy { dayStart(it.date) }
                .map { (day, txns) ->
                    DailyTotal(date = day, amount = txns.sumOf { it.amount })
                }
                .sortedBy { it.date }

            PeriodSummary(
                totalExpenses = totalExpenses,
                totalIncome = totalIncome,
                expensesByCategory = expensesByCategory,
                dailyExpenses = dailyExpenses,
            )
        }
    }

    private fun periodRange(period: StatsPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        // End of today
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        // Start
        when (period) {
            StatsPeriod.WEEK -> cal.add(Calendar.DAY_OF_YEAR, -6)
            StatsPeriod.MONTH -> cal.add(Calendar.MONTH, -1)
            StatsPeriod.YEAR -> cal.add(Calendar.YEAR, -1)
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        return start to end
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
