package com.atelbay.money_manager.presentation.statistics.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.components.MoneyManagerSegmentedButton
import com.atelbay.money_manager.core.ui.components.StatType
import com.atelbay.money_manager.core.ui.components.SummaryStatCard
import com.atelbay.money_manager.core.ui.theme.MoneyManagerMotion
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal
import com.atelbay.money_manager.core.ui.util.LocalReduceMotion
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.core.ui.util.MoneyDisplayPresentation
import com.atelbay.money_manager.core.ui.util.formatAmount
import com.atelbay.money_manager.domain.statistics.model.CategorySummary
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.VicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shape.dashedShape
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.VicoTheme
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.Shape
import android.text.Layout
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.ColumnCartesianLayerMarkerTarget
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.TimeZone



private class TodayColumnProvider(
    private val fullOpacityColumn: LineComponent,
    private val reducedOpacityColumn: LineComponent,
) : ColumnCartesianLayer.ColumnProvider {
    override fun getColumn(
        entry: ColumnCartesianLayerModel.Entry,
        seriesIndex: Int,
        extraStore: ExtraStore,
    ): LineComponent {
        val todayIndex = extraStore.getOrNull(todayIndexKey) ?: -1
        return if (entry.x.toInt() == todayIndex) fullOpacityColumn else reducedOpacityColumn
    }

    override fun getWidestSeriesColumn(seriesIndex: Int, extraStore: ExtraStore): LineComponent =
        fullOpacityColumn
}

private class DynamicRangeProvider : CartesianLayerRangeProvider {
    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val visibleMax = extraStore.getOrNull(visibleMaxYKey) ?: return maxY.coerceAtLeast(0.0)
        if (visibleMax <= 0.0) return if (maxY > 0.0) maxY * 1.1 else 1.0
        return visibleMax * 1.1  // 10% padding above tallest visible bar
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: StatisticsState,
    chartModelProducer: CartesianChartModelProducer = CartesianChartModelProducer(),
    onPeriodChange: (StatsPeriod) -> Unit,
    onTransactionTypeChange: (TransactionType) -> Unit,
    onCategoryClick: (CategorySummary) -> Unit = {},
    onRetry: () -> Unit,
    onSetMonth: (YearMonth?) -> Unit = {},
    onVisibleMaxChanged: (Double) -> Unit = {},
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val s = MoneyManagerTheme.strings

    val isExpense = state.transactionType == TransactionType.EXPENSE
    val currentCategories = if (isExpense) {
        state.displayedExpensesByCategory
    } else {
        state.displayedIncomesByCategory
    }
    val currentTotal = if (isExpense) state.displayedTotalExpenses else state.displayedTotalIncome

    var showMonthPicker by remember { mutableStateOf(false) }
    val monthFormatter = remember(s.locale) { DateTimeFormatter.ofPattern("MMM yyyy", s.locale) }
    val pillLabel = (state.selectedMonth ?: YearMonth.now()).format(monthFormatter)

