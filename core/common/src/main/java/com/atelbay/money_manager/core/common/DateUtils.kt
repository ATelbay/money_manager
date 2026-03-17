package com.atelbay.money_manager.core.common

import java.util.Calendar
import java.util.TimeZone

fun startOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance(TimeZone.getDefault())
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
