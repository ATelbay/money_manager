package com.atelbay.money_manager.domain.budgets.usecase

import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.model.Budget
import com.atelbay.money_manager.domain.budgets.repository.BudgetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetBudgetsWithSpendingUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionDao: TransactionDao,
) {
    operator fun invoke(): Flow<List<Budget>> =
        budgetRepository.observeAll().flatMapLatest { budgets ->
            if (budgets.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                val (monthStart, monthEnd) = currentMonthRange()
                val spendingFlows = budgets.map { budget ->
                    transactionDao.observeExpenseSumByCategory(
                        categoryId = budget.categoryId,
                        startDate = monthStart,
                        endDate = monthEnd,
                    )
                }
                combine(spendingFlows) { spendingArray ->
                    budgets.mapIndexed { index, budget ->
                        val spent = spendingArray[index]
                        val remaining = (budget.monthlyLimit - spent).coerceAtLeast(0.0)
                        val percentage = if (budget.monthlyLimit > 0) {
                            (spent / budget.monthlyLimit).toFloat().coerceAtLeast(0f)
                        } else {
                            0f
                        }
                        budget.copy(
                            spent = spent,
                            remaining = remaining,
                            percentage = percentage,
                        )
                    }.sortedByDescending { it.percentage }
                }
            }
        }

    private fun currentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis

        return monthStart to monthEnd
    }
}