    if (showMonthPicker) {
        MonthPickerDialog(
            initialYearMonth = state.selectedMonth ?: YearMonth.now(),
            onMonthSelected = { yearMonth ->
                onSetMonth(yearMonth)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
        )
    }

    Scaffold(
        modifier = modifier.testTag("statistics:screen"),
        contentWindowInsets = contentWindowInsets,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        s.statisticsTitle,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                        color = colors.textPrimary,
                    )
                },
                actions = {
                    CalendarFilterPill(
                        label = pillLabel,
                        onClick = { showMonthPicker = true },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                modifier = Modifier.testTag("statistics:header"),
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

        // Error state
        if (state.error != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                PeriodSelector(
                    selected = state.period,
                    onSelect = onPeriodChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                StatisticsTypeCards(
                    state = state,
                    onTransactionTypeChange = onTransactionTypeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("statistics:error"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error,
                        style = typography.cardTitle,
                        color = colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.testTag("statistics:retryButton"),
                    ) {
                        Text(text = s.retryButton)
                    }
                }
            }
            return@Scaffold
        }

        // Empty state
        val isEmpty = if (isExpense) {
            state.displayedExpensesByCategory.isEmpty()
        } else {
            state.displayedIncomesByCategory.isEmpty()
        }
        if (isEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                PeriodSelector(
                    selected = state.period,
                    onSelect = onPeriodChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                StatisticsTypeCards(
                    state = state,
                    onTransactionTypeChange = onTransactionTypeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                EmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("statistics:emptyState"),
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
            item(key = "period") {
                PeriodSelector(
                    selected = state.period,
                    onSelect = onPeriodChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            // Unified Chart Card (replaces old "totals" + "bar" items)
            item(key = "chart_card") {
                UnifiedChartCard(
                    state = state,
                    chartModelProducer = chartModelProducer,
                    onTransactionTypeChange = onTransactionTypeChange,
                    onVisibleMaxChanged = onVisibleMaxChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            // By Category Section (donut + legend + expandable breakdown)
            if (currentCategories.isNotEmpty()) {
                item(key = "categories") {
                    ByCategorySection(
                        categories = currentCategories,
                        totalAmount = currentTotal,
                        moneyDisplay = state.currencyUiState.moneyDisplay,
                        isUnavailable = state.currencyUiState.isUnavailable,
                        unavailableText = s.mixedCurrencyUnavailable,
                        onCategoryClick = onCategoryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
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
    val strings = MoneyManagerTheme.strings
    val options = listOf(strings.periodWeek, strings.periodMonth, strings.periodYear)
    val selectedOption = when (selected) {
        StatsPeriod.WEEK -> strings.periodWeek
        StatsPeriod.MONTH -> strings.periodMonth
        StatsPeriod.YEAR -> strings.periodYear
    }
    MoneyManagerSegmentedButton(
        options = options,
        selectedOption = selectedOption,
        onOptionSelected = { option ->
            val period = when (option) {
                strings.periodWeek -> StatsPeriod.WEEK
                strings.periodMonth -> StatsPeriod.MONTH
                else -> StatsPeriod.YEAR
            }
            onSelect(period)
        },
        height = 44.dp,
        testTagPrefix = "statistics:period",
        modifier = modifier.testTag("statistics:periodSelector"),
    )
}

@Composable
private fun StatisticsTypeCards(
    state: StatisticsState,
    onTransactionTypeChange: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = MoneyManagerTheme.strings
    Row(
        modifier = modifier.testTag("statistics:typeSelector"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryStatCard(
            title = strings.expensesUpper,
            value = state.displayedTotalExpenses,
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            moneyDisplay = state.currencyUiState.moneyDisplay,
            unavailableSupportingText = strings.mixedCurrencyUnavailable
                .takeIf { state.currencyUiState.isUnavailable },
            type = StatType.EXPENSE,
            selected = state.transactionType == TransactionType.EXPENSE,
            onClick = { onTransactionTypeChange(TransactionType.EXPENSE) },
            modifier = Modifier
                .weight(1f)
                .testTag("statistics:totalExpenses"),
        )
        SummaryStatCard(
            title = strings.incomeUpper,
            value = state.displayedTotalIncome,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            moneyDisplay = state.currencyUiState.moneyDisplay,
            unavailableSupportingText = strings.mixedCurrencyUnavailable
                .takeIf { state.currencyUiState.isUnavailable },
            type = StatType.INCOME,
            selected = state.transactionType == TransactionType.INCOME,
            onClick = { onTransactionTypeChange(TransactionType.INCOME) },
            modifier = Modifier
                .weight(1f)
                .testTag("statistics:totalIncome"),
        )
    }
}

// ── Donut Chart ──

private fun formatCompactAmount(amount: Double): String = when {
    amount >= 1_000_000 -> String.format("%.1fM", amount / 1_000_000)
    amount >= 1_000 -> String.format("%.0fK", amount / 1_000)
    else -> String.format("%.0f", amount)
}

@Composable
private fun DonutChart(
    categories: ImmutableList<StatisticsCategoryDisplayItem>,
    totalAmount: Double,
    moneyDisplay: MoneyDisplayPresentation,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors

    val reduceMotion = LocalReduceMotion.current
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(categories.map { it.category.categoryId to it.displayAmount }) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = MoneyManagerMotion.duration(MoneyManagerMotion.DurationExtraLong, reduceMotion),
            ),
        )
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
                val sweep = (cat.displayPercentage / 100f) * 360f * animProgress.value
                drawArc(
                    color = Color(cat.category.categoryColor),
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatCompactAmount(totalAmount),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                lineHeight = 14.sp,
                color = colors.textPrimary,
            )
            Text(
                text = moneyDisplay.primaryLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 12.sp,
                color = colors.textSecondary,
            )
        }
    }
}


// ── Vico Bar Chart ──

@Composable
private fun VicoBarChartSection(
    chart: StatisticsChartState,
    modelProducer: CartesianChartModelProducer,
    barColor: Color,
    isUnavailable: Boolean,
    unavailableText: String,
    period: StatsPeriod,
    scrollState: VicoScrollState,
    points: ImmutableList<StatisticsChartPoint>,
    onVisibleMaxChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors

    var chartWidthPx by remember { mutableIntStateOf(0) }

    val amounts = remember(points) { points.map { it.amount ?: 0.0 } }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    LaunchedEffect(scrollState, amounts, chartWidthPx) {
        if (amounts.isEmpty() || chartWidthPx <= 0) return@LaunchedEffect
        snapshotFlow { scrollState.value to scrollState.maxValue }
            .distinctUntilChanged()
            .debounce(150)
            .collect { (scrollPx, maxScrollPx) ->
                val totalContentPx = chartWidthPx + maxScrollPx
                if (totalContentPx <= 0f) return@collect

                val totalBars = amounts.size
                val firstVisible = (totalBars * scrollPx / totalContentPx).toInt().coerceIn(0, totalBars - 1)
                val visibleCount = (totalBars * chartWidthPx / totalContentPx).toInt().coerceAtLeast(1)
                val lastVisible = (firstVisible + visibleCount + 1).coerceAtMost(totalBars) // +1 for partially visible edge bar

                val visibleMax = amounts.subList(firstVisible, lastVisible).max()
                onVisibleMaxChanged(visibleMax)
            }
    }

    Column(modifier = modifier) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            if (isUnavailable) {
                StatisticsUnavailableCard(
                    text = unavailableText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("statistics:barChartUnavailable"),
                )
            } else {
                val yAxisFormatter = remember {
                    CartesianValueFormatter { context, value, _ ->
                        val symbol = context.model.extraStore.getOrNull(currencySymbolKey) ?: ""
                        val isPrefix = context.model.extraStore.getOrNull(currencyPrefixKey) ?: true
                        val formatted = when {
                            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000)
                            value >= 1_000 -> String.format("%.0fK", value / 1_000)
                            else -> String.format("%.0f", value)
                        }
                        if (isPrefix) "$symbol $formatted" else "$formatted $symbol"
                    }
                }

                val xAxisFormatter = remember {
                    CartesianValueFormatter { context, value, _ ->
                        val label = context.model.extraStore.getOrNull(xToLabelMapKey)?.get(value) ?: ""
                        val todayIndex = context.model.extraStore.getOrNull(todayIndexKey) ?: -1
                        if (value.toInt() == todayIndex) "$label•" else label
                    }
                }

                val zoomState = rememberVicoZoomState(zoomEnabled = false)

                val fullOpacityColumn = rememberLineComponent(
                    fill = remember(barColor) { fill(barColor) },
                    thickness = 16.dp,
                    shape = CorneredShape.rounded(
                        topLeftPercent = 40,
                        topRightPercent = 40,
                    ),
                )
                val reducedOpacityColumn = rememberLineComponent(
                    fill = remember(barColor) { fill(barColor.copy(alpha = 0.7f)) },
                    thickness = 16.dp,
                    shape = CorneredShape.rounded(
                        topLeftPercent = 40,
                        topRightPercent = 40,
                    ),
                )
                val columnProvider = remember(fullOpacityColumn, reducedOpacityColumn) {
                    TodayColumnProvider(fullOpacityColumn, reducedOpacityColumn)
                }
                val rangeProvider = remember { DynamicRangeProvider() }

                val marker = rememberDefaultCartesianMarker(
                    label = rememberTextComponent(
                        color = colors.textPrimary,
                        textAlignment = Layout.Alignment.ALIGN_CENTER,
                    ),
                    valueFormatter = DefaultCartesianMarker.ValueFormatter { context, targets ->
                        val target = targets.firstOrNull() ?: return@ValueFormatter ""
                        val entry = (target as? ColumnCartesianLayerMarkerTarget)?.columns?.firstOrNull()
                        val x = entry?.entry?.x ?: return@ValueFormatter ""
                        val y = entry.entry.y

                        val symbol = context.model.extraStore.getOrNull(currencySymbolKey) ?: ""
                        val isPrefix = context.model.extraStore.getOrNull(currencyPrefixKey) ?: true
                        val dateStr = context.model.extraStore.getOrNull(xToDateStringKey)?.get(x) ?: ""

                        val amountFormatted = java.text.NumberFormat.getNumberInstance().format(y)
                        val amountWithCurrency = if (isPrefix) "$symbol $amountFormatted" else "$amountFormatted $symbol"
                        "$amountWithCurrency\n$dateStr"
                    },
                )

                ProvideVicoTheme(
                    theme = VicoTheme(
                        candlestickCartesianLayerColors = VicoTheme.CandlestickCartesianLayerColors(
                            bullish = barColor,
                            neutral = barColor,
                            bearish = barColor,
                        ),
                        columnCartesianLayerColors = listOf(barColor),
                        textColor = colors.textSecondary,
                        lineColor = colors.borderSubtle,
                    ),
                ) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer(
                                columnProvider = columnProvider,
                                rangeProvider = rangeProvider,
                            ),
                            startAxis = VerticalAxis.rememberStart(
                                valueFormatter = yAxisFormatter,
                                guideline = rememberLineComponent(
                                    fill = remember(colors.borderSubtle) { fill(colors.borderSubtle) },
                                    shape = dashedShape(
                                        shape = Shape.Rectangle,
                                        dashLength = 8.dp,
                                        gapLength = 4.dp,
                                    ),
                                ),
                            ),
                            bottomAxis = HorizontalAxis.rememberBottom(
                                valueFormatter = xAxisFormatter,
                                guideline = null,
                            ),
                            marker = marker,
                        ),
                        modelProducer = modelProducer,
                        scrollState = scrollState,
                        zoomState = zoomState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(16.dp)
                            .onSizeChanged { chartWidthPx = it.width }
                            .testTag(
                                if (period == StatsPeriod.MONTH) {
                                    "statistics:monthChartContainer"
                                } else {
                                    "statistics:barChart"
                                },
                            ),
                    )
                }
            }
        }
    }
}

// ── Chart Card Composables ──

// T005
@Composable
private fun ExpenseIncomeToggle(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors

    val expenseBg by animateColorAsState(
        targetValue = if (selected == TransactionType.EXPENSE) colors.glassBgStart else Color.Transparent,
        animationSpec = MoneyManagerMotion.ColorTransition,
        label = "expenseBg",
    )
    val expenseText by animateColorAsState(
        targetValue = if (selected == TransactionType.EXPENSE) colors.textPrimary else colors.textSecondary,
        animationSpec = MoneyManagerMotion.ColorTransition,
        label = "expenseText",
    )
    val incomeBg by animateColorAsState(
        targetValue = if (selected == TransactionType.INCOME) colors.glassBgStart else Color.Transparent,
        animationSpec = MoneyManagerMotion.ColorTransition,
        label = "incomeBg",
    )
    val incomeText by animateColorAsState(
        targetValue = if (selected == TransactionType.INCOME) colors.textPrimary else colors.textSecondary,
        animationSpec = MoneyManagerMotion.ColorTransition,
        label = "incomeText",
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceBorder)
            .padding(4.dp)
            .testTag("statistics:expenseIncomeToggle"),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val strings = MoneyManagerTheme.strings
        // Expense pill
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onSelect(TransactionType.EXPENSE) },
            color = expenseBg,
            tonalElevation = if (selected == TransactionType.EXPENSE) 1.dp else 0.dp,
            shadowElevation = if (selected == TransactionType.EXPENSE) 1.dp else 0.dp,
        ) {
            Text(
                text = strings.expensesLabel,
                color = expenseText,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        // Income pill
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onSelect(TransactionType.INCOME) },
            color = incomeBg,
            tonalElevation = if (selected == TransactionType.INCOME) 1.dp else 0.dp,
            shadowElevation = if (selected == TransactionType.INCOME) 1.dp else 0.dp,
        ) {
            Text(
                text = strings.incomeLabel,
                color = incomeText,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

// T006
@Composable
private fun ChartCardHeader(
    title: String,
    dateRange: String,
    selectedType: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.testTag("statistics:chartTitle"),
            )
            Text(
                text = dateRange,
                fontSize = 12.sp,
                color = colors.textSecondary,
                modifier = Modifier.testTag("statistics:chartDateRange"),
            )
        }
        ExpenseIncomeToggle(
            selected = selectedType,
            onSelect = onTypeChange,
        )
    }
}

// T007
@Composable
private fun ChartScrollIndicator(
    scrollFraction: Float,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    val colors = MoneyManagerTheme.colors
    val trackWidth = 60.dp
    val thumbWidth = 24.dp
    val trackHeight = 3.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("statistics:scrollIndicator"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(trackHeight)
                .clip(RoundedCornerShape(50))
                .background(colors.divider),
        ) {
            val thumbOffset = ((trackWidth - thumbWidth) * scrollFraction.coerceIn(0f, 1f))
            Box(
                modifier = Modifier
                    .width(thumbWidth)
                    .height(trackHeight)
                    .offset(x = thumbOffset)
                    .clip(RoundedCornerShape(50))
                    .background(colors.textSecondary),
            )
        }
    }
}

