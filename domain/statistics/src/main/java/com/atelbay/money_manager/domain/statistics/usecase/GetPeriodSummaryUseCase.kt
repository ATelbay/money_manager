package com.atelbay.money_manager.domain.statistics.usecase

import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.domain.statistics.model.CategoryMetadata
import com.atelbay.money_manager.domain.statistics.model.MonthBucket
import com.atelbay.money_manager.domain.statistics.model.PeriodSummary
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * Builds the [PeriodSummary] *skeleton* for a period: the resolved date range, the ordered
 * zero-filled day/month buckets, and the category catalog. It deliberately does **not** read
 * transactions or compute any amounts — the single, currency-aware aggregation happens in the
 * presentation layer's `StatisticsCurrencyDisplayResolver`, from one transaction stream. This
 * eliminates the previous double read of the transactions table and the duplicated aggregation.
 */
class GetPeriodSummaryUseCase @Inject constructor(
    private val categoryDao: CategoryDao,
    private val rangeResolver: StatisticsPeriodRangeResolver,
) {
    operator fun invoke(period: StatsPeriod, anchorMillis: Long? = null): Flow<PeriodSummary> {
        val dateRange = rangeResolver(period, anchorMillis)
        val start = dateRange.startMillis
        val end = dateRange.endMillis

        // YEAR renders monthly buckets only; every other period renders the daily series. The
        // skeletons depend solely on the range, so they are computed once here, not per emission.
        val dayBuckets = if (period != StatsPeriod.YEAR) buildDayBuckets(start, end) else emptyList()
        val monthBuckets = if (period == StatsPeriod.YEAR) buildMonthBuckets(start, end) else emptyList()

        return categoryDao.observeAll().map { categories ->
            PeriodSummary(
                dateRange = dateRange,
                dayBuckets = dayBuckets,
                monthBuckets = monthBuckets,
                categories = categories.map { category ->
                    CategoryMetadata(
                        categoryId = category.id,
                        categoryName = category.name,
                        categoryIcon = category.icon,
                        categoryColor = category.color,
                    )
                },
            )
        }
    }

    // Every day from start to end (inclusive), as day-start millis.
    private fun buildDayBuckets(start: Long, end: Long): List<Long> {
        val result = mutableListOf<Long>()
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = start
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val endDay = dayStart(end)
        while (cal.timeInMillis <= endDay) {
            result += cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    // Every month from start to end (inclusive), with a pre-formatted short label.
    private fun buildMonthBuckets(start: Long, end: Long): List<MonthBucket> {
        val sdf = SimpleDateFormat("MMM", Locale.getDefault())
        val result = mutableListOf<MonthBucket>()

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
            result += MonthBucket(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH),
                label = sdf.format(cal.time),
            )
            cal.add(Calendar.MONTH, 1)
        }
        return result
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
