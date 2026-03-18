package com.atelbay.money_manager.domain.statistics.usecase

import com.atelbay.money_manager.domain.statistics.model.StatisticsDateRange
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

class StatisticsPeriodRangeResolver @Inject constructor() {

    operator fun invoke(period: StatsPeriod, anchorMillis: Long? = null): StatisticsDateRange {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        if (anchorMillis != null) calendar.timeInMillis = anchorMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        when (period) {
            StatsPeriod.WEEK -> calendar.add(Calendar.DAY_OF_YEAR, -6)
            StatsPeriod.MONTH -> calendar.add(Calendar.MONTH, -1)
            StatsPeriod.YEAR -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.add(Calendar.MONTH, -11)
            }
        }

        return StatisticsDateRange(
            startMillis = calendar.timeInMillis,
            endMillis = end,
        )
    }
}
