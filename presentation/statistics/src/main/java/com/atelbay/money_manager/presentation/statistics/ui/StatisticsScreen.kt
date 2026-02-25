package com.atelbay.money_manager.presentation.statistics.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.components.MoneyManagerChip
import com.atelbay.money_manager.core.ui.components.StatType
import com.atelbay.money_manager.core.ui.components.SummaryStatCard
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal
import com.atelbay.money_manager.domain.statistics.model.CategorySummary
import com.atelbay.money_manager.domain.statistics.model.DailyTotal
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: StatisticsState,
    onPeriodChange: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Scaffold(
        modifier = modifier.testTag("statistics:screen"),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Статистика",
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Teal)
            }
            return@Scaffold
        }

        // Empty state
        if (state.expensesByCategory.isEmpty() && state.totalIncome == 0.0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // Period selector even in empty state
                PeriodSelector(
                    selected = state.period,
                    onSelect = onPeriodChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )

                EmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("statistics:content"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Period selector
            item(key = "period") {
                PeriodSelector(
                    selected = state.period,
                    onSelect = onPeriodChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            // Summary Cards — Bento Grid
            item(key = "totals") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryStatCard(
                        title = "РАСХОДЫ",
                        value = state.totalExpenses,
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        type = StatType.EXPENSE,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("statistics:totalExpenses"),
                    )
                    SummaryStatCard(
                        title = "ДОХОДЫ",
                        value = state.totalIncome,
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        type = StatType.INCOME,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("statistics:totalIncome"),
                    )
                }
            }

            // Donut Chart
            if (state.expensesByCategory.isNotEmpty()) {
                item(key = "donut") {
                    DonutChartCard(
                        categories = state.expensesByCategory,
                        totalExpenses = state.totalExpenses,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("statistics:pieChart"),
                    )
                }
            }

            // Bar Chart
            if (state.dailyExpenses.isNotEmpty()) {
                item(key = "bar") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "РАСХОДЫ ПО ДНЯМ",
                            style = typography.caption,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            ExpenseBarChart(
                                dailyExpenses = state.dailyExpenses,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(16.dp)
                                    .testTag("statistics:barChart"),
                            )
                        }
                    }
                }
            }

            // Category Breakdown
            if (state.expensesByCategory.isNotEmpty()) {
                item(key = "breakdown") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "ПО КАТЕГОРИЯМ",
                            style = typography.caption,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                        CategoryBreakdownCard(
                            categories = state.expensesByCategory,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// ── Period Selector ──

@Composable
private fun PeriodSelector(
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.testTag("statistics:periodSelector"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatsPeriod.entries.forEachIndexed { index, period ->
            if (index > 0) Spacer(modifier = Modifier.width(8.dp))
            MoneyManagerChip(
                label = when (period) {
                    StatsPeriod.WEEK -> "Неделя"
                    StatsPeriod.MONTH -> "Месяц"
                    StatsPeriod.YEAR -> "Год"
                },
                selected = selected == period,
                onClick = { onSelect(period) },
                modifier = Modifier.testTag("statistics:period_${period.name}"),
            )
        }
    }
}

// ── Donut Chart Card ──

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DonutChartCard(
    categories: ImmutableList<CategorySummary>,
    totalExpenses: Double,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Donut chart
            DonutChart(
                categories = categories,
                totalExpenses = totalExpenses,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                categories.forEach { category ->
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(category.categoryColor)),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = category.categoryName,
                            style = typography.caption,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    categories: ImmutableList<CategorySummary>,
    totalExpenses: Double,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val formatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(categories) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.18f
            val radius = (size.minDimension - strokeWidth) / 2f
            val topLeft = Offset(
                (size.width - radius * 2) / 2f,
                (size.height - radius * 2) / 2f,
            )
            val arcSize = Size(radius * 2, radius * 2)
            val gap = 2f

            var startAngle = -90f
            categories.forEach { cat ->
                val sweep = (cat.percentage / 100f) * 360f * animProgress.value
                drawArc(
                    color = Color(cat.categoryColor),
                    startAngle = startAngle + gap / 2f,
                    sweepAngle = (sweep - gap).coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
                startAngle += sweep
            }
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Расходы",
                style = typography.caption,
                color = colors.textSecondary,
            )
            Text(
                text = "\u20B8 ${formatter.format(totalExpenses)}",
                style = typography.sectionHeader,
                color = colors.textPrimary,
            )
        }
    }
}

// ── Bar Chart ──

@Composable
private fun ExpenseBarChart(
    dailyExpenses: ImmutableList<DailyTotal>,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val barColor = colors.expense.copy(alpha = 0.8f)
    val gridColor = colors.borderSubtle
    val labelColor = colors.textTertiary
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MoneyManagerTheme.typography.caption.copy(color = labelColor)

    val dateFormat = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }

    val labels = remember(dailyExpenses) {
        dailyExpenses.map { daily ->
            dateFormat.format(Date(daily.date))
        }
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(dailyExpenses) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 600))
    }

    Canvas(modifier = modifier) {
        if (dailyExpenses.isEmpty()) return@Canvas

        val maxAmount = dailyExpenses.maxOf { it.amount }
        if (maxAmount <= 0.0) return@Canvas

        val topPadding = 8.dp.toPx()
        val bottomPadding = 28.dp.toPx()
        val chartHeight = size.height - bottomPadding - topPadding
        val barSpacing = 6.dp.toPx()
        val maxBarWidth = 40.dp.toPx()
        val naturalBarWidth =
            (size.width - barSpacing * (dailyExpenses.size - 1)) / dailyExpenses.size
        val barWidth = naturalBarWidth.coerceAtMost(maxBarWidth)
        val cornerRadius = 4.dp.toPx()

        val totalBarsWidth =
            barWidth * dailyExpenses.size + barSpacing * (dailyExpenses.size - 1)
        val leftOffset = (size.width - totalBarsWidth) / 2f

        // Grid lines
        for (i in 1..3) {
            val y = topPadding + chartHeight * (1f - i / 4f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Bars + labels
        dailyExpenses.forEachIndexed { index, daily ->
            val barHeight =
                (daily.amount / maxAmount).toFloat() * chartHeight * animProgress.value
            val x = leftOffset + index * (barWidth + barSpacing)
            val y = topPadding + chartHeight - barHeight

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            )

            // Label below bar
            val label = labels[index]
            val measuredText = textMeasurer.measure(label, labelStyle)
            val labelX = x + (barWidth - measuredText.size.width) / 2f
            val labelY = topPadding + chartHeight + 4.dp.toPx()
            drawText(measuredText, topLeft = Offset(labelX, labelY))
        }
    }
}