// T008
@Composable
private fun ChartTotalRow(
    label: String,
    amount: Double?,
    moneyDisplay: MoneyDisplayPresentation,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val amountFormatter = remember { DecimalFormat("#,##0") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
        )
        if (amount != null) {
            Text(
                text = moneyDisplay.formatAmount(amount = amount, formatter = amountFormatter),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun formatDateRangeLabel(dateRange: com.atelbay.money_manager.domain.statistics.model.StatisticsDateRange?): String {
    if (dateRange == null) return ""
    val strings = MoneyManagerTheme.strings
    val locale = strings.locale
    val timeZone = TimeZone.getDefault()
    val start = Calendar.getInstance(timeZone).apply { timeInMillis = dateRange.startMillis }
    val end = Calendar.getInstance(timeZone).apply { timeInMillis = dateRange.endMillis }

    val monthDayFormat = remember(locale) { SimpleDateFormat("MMM d", locale) }
    val fullDateFormat = remember(locale) { SimpleDateFormat("MMM d, yyyy", locale) }
    val monthFormat = remember(locale) { SimpleDateFormat("MMM", locale) }

    return when {
        start.get(Calendar.YEAR) != end.get(Calendar.YEAR) -> {
            strings.statisticsDateRangeCrossYear(
                fullDateFormat.format(Date(dateRange.startMillis)),
                fullDateFormat.format(Date(dateRange.endMillis)),
            )
        }
        start.get(Calendar.MONTH) == end.get(Calendar.MONTH) -> {
            strings.statisticsDateRangeSingleMonth(
                monthFormat.format(start.time),
                start.get(Calendar.DAY_OF_MONTH),
                end.get(Calendar.DAY_OF_MONTH),
                end.get(Calendar.YEAR),
            )
        }
        else -> {
            strings.statisticsDateRangeCrossMonth(
                monthDayFormat.format(Date(dateRange.startMillis)),
                monthDayFormat.format(Date(dateRange.endMillis)),
                end.get(Calendar.YEAR),
            )
        }
    }
}

// T009
@Composable
private fun UnifiedChartCard(
    state: StatisticsState,
    chartModelProducer: CartesianChartModelProducer,
    onTransactionTypeChange: (TransactionType) -> Unit,
    onVisibleMaxChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val strings = MoneyManagerTheme.strings
    val isExpense = state.transactionType == TransactionType.EXPENSE

    val barColor = if (isExpense) colors.expense.copy(alpha = 0.8f) else colors.income.copy(alpha = 0.8f)

    // T011: lift scrollState here so we can observe it for the indicator
    val scrollState = rememberVicoScrollState(
        scrollEnabled = true,
        initialScroll = Scroll.Absolute.End,
    )

    val scrollFraction = scrollState.value / scrollState.maxValue.coerceAtLeast(1f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val chartTitle = when (state.transactionType) {
                TransactionType.EXPENSE -> strings.expensesLabel
                TransactionType.INCOME -> strings.incomeLabel
            }.let { subject ->
                when (state.period) {
                    StatsPeriod.YEAR -> strings.statisticsChartByMonth(subject)
                    StatsPeriod.WEEK, StatsPeriod.MONTH -> strings.statisticsChartByDay(subject)
                }
            }
            val dateRangeLabel = formatDateRangeLabel(state.dateRange)
            ChartCardHeader(
                title = chartTitle,
                dateRange = dateRangeLabel,
                selectedType = state.transactionType,
                onTypeChange = onTransactionTypeChange,
            )

            if (state.chart.points.isNotEmpty()) {
                if (state.chart.allAmountsZero) {
                    val emptyText = if (isExpense) strings.chartNoExpenses else strings.chartNoIncome
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = emptyText,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Box(modifier = Modifier.height(180.dp)) {
                        VicoBarChartSection(
                            chart = state.chart,
                            modelProducer = chartModelProducer,
                            barColor = barColor,
                            isUnavailable = state.currencyUiState.isUnavailable,
                            unavailableText = strings.mixedCurrencyUnavailable,
                            period = state.period,
                            scrollState = scrollState,
                            points = state.chart.points,
                            onVisibleMaxChanged = onVisibleMaxChanged,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    ChartScrollIndicator(
                        scrollFraction = scrollFraction,
                        isVisible = true,
                    )
                }
            }

            val totalLabel = if (isExpense) strings.totalExpenses else strings.totalIncome
            val totalAmount = if (isExpense) state.displayedTotalExpenses else state.displayedTotalIncome
            ChartTotalRow(
                label = totalLabel,
                amount = totalAmount,
                moneyDisplay = state.currencyUiState.moneyDisplay,
            )
        }
    }
}

// ── T014: CategoryLegendRow ──

@Composable
private fun CategoryLegendRow(
    color: Color,
    name: String,
    percentage: String,
) {
    val colors = MoneyManagerTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            fontSize = 13.sp,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = percentage,
            fontSize = 13.sp,
            color = color,
        )
    }
}

// ── T015: CategoryLegend ──

@Composable
private fun CategoryLegend(
    categories: ImmutableList<StatisticsCategoryDisplayItem>,
) {
    val colors = MoneyManagerTheme.colors
    val strings = MoneyManagerTheme.strings
    Column(
        modifier = Modifier.testTag("statistics:categoryLegend"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val top3 = categories.take(3)
        val rest = if (categories.size > 3) categories.drop(3) else emptyList()

        top3.forEach { item ->
            CategoryLegendRow(
                color = Color(item.category.categoryColor),
                name = item.category.categoryName,
                percentage = "${item.displayPercentage}%",
            )
        }

        if (rest.isNotEmpty()) {
            val otherPercentage = rest.sumOf { it.displayPercentage }
            CategoryLegendRow(
                color = colors.textSecondary,
                name = strings.other,
                percentage = "$otherPercentage%",
            )
        }
    }
}

// ── T016: CompactDonutCard ──

@Composable
private fun CompactDonutCard(
    categories: ImmutableList<StatisticsCategoryDisplayItem>,
    totalAmount: Double?,
    moneyDisplay: MoneyDisplayPresentation,
) {
    val strings = MoneyManagerTheme.strings
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (totalAmount != null) {
                DonutChart(
                    categories = categories,
                    totalAmount = totalAmount,
                    moneyDisplay = moneyDisplay,
                    modifier = Modifier
                        .size(100.dp)
                        .testTag("statistics:pieChart"),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .testTag("statistics:pieChart"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = strings.mixedCurrencyUnavailable,
                        fontSize = 11.sp,
                        color = MoneyManagerTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                CategoryLegend(categories = categories)
            }
        }
    }
}

// ── T017: ByCategorySection ──

@Composable
private fun ByCategorySection(
    categories: ImmutableList<StatisticsCategoryDisplayItem>,
    totalAmount: Double?,
    moneyDisplay: MoneyDisplayPresentation,
    isUnavailable: Boolean,
    unavailableText: String,
    onCategoryClick: (CategorySummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val amountFormatter = remember { DecimalFormat("#,##0") }
    val reduceMotion = LocalReduceMotion.current

    Column(
        modifier = modifier.testTag("statistics:byCategorySection"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = MoneyManagerTheme.strings.byCategory,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
        }

        CompactDonutCard(
            categories = categories,
            totalAmount = if (isUnavailable) null else totalAmount,
            moneyDisplay = moneyDisplay,
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    categories.forEachIndexed { index, summary ->
                        val alpha = remember(summary.category.categoryId) { Animatable(0f) }
                        LaunchedEffect(summary.category.categoryId) {
                            delay(MoneyManagerMotion.staggerDelay(index, reduceMotion))
                            alpha.animateTo(
                                1f,
                                tween(MoneyManagerMotion.duration(MoneyManagerMotion.DurationMedium, reduceMotion)),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { this.alpha = alpha.value }
                                .clickable { onCategoryClick(summary.category) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("statistics:category_${summary.category.categoryId}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(summary.category.categoryColor)),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = summary.category.categoryName,
                                style = typography.cardTitle,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (isUnavailable || summary.displayAmount == null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = unavailableText,
                                    style = typography.caption,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 140.dp),
                                )
                            } else {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${summary.displayPercentage}%",
                                    style = typography.caption,
                                    color = Color(summary.category.categoryColor),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(summary.category.categoryColor).copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = moneyDisplay.formatAmount(
                                        amount = summary.displayAmount,
                                        formatter = amountFormatter,
                                    ),
                                    style = typography.amount,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 16.sp, stepSize = 1.sp),
                                    modifier = Modifier.widthIn(max = 140.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.textSecondary,
                            )
                        }
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
}

// ── T024: CalendarFilterPill ──

@Composable
private fun CalendarFilterPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.testTag("statistics:calendarPill"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun StatisticsUnavailableCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = typography.cardTitle,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
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
                    .size(100.dp)
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
                text = MoneyManagerTheme.strings.noDataForPeriod,
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
                displayedTotalExpenses = 85_000.0,
                displayedTotalIncome = 200_000.0,
                displayedExpensesByCategory = persistentListOf(
                    StatisticsCategoryDisplayItem(
                        category = CategorySummary(1, "Еда", "restaurant", 0xFFFF6B6B, 35_000.0, 41),
                        displayAmount = 35_000.0,
                        displayPercentage = 41,
                    ),
                    StatisticsCategoryDisplayItem(
                        category = CategorySummary(2, "Транспорт", "directions_car", 0xFF4ECDC4, 20_000.0, 24),
                        displayAmount = 20_000.0,
                        displayPercentage = 24,
                    ),
                    StatisticsCategoryDisplayItem(
                        category = CategorySummary(3, "Развлечения", "sports_esports", 0xFF45B7D1, 15_000.0, 18),
                        displayAmount = 15_000.0,
                        displayPercentage = 18,
                    ),
                    StatisticsCategoryDisplayItem(
                        category = CategorySummary(4, "Покупки", "shopping_bag", 0xFFF7B801, 15_000.0, 17),
                        displayAmount = 15_000.0,
                        displayPercentage = 17,
                    ),
                ),
                displayedDailyExpenses = persistentListOf(
                    StatisticsDisplayDailyTotal(1738800000000, 5000.0),
                    StatisticsDisplayDailyTotal(1738886400000, 3000.0),
                    StatisticsDisplayDailyTotal(1738972800000, 8000.0),
                    StatisticsDisplayDailyTotal(1739059200000, 2000.0),
                    StatisticsDisplayDailyTotal(1739145600000, 12000.0),
                ),
                currencyUiState = StatisticsCurrencyUiState(
                    moneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                    displayMode = com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY,
                ),
                chart = StatisticsChartState(
                    points = persistentListOf(
                        StatisticsChartPoint(1738800000000, "6", 5000.0),
                        StatisticsChartPoint(1738886400000, "7", 3000.0),
                        StatisticsChartPoint(1738972800000, "8", 8000.0),
                        StatisticsChartPoint(1739059200000, "9", 2000.0),
                        StatisticsChartPoint(1739145600000, "10", 12000.0, isToday = true),
                    ),
                    isScrollable = true,
                ),
            ),
            onPeriodChange = {},
            onTransactionTypeChange = {},
            onRetry = {},
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
            onTransactionTypeChange = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun StatisticsScreenErrorPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        StatisticsScreen(
            state = StatisticsState(
                isLoading = false,
                error = "Не удалось загрузить данные",
            ),
            onPeriodChange = {},
            onTransactionTypeChange = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun StatisticsScreenIncomePreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        StatisticsScreen(
            state = StatisticsState(
                isLoading = false,
                transactionType = TransactionType.INCOME,
                displayedTotalExpenses = 85_000.0,
                displayedTotalIncome = 200_000.0,
                displayedIncomesByCategory = persistentListOf(
                    StatisticsCategoryDisplayItem(
                        category = CategorySummary(1, "Зарплата", "payments", 0xFF4ECDC4, 150_000.0, 75),
                        displayAmount = 150_000.0,
                        displayPercentage = 75,
                    ),
                    StatisticsCategoryDisplayItem(
                        category = CategorySummary(2, "Фриланс", "work", 0xFF45B7D1, 50_000.0, 25),
                        displayAmount = 50_000.0,
                        displayPercentage = 25,
                    ),
                ),
                displayedDailyIncome = persistentListOf(
                    StatisticsDisplayDailyTotal(1738800000000, 10000.0),
                    StatisticsDisplayDailyTotal(1738886400000, 5000.0),
                    StatisticsDisplayDailyTotal(1738972800000, 15000.0),
                    StatisticsDisplayDailyTotal(1739059200000, 0.0),
                    StatisticsDisplayDailyTotal(1739145600000, 20000.0),
                ),
                currencyUiState = StatisticsCurrencyUiState(
                    moneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                    displayMode = com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY,
                ),
                chart = StatisticsChartState(
                    points = persistentListOf(
                        StatisticsChartPoint(1738800000000, "6", 10000.0),
                        StatisticsChartPoint(1738886400000, "7", 5000.0),
                        StatisticsChartPoint(1738972800000, "8", 15000.0),
                        StatisticsChartPoint(1739059200000, "9", 0.0),
                        StatisticsChartPoint(1739145600000, "10", 20000.0, isToday = true),
                    ),
                    isScrollable = true,
                ),
            ),
            onPeriodChange = {},
            onTransactionTypeChange = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun StatisticsScreenYearPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        StatisticsScreen(
            state = StatisticsState(
                isLoading = false,
                period = StatsPeriod.YEAR,
                displayedTotalExpenses = 1_200_000.0,
                displayedTotalIncome = 2_400_000.0,
                displayedExpensesByCategory = persistentListOf(
                    StatisticsCategoryDisplayItem(
                        category = CategorySummary(1, "Еда", "restaurant", 0xFFFF6B6B, 500_000.0, 42),
                        displayAmount = 500_000.0,
                        displayPercentage = 42,
                    ),
                    StatisticsCategoryDisplayItem(
                        category = CategorySummary(2, "Транспорт", "directions_car", 0xFF4ECDC4, 300_000.0, 25),
                        displayAmount = 300_000.0,
                        displayPercentage = 25,
                    ),
                    StatisticsCategoryDisplayItem(
                        category = CategorySummary(3, "Развлечения", "sports_esports", 0xFF45B7D1, 200_000.0, 17),
                        displayAmount = 200_000.0,
                        displayPercentage = 17,
                    ),
                    StatisticsCategoryDisplayItem(
                        category = CategorySummary(4, "Покупки", "shopping_bag", 0xFFF7B801, 200_000.0, 16),
                        displayAmount = 200_000.0,
                        displayPercentage = 16,
                    ),
                ),
                displayedMonthlyExpenses = persistentListOf(
                    StatisticsDisplayMonthlyTotal(2025, 1, "Янв", 80_000.0),
                    StatisticsDisplayMonthlyTotal(2025, 2, "Фев", 95_000.0),
                    StatisticsDisplayMonthlyTotal(2025, 3, "Мар", 110_000.0),
                    StatisticsDisplayMonthlyTotal(2025, 4, "Апр", 75_000.0),
                    StatisticsDisplayMonthlyTotal(2025, 5, "Май", 120_000.0),
                    StatisticsDisplayMonthlyTotal(2025, 6, "Июн", 90_000.0),
                    StatisticsDisplayMonthlyTotal(2025, 7, "Июл", 100_000.0),
                    StatisticsDisplayMonthlyTotal(2025, 8, "Авг", 85_000.0),
                    StatisticsDisplayMonthlyTotal(2025, 9, "Сен", 105_000.0),
                    StatisticsDisplayMonthlyTotal(2025, 10, "Окт", 95_000.0),
                    StatisticsDisplayMonthlyTotal(2025, 11, "Ноя", 115_000.0),
                    StatisticsDisplayMonthlyTotal(2025, 12, "Дек", 130_000.0),
                ),
                currencyUiState = StatisticsCurrencyUiState(
                    moneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                    displayMode = com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY,
                ),
                chart = StatisticsChartState(
                    points = persistentListOf(
                        StatisticsChartPoint(1, "Янв", 80_000.0),
                        StatisticsChartPoint(2, "Фев", 95_000.0),
                        StatisticsChartPoint(3, "Мар", 110_000.0),
                        StatisticsChartPoint(4, "Апр", 75_000.0),
                        StatisticsChartPoint(5, "Май", 120_000.0),
                        StatisticsChartPoint(6, "Июн", 90_000.0),
                        StatisticsChartPoint(7, "Июл", 100_000.0),
                        StatisticsChartPoint(8, "Авг", 85_000.0),
                        StatisticsChartPoint(9, "Сен", 105_000.0),
                        StatisticsChartPoint(10, "Окт", 95_000.0),
                        StatisticsChartPoint(11, "Ноя", 115_000.0),
                        StatisticsChartPoint(12, "Дек", 130_000.0),
                    ),
                ),
            ),
            onPeriodChange = {},
            onTransactionTypeChange = {},
            onRetry = {},
        )
    }
}
