package com.atelbay.money_manager.feature.statistics.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.theme.ExpenseColor
import com.atelbay.money_manager.core.ui.theme.IncomeColor
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.feature.statistics.domain.model.CategorySummary
import com.atelbay.money_manager.feature.statistics.domain.model.DailyTotal
import com.atelbay.money_manager.feature.statistics.domain.model.StatsPeriod
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: StatisticsState,
    onPeriodChange: (StatsPeriod) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("statistics:screen"),
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
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
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("statistics:content"),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Period selector
            item(key = "period") {
                PeriodSelector(
                    selected = state.period,
                    onSelect = onPeriodChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // Totals
            item(key = "totals") {
                TotalsRow(
                    totalExpenses = state.totalExpenses,
                    totalIncome = state.totalIncome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // Donut chart
            if (state.expensesByCategory.isNotEmpty()) {
                item(key = "donut") {
                    DonutChart(
                        categories = state.expensesByCategory,
                        totalExpenses = state.totalExpenses,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 8.dp)
                            .testTag("statistics:pieChart"),
                    )
                }
            }

            // Bar chart
            if (state.dailyExpenses.isNotEmpty()) {
                item(key = "bar_title") {
                    Text(
                        text = "Расходы по дням",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item(key = "bar") {
                    ExpenseBarChart(
                        dailyExpenses = state.dailyExpenses,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 16.dp)
                            .testTag("statistics:barChart"),
                    )
                }
            }

            // Category breakdown list
            if (state.expensesByCategory.isNotEmpty()) {
                item(key = "breakdown_title") {
                    Text(
                        text = "По категориям",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                items(
                    items = state.expensesByCategory,
                    key = { it.categoryId },
                ) { summary ->
                    CategoryRow(
                        summary = summary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("statistics:category_${summary.categoryId}"),
                    )
                }
            }

            // Empty state
            if (state.expensesByCategory.isEmpty() && state.totalIncome == 0.0) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Нет данных за выбранный период",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.testTag("statistics:periodSelector"),
    ) {
        StatsPeriod.entries.forEachIndexed { index, period ->
            SegmentedButton(
                selected = selected == period,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = StatsPeriod.entries.size),
                modifier = Modifier.testTag("statistics:period_${period.name}"),
            ) {
                Text(
                    text = when (period) {
                        StatsPeriod.WEEK -> "Неделя"
                        StatsPeriod.MONTH -> "Месяц"
                        StatsPeriod.YEAR -> "Год"
                    },
                )
            }
        }
    }
}

@Composable
private fun TotalsRow(
    totalExpenses: Double,
    totalIncome: Double,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TotalCard(
            label = "Расходы",
            amount = totalExpenses,
            color = ExpenseColor,
            modifier = Modifier
                .weight(1f)
                .testTag("statistics:totalExpenses"),
        )
        TotalCard(
            label = "Доходы",
            amount = totalIncome,
            color = IncomeColor,
            modifier = Modifier
                .weight(1f)
                .testTag("statistics:totalIncome"),
        )
    }
}

@Composable
private fun TotalCard(
    label: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatAmount(amount),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun DonutChart(
    categories: ImmutableList<CategorySummary>,
    totalExpenses: Double,
    modifier: Modifier = Modifier,
) {
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

            var startAngle = -90f
            categories.forEach { cat ->
                val sweep = (cat.percentage / 100f) * 360f * animProgress.value
                drawArc(
                    color = Color(cat.categoryColor),
                    startAngle = startAngle,
                    sweepAngle = sweep,
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatAmount(totalExpenses),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ExpenseBarChart(
    dailyExpenses: ImmutableList<DailyTotal>,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(dailyExpenses) {
        if (dailyExpenses.isNotEmpty()) {
            modelProducer.runTransaction {
                columnSeries {
                    series(dailyExpenses.map { it.amount })
                }
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}

@Composable
private fun CategoryRow(
    summary: CategorySummary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(summary.categoryColor)),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = summary.categoryName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatAmount(summary.totalAmount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${summary.percentage.toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val amountFormat = DecimalFormat("#,##0.##")
private fun formatAmount(amount: Double): String = amountFormat.format(amount)

@Preview(showBackground = true)
@Composable
private fun StatisticsScreenPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        StatisticsScreen(
            state = StatisticsState(
                isLoading = false,
                totalExpenses = 85_000.0,
                totalIncome = 200_000.0,
                expensesByCategory = persistentListOf(
                    CategorySummary(1, "Еда", "restaurant", 0xFFE57373, 35_000.0, 41.2f),
                    CategorySummary(2, "Транспорт", "directions_car", 0xFF64B5F6, 20_000.0, 23.5f),
                    CategorySummary(3, "Развлечения", "sports_esports", 0xFFBA68C8, 15_000.0, 17.6f),
                    CategorySummary(4, "Покупки", "shopping_bag", 0xFFFFB74D, 15_000.0, 17.6f),
                ),
                dailyExpenses = persistentListOf(
                    DailyTotal(1L, 5000.0),
                    DailyTotal(2L, 3000.0),
                    DailyTotal(3L, 8000.0),
                    DailyTotal(4L, 2000.0),
                    DailyTotal(5L, 12000.0),
                ),
            ),
            onPeriodChange = {},
            onBack = {},
        )
    }
}
