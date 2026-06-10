package com.atelbay.money_manager.domain.recurring.usecase

import com.atelbay.money_manager.core.model.Frequency
import com.atelbay.money_manager.core.model.RecurringTransaction
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.domain.recurring.repository.RecurringTransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Date-math tests for recurring generation. Each recurring uses an `endDate` in the past so the
 * generated window is bounded by `endDate` (not `today`) and the assertions stay deterministic.
 */
class GeneratePendingTransactionsUseCaseTest {

    private val repository = mockk<RecurringTransactionRepository>(relaxed = true)
    private val useCase = GeneratePendingTransactionsUseCase(repository)

    @Test
    fun `monthly clamps end-of-month and recovers target day`() = runTest {
        // Jan 31 start, dayOfMonth 31, through Apr 30 2020 (leap year).
        val captured = generate(
            recurring(Frequency.MONTHLY, start = date(2020, 1, 31), end = date(2020, 4, 30), dayOfMonth = 31),
        )
        assertEquals(
            listOf(
                LocalDate.of(2020, 1, 31),
                LocalDate.of(2020, 2, 29), // clamped to leap February
                LocalDate.of(2020, 3, 31), // recovered to 31 (not stuck at 29)
                LocalDate.of(2020, 4, 30), // clamped to 30
            ),
            captured.localDates(),
        )
    }

    @Test
    fun `weekly lands on the configured day of week`() = runTest {
        // Start Mon Jan 6 2020, target Wednesday (3), through Jan 20.
        val captured = generate(
            recurring(Frequency.WEEKLY, start = date(2020, 1, 6), end = date(2020, 1, 20), dayOfWeek = 3),
        )
        assertEquals(
            listOf(LocalDate.of(2020, 1, 8), LocalDate.of(2020, 1, 15)),
            captured.localDates(),
        )
    }

    @Test
    fun `yearly preserves Feb 29 across leap years`() = runTest {
        val captured = generate(
            recurring(Frequency.YEARLY, start = date(2020, 2, 29), end = date(2024, 3, 1)),
        )
        assertEquals(
            listOf(
                LocalDate.of(2020, 2, 29),
                LocalDate.of(2021, 2, 28),
                LocalDate.of(2022, 2, 28),
                LocalDate.of(2023, 2, 28),
                LocalDate.of(2024, 2, 29), // restored on the next leap year (no permanent drift)
            ),
            captured.localDates(),
        )
    }

    @Test
    fun `first monthly occurrence honours dayOfMonth instead of the start date`() = runTest {
        // Start Jan 15 but dayOfMonth 1 → first occurrence is Feb 1 (Jan 1 is before the start).
        val captured = generate(
            recurring(Frequency.MONTHLY, start = date(2020, 1, 15), end = date(2020, 3, 2), dayOfMonth = 1),
        )
        assertEquals(
            listOf(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 3, 1)),
            captured.localDates(),
        )
    }

    @Test
    fun `daily catches up every missed day`() = runTest {
        val captured = generate(
            recurring(Frequency.DAILY, start = date(2020, 1, 1), end = date(2020, 1, 5)),
        )
        assertEquals(5, captured.size)
        assertEquals(LocalDate.of(2020, 1, 1), captured.localDates().first())
        assertEquals(LocalDate.of(2020, 1, 5), captured.localDates().last())
    }

    @Test
    fun `expired recurring with nothing pending is deactivated and never generates`() = runTest {
        val end = date(2020, 1, 5)
        val rec = recurring(
            Frequency.DAILY,
            start = date(2020, 1, 1),
            end = end,
            lastGeneratedDate = end, // already caught up to the end
        )
        coEvery { repository.getActiveRecurrings() } returns listOf(rec)

        useCase()

        coVerify(exactly = 0) { repository.generateTransactionsAtomically(any(), any(), any()) }
        coVerify(exactly = 1) { repository.toggleActive(rec.id, false) }
    }

    // ── helpers ──

    private suspend fun generate(rec: RecurringTransaction): List<Transaction> {
        coEvery { repository.getActiveRecurrings() } returns listOf(rec)
        val slot = slot<List<Transaction>>()
        coEvery {
            repository.generateTransactionsAtomically(any(), capture(slot), any())
        } returns Unit
        useCase()
        return if (slot.isCaptured) slot.captured else emptyList()
    }

    private fun List<Transaction>.localDates(): List<LocalDate> =
        map { Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() }

    private fun date(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun recurring(
        frequency: Frequency,
        start: Long,
        end: Long,
        dayOfMonth: Int? = null,
        dayOfWeek: Int? = null,
        lastGeneratedDate: Long? = null,
    ) = RecurringTransaction(
        id = 1,
        amount = 10_000L,
        type = TransactionType.EXPENSE,
        categoryId = 1,
        categoryName = "Cat",
        categoryIcon = "icon",
        categoryColor = 0L,
        accountId = 1,
        accountName = "Acc",
        note = null,
        frequency = frequency,
        startDate = start,
        endDate = end,
        dayOfMonth = dayOfMonth,
        dayOfWeek = dayOfWeek,
        lastGeneratedDate = lastGeneratedDate,
        isActive = true,
        createdAt = 0L,
    )
}
