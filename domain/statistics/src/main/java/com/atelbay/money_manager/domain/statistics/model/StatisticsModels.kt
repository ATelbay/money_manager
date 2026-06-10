package com.atelbay.money_manager.domain.statistics.model

/**
 * Metadata for a single category (no amounts). The numeric aggregation lives entirely in the
 * presentation layer's currency-display resolver, because it depends on the display currency and
 * the live exchange rate — see `StatisticsCurrencyDisplayResolver`.
 */
data class CategoryMetadata(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
)

/**
 * An ordered, zero-fillable month slot for the YEAR period. Carries no amount — only the
 * year/month identity and a pre-formatted short label.
 */
data class MonthBucket(
    val year: Int,
    val month: Int,
    val label: String,
)

data class StatisticsDateRange(
    val startMillis: Long,
    val endMillis: Long,
)

enum class TransactionType {
    EXPENSE,
    INCOME,
}

/**
 * The "skeleton" of a statistics period. It contains everything that does **not** depend on the
 * actual transaction amounts or the display currency:
 * - the resolved [dateRange];
 * - [dayBuckets] — ordered, zero-filled day-start millis (empty for YEAR, which renders months);
 * - [monthBuckets] — ordered, zero-filled months (populated only for YEAR);
 * - [categories] — the category catalog (metadata only).
 *
 * All numeric aggregation (totals, per-category/day/month sums, percentages) is performed once in
 * the presentation layer's currency-display resolver from a single transaction stream, so the
 * amounts are never computed twice.
 */
data class PeriodSummary(
    val dateRange: StatisticsDateRange,
    val dayBuckets: List<Long>,
    val monthBuckets: List<MonthBucket>,
    val categories: List<CategoryMetadata>,
)

enum class StatsPeriod {
    WEEK,
    MONTH,
    YEAR,
}
