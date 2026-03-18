package com.atelbay.money_manager.domain.statistics.usecase

import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StatisticsPeriodRangeResolverTest {

    private val resolver = StatisticsPeriodRangeResolver()

    // Build a fixed Calendar at noon on 2024-06-15 to use as anchor
    private fun anchorCalendar(): Calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
        set(2024, Calendar.JUNE, 15, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private val anchorMillis = anchorCalendar().timeInMillis

    @Test
    fun `WEEK with anchorMillis - end is end-of-anchor-day`() {
        val range = resolver(StatsPeriod.WEEK, anchorMillis)

        val expectedEnd = anchorCalendar().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        assertEquals(expectedEnd, range.endMillis)
    }

    @Test
    fun `WEEK with anchorMillis - start is 6 days before anchor day`() {
        val range = resolver(StatsPeriod.WEEK, anchorMillis)

        val expectedStart = anchorCalendar().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }.timeInMillis

        assertEquals(expectedStart, range.startMillis)
    }

    @Test
    fun `MONTH with anchorMillis - start is 1 month before anchor day`() {
        val range = resolver(StatsPeriod.MONTH, anchorMillis)

        val expectedStart = anchorCalendar().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -1)
        }.timeInMillis

        assertEquals(expectedStart, range.startMillis)
    }

    @Test
    fun `YEAR with anchorMillis - start is 11 months before first of anchor month`() {
        val range = resolver(StatsPeriod.YEAR, anchorMillis)

        val expectedStart = anchorCalendar().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -11)
        }.timeInMillis

        assertEquals(expectedStart, range.startMillis)
    }

    @Test
    fun `YEAR with anchorMillis - end is end-of-anchor-day`() {
        val range = resolver(StatsPeriod.YEAR, anchorMillis)

        val expectedEnd = anchorCalendar().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        assertEquals(expectedEnd, range.endMillis)
    }

    @Test
    fun `null anchorMillis uses current time - end is today end-of-day`() {
        val before = System.currentTimeMillis()
        val range = resolver(StatsPeriod.MONTH, null)
        val after = System.currentTimeMillis()

        // end must be after "before" (end-of-today is always >= now)
        assert(range.endMillis >= before) {
            "endMillis ${range.endMillis} should be >= $before"
        }
        // end must not be in the future beyond end-of-today
        val todayEndCal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = after
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        assert(range.endMillis <= todayEndCal.timeInMillis) {
            "endMillis ${range.endMillis} should be <= ${todayEndCal.timeInMillis}"
        }
    }

    @Test
    fun `null anchorMillis - startMillis is before endMillis`() {
        val range = resolver(StatsPeriod.WEEK, null)
        assert(range.startMillis < range.endMillis) {
            "startMillis should be before endMillis"
        }
    }

    @Test
    fun `anchorMillis range - startMillis is before endMillis for all periods`() {
        for (period in StatsPeriod.entries) {
            val range = resolver(period, anchorMillis)
            assert(range.startMillis < range.endMillis) {
                "$period: startMillis should be before endMillis"
            }
        }
    }
}