// ── Category Breakdown ──

@Composable
private fun CategoryBreakdownCard(
    categories: ImmutableList<CategorySummary>,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val amountFormatter = remember { DecimalFormat("#,##0") }

    GlassCard(modifier = modifier) {
        Column {
            categories.forEachIndexed { index, summary ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("statistics:category_${summary.categoryId}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Color dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(summary.categoryColor)),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Category name
                    Text(
                        text = summary.categoryName,
                        style = typography.cardTitle,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Percentage badge
                    Text(
                        text = "${summary.percentage.toInt()}%",
                        style = typography.caption,
                        color = Color(summary.categoryColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(summary.categoryColor).copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Amount
                    Text(
                        text = "\u20B8 ${amountFormatter.format(summary.totalAmount)}",
                        style = typography.amount,
                        color = colors.textPrimary,
                    )
                }

                // Divider
                if (index < categories.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = colors.borderSubtle,
                    )
                }
            }
        }
    }
}

// ── Empty State ──

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val colors = MoneyManagerTheme.colors

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Simple bar chart icon
            Canvas(
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp),
            ) {
                val barWidth = 8.dp.toPx()
                val spacing = 13.dp.toPx()
                val startX = 15.dp.toPx()
                val baseY = 60.dp.toPx()
                val heights = listOf(15f, 25f, 35f, 20f)
                val alphas = listOf(1f, 0.7f, 0.5f, 0.3f)

                heights.forEachIndexed { index, height ->
                    val x = startX + index * spacing
                    val h = height.dp.toPx()
                    drawRoundRect(
                        color = Teal.copy(alpha = alphas[index]),
                        topLeft = Offset(x, baseY - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }

                // Base line
                drawLine(
                    color = Teal.copy(alpha = 0.3f),
                    start = Offset(10.dp.toPx(), baseY),
                    end = Offset(70.dp.toPx(), baseY),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            Text(
                text = "Нет данных за выбранный период",
                style = MoneyManagerTheme.typography.cardTitle,
                color = colors.textSecondary,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun StatisticsScreenPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        StatisticsScreen(
            state = StatisticsState(
                isLoading = false,
                totalExpenses = 85_000.0,
                totalIncome = 200_000.0,
                expensesByCategory = persistentListOf(
                    CategorySummary(1, "Еда", "restaurant", 0xFFFF6B6B, 35_000.0, 41.2f),
                    CategorySummary(2, "Транспорт", "directions_car", 0xFF4ECDC4, 20_000.0, 23.5f),
                    CategorySummary(
                        3,
                        "Развлечения",
                        "sports_esports",
                        0xFF45B7D1,
                        15_000.0,
                        17.6f,
                    ),
                    CategorySummary(4, "Покупки", "shopping_bag", 0xFFF7B801, 15_000.0, 17.6f),
                ),
                dailyExpenses = persistentListOf(
                    DailyTotal(1738800000000, 5000.0),
                    DailyTotal(1738886400000, 3000.0),
                    DailyTotal(1738972800000, 8000.0),
                    DailyTotal(1739059200000, 2000.0),
                    DailyTotal(1739145600000, 12000.0),
                ),
            ),
            onPeriodChange = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun StatisticsScreenEmptyPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        StatisticsScreen(
            state = StatisticsState(isLoading = false),
            onPeriodChange = {},
        )
    }
}
