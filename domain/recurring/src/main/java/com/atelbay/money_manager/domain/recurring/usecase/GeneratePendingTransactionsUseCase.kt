package com.atelbay.money_manager.domain.recurring.usecase

import com.atelbay.money_manager.core.model.Frequency
import com.atelbay.money_manager.core.model.RecurringTransaction
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.domain.recurring.repository.RecurringTransactionRepository
import com.atelbay.money_manager.domain.transactions.repository.TransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

class GeneratePendingTransactionsUseCase @Inject constructor(
    private val recurringRepository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke() {
        val today = LocalDate.now()
        val activeRecurrings = recurringRepository.getActiveRecurrings()

        for (recurring in activeRecurrings) {
            generateForRecurring(recurring, today)
        }
    }

    private suspend fun generateForRecurring(recurring: RecurringTransaction, today: LocalDate) {
        val startMillis = recurring.lastGeneratedDate ?: recurring.startDate
        val fromDate = Instant.ofEpochMilli(startMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val missedDates = computeMissedDates(recurring, fromDate, today)
        if (missedDates.isEmpty()) {
            // Deactivate if endDate has passed
            val endDate = recurring.endDate
            if (endDate != null && today.isAfter(
                    Instant.ofEpochMilli(endDate).atZone(ZoneId.systemDefault()).toLocalDate()
                )
            ) {
                recurringRepository.toggleActive(recurring.id, false)
            }
            return
        }

        val now = System.currentTimeMillis()
        for (date in missedDates) {
            val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            transactionRepository.save(
                Transaction(
                    id = 0,
                    amount = recurring.amount,
                    type = recurring.type,
                    categoryId = recurring.categoryId,
                    categoryName = recurring.categoryName,
                    categoryIcon = recurring.categoryIcon,
                    categoryColor = recurring.categoryColor,
                    accountId = recurring.accountId,
                    note = recurring.note,
                    date = dateMillis,
                    createdAt = now,
                )
            )
        }

        val lastDate = missedDates.last()
        val lastDateMillis = lastDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        recurringRepository.updateLastGeneratedDate(recurring.id, lastDateMillis)

        // Deactivate if endDate has passed
        val endDate = recurring.endDate
        if (endDate != null && today.isAfter(
                Instant.ofEpochMilli(endDate).atZone(ZoneId.systemDefault()).toLocalDate()
            )
        ) {
            recurringRepository.toggleActive(recurring.id, false)
        }
    }

    private fun computeMissedDates(
        recurring: RecurringTransaction,
        fromDate: LocalDate,
        today: LocalDate,
    ): List<LocalDate> {
        val endDateLocal = recurring.endDate?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        val effectiveEnd = if (endDateLocal != null && endDateLocal.isBefore(today)) endDateLocal else today

        val dates = mutableListOf<LocalDate>()

        // When never generated, include startDate itself if it is <= today
        var candidate: LocalDate? = if (recurring.lastGeneratedDate == null) {
            fromDate
        } else {
            nextOccurrence(recurring, fromDate)
        }

        while (candidate != null && !candidate.isAfter(effectiveEnd)) {
            dates.add(candidate)
            candidate = nextOccurrence(recurring, candidate)
        }

        return dates
    }

    private fun nextOccurrence(recurring: RecurringTransaction, after: LocalDate): LocalDate? {
        return when (recurring.frequency) {
            Frequency.DAILY -> after.plusDays(1)
            Frequency.WEEKLY -> {
                val targetDow = recurring.dayOfWeek ?: 1
                var next = after.plusDays(1)
                while (next.dayOfWeek.value != targetDow) {
                    next = next.plusDays(1)
                }
                next
            }
            Frequency.MONTHLY -> {
                val targetDay = recurring.dayOfMonth ?: 1
                val nextMonth = after.plusMonths(1)
                val yearMonth = YearMonth.of(nextMonth.year, nextMonth.month)
                val clampedDay = minOf(targetDay, yearMonth.lengthOfMonth())
                LocalDate.of(nextMonth.year, nextMonth.month, clampedDay)
            }
            Frequency.YEARLY -> after.plusYears(1)
        }
    }
}
