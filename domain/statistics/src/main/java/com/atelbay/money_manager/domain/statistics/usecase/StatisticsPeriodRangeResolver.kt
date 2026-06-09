package com.atelbay.money_manager.domain.statistics.usecase

import com.atelbay.money_manager.domain.statistics.model.StatisticsDateRange
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * Resolves the [StatisticsDateRange] for a given [StatsPeriod] and optional anchor instant.
 *
 * Semantics:
 * - **WEEK** — rolling 7-day window **ending on the anchor day** (anchor day inclusive).
 *   With a null anchor this is "the last 7 days up to today".
 * - **MONTH** — the **full calendar month** that contains the anchor (1st 00:00 … last day 23:59).
 *   This makes an explicit month selection show *that* month rather than a rolling window.
 * - **YEAR** — the **12 calendar months ending with the anchor's month** (1st of month−11 … last day
 *   of the anchor month).
 *
 * For MONTH/YEAR the end is clamped to the end of *today*, so the current (in-progress) month never
 * renders a long tail of empty future days/bars. Selecting a fully elapsed past month yields the
 * whole month.
 */
class StatisticsPeriodRangeResolver @Inject constructor() {

    operator fun invoke(period: StatsPeriod, anchorMillis: Long? = null): StatisticsDateRange {
        val timeZone = TimeZone.getDefault()
        val anchor = Calendar.getInstance(timeZone)
        if (anchorMillis != null) anchor.timeInMillis = anchorMillis

        val todayEnd = Calendar.getInstance(timeZone).apply { endOfDay() }.timeInMillis

        return when (period) {
            StatsPeriod.WEEK -> {
                val end = (anchor.clone() as Calendar).apply { endOfDay() }.timeInMillis
                val start = (anchor.clone() as Calendar).apply {
                    startOfDay()
                    add(Calendar.DAY_OF_YEAR, -6)
                }.timeInMillis
                StatisticsDateRange(startMillis = start, endMillis = minOf(end, todayEnd))
            }

            StatsPeriod.MONTH -> {
                val start = (anchor.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    startOfDay()
                }.timeInMillis
                val monthEnd = (anchor.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    endOfDay()
                }.timeInMillis
                StatisticsDateRange(startMillis = start, endMillis = minOf(monthEnd, todayEnd))
            }

            StatsPeriod.YEAR -> {
                val start = (anchor.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    startOfDay()
                    add(Calendar.MONTH, -11)
                }.timeInMillis
                val monthEnd = (anchor.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    endOfDay()
                }.timeInMillis
                StatisticsDateRange(startMillis = start, endMillis = minOf(monthEnd, todayEnd))
            }
        }
    }

    private fun Calendar.startOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun Calendar.endOfDay() {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
}
