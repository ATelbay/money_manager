package com.atelbay.money_manager.presentation.statistics.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.components.MoneyManagerChip
import com.atelbay.money_manager.core.ui.components.StatType
import com.atelbay.money_manager.core.ui.components.SummaryStatCard
import com.atelbay.money_manager.core.ui.theme.MoneyManagerMotion
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal
import com.atelbay.money_manager.core.ui.util.LocalReduceMotion
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.core.ui.util.MoneyDisplayPresentation
import com.atelbay.money_manager.core.ui.util.defaultMoneyNumberFormat
import com.atelbay.money_manager.core.ui.util.formatAmount
import com.atelbay.money_manager.domain.statistics.model.CategorySummary
import com.atelbay.money_manager.domain.statistics.model.DailyTotal
import com.atelbay.money_manager.domain.statistics.model.MonthlyTotal
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
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
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.core.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.Shape
import android.graphics.Paint
import android.text.Layout
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.ColumnCartesianLayerMarkerTarget
import java.text.DecimalFormat

// ExtraStore keys for Vico chart metadata
internal val xToLabelMapKey = ExtraStore.Key<Map<Double, String>>()
internal val xToDateStringKey = ExtraStore.Key<Map<Double, String>>()
internal val todayIndexKey = ExtraStore.Key<Int>()
internal val currencySymbolKey = ExtraStore.Key<String>()
internal val currencyPrefixKey = ExtraStore.Key<Boolean>()

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

private class TodayDotDecoration(
    private val dotColor: Int,
    private val dotRadiusDp: Float = 3f,
) : Decoration {
    override fun drawOverLayers(context: CartesianDrawingContext) {
        with(context) {
            val todayIndex = model.extraStore.getOrNull(todayIndexKey) ?: return
            if (todayIndex < 0) return

            val todayX = todayIndex.toDouble()
            val layerStart = if (isLtr) layerBounds.left else layerBounds.right
            val drawingStart =
                layerStart + layoutDirectionMultiplier * layerDimensions.startPadding
            val xSpacingMultiplier =
                ((todayX - ranges.minX) / ranges.xStep).toFloat()
            val canvasX = drawingStart +
                layoutDirectionMultiplier * (layerDimensions.xSpacing * xSpacingMultiplier - scroll)
            val canvasY = layerBounds.top - dotRadiusDp * density * 2
            val dotRadius = dotRadiusDp * density

            if (canvasX < layerBounds.left - dotRadius || canvasX > layerBounds.right + dotRadius) {
                return
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = dotColor
                style = Paint.Style.FILL
            }
            canvas.drawCircle(canvasX, canvasY, dotRadius, paint)
        }
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

    Scaffold(
        modifier = modifier.testTag("statistics:screen"),
        contentWindowInsets = contentWindowInsets,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = s.statisticsTitle,
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
            state.expensesByCategory.isEmpty()
        } else {
            state.incomesByCategory.isEmpty()
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

            // Summary Cards
            item(key = "totals") {
                StatisticsTypeCards(
                    state = state,
                    onTransactionTypeChange = onTransactionTypeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            // Donut Chart
            if (currentCategories.isNotEmpty()) {
                item(key = "donut") {
                    DonutChartCard(
                        categories = currentCategories,
                        totalAmount = currentTotal,
                        moneyDisplay = state.currencyUiState.moneyDisplay,
                        centerLabel = if (isExpense) s.expensesLabel else s.incomeLabel,
                        isUnavailable = state.currencyUiState.isUnavailable,
                        unavailableText = s.mixedCurrencyUnavailable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("statistics:pieChart"),
                    )
                }
            }

            // Bar Chart
            if (state.chart.points.isNotEmpty()) {
                item(key = "bar") {
                    VicoBarChartSection(
                        chart = state.chart,
                        modelProducer = chartModelProducer,
                        barColor = if (isExpense) {
                            colors.expense.copy(alpha = 0.8f)
                        } else {
                            colors.income.copy(alpha = 0.8f)
                        },
                        isUnavailable = state.currencyUiState.isUnavailable,
                        unavailableText = s.mixedCurrencyUnavailable,
                        period = state.period,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            // Category Breakdown
            if (currentCategories.isNotEmpty()) {
                item(key = "breakdown") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = s.byCategory,
                            style = typography.caption,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                        CategoryBreakdownCard(
                            categories = currentCategories,
                            moneyDisplay = state.currencyUiState.moneyDisplay,
                            isUnavailable = state.currencyUiState.isUnavailable,
                            unavailableText = s.mixedCurrencyUnavailable,
                            onCategoryClick = onCategoryClick,
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
        val strings = MoneyManagerTheme.strings
        StatsPeriod.entries.forEachIndexed { index, period ->
            if (index > 0) Spacer(modifier = Modifier.width(8.dp))
            MoneyManagerChip(
                label = when (period) {
                    StatsPeriod.WEEK -> strings.periodWeek
                    StatsPeriod.MONTH -> strings.periodMonth
                    StatsPeriod.YEAR -> strings.periodYear
                },
                selected = selected == period,
                onClick = { onSelect(period) },
                modifier = Modifier.testTag("statistics:period_${period.name}"),
            )
        }
    }
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

// ── Donut Chart Card ──

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DonutChartCard(
    categories: ImmutableList<StatisticsCategoryDisplayItem>,
    totalAmount: Double?,
    moneyDisplay: MoneyDisplayPresentation,
    centerLabel: String,
    isUnavailable: Boolean,
    unavailableText: String,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isUnavailable || totalAmount == null) {
                StatisticsUnavailableCard(
                    text = unavailableText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .testTag("statistics:pieChartUnavailable"),
                )
            } else {
                DonutChart(
                    categories = categories,
                    totalAmount = totalAmount,
                    moneyDisplay = moneyDisplay,
                    centerLabel = centerLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                                .background(Color(category.category.categoryColor)),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = category.category.categoryName,
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
    categories: ImmutableList<StatisticsCategoryDisplayItem>,
    totalAmount: Double,
    moneyDisplay: MoneyDisplayPresentation,
    centerLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val formatter = remember { DecimalFormat("#,##0") }

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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = typography.caption,
                color = colors.textSecondary,
            )
            Text(
                text = moneyDisplay.formatAmount(
                    amount = totalAmount,
                    formatter = formatter,
                ),
                style = typography.sectionHeader,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 20.sp, stepSize = 1.sp),
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
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Column(modifier = modifier) {
        Text(
            text = chart.title,
            style = typography.caption,
            color = colors.textSecondary,
            modifier = Modifier
                .padding(start = 4.dp, bottom = 4.dp)
                .testTag("statistics:chartTitle"),
        )
        Text(
            text = chart.dateRangeLabel,
            style = typography.caption,
            color = colors.textTertiary,
            modifier = Modifier
                .padding(start = 4.dp, bottom = 8.dp)
                .testTag("statistics:chartDateRange"),
        )

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
                        context.model.extraStore.getOrNull(xToLabelMapKey)?.get(value) ?: ""
                    }
                }

                val scrollState = rememberVicoScrollState(
                    scrollEnabled = period == StatsPeriod.MONTH,
                    initialScroll = if (period == StatsPeriod.MONTH) {
                        Scroll.Absolute.End
                    } else {
                        Scroll.Absolute.Start
                    },
                )
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

                val dotColor = barColor
                val todayDotDecoration = remember(dotColor) {
                    TodayDotDecoration(
                        dotColor = android.graphics.Color.argb(
                            (dotColor.alpha * 255).toInt(),
                            (dotColor.red * 255).toInt(),
                            (dotColor.green * 255).toInt(),
                            (dotColor.blue * 255).toInt(),
                        ),
                    )
                }

                val marker = rememberDefaultCartesianMarker(
                    label = rememberTextComponent(
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

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = columnProvider,
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
                        decorations = listOf(todayDotDecoration),
                    ),
                    modelProducer = modelProducer,
                    scrollState = scrollState,
                    zoomState = zoomState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(16.dp)
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

// ── Category Breakdown ──

@Composable
private fun CategoryBreakdownCard(
    categories: ImmutableList<StatisticsCategoryDisplayItem>,
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

    GlassCard(modifier = modifier) {
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
                    // Color dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(summary.category.categoryColor)),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Category name
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
                totalExpenses = 85_000.0,
                totalIncome = 200_000.0,
                displayedTotalExpenses = 85_000.0,
                displayedTotalIncome = 200_000.0,
                expensesByCategory = persistentListOf(
                    CategorySummary(1, "Еда", "restaurant", 0xFFFF6B6B, 35_000.0, 41),
                    CategorySummary(2, "Транспорт", "directions_car", 0xFF4ECDC4, 20_000.0, 24),
                    CategorySummary(3, "Развлечения", "sports_esports", 0xFF45B7D1, 15_000.0, 18),
                    CategorySummary(4, "Покупки", "shopping_bag", 0xFFF7B801, 15_000.0, 17),
                ),
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
                dailyExpenses = persistentListOf(
                    DailyTotal(1738800000000, 5000.0),
                    DailyTotal(1738886400000, 3000.0),
                    DailyTotal(1738972800000, 8000.0),
                    DailyTotal(1739059200000, 2000.0),
                    DailyTotal(1739145600000, 12000.0),
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
                    title = "Expenses by day",
                    dateRangeLabel = "Feb 6 - Feb 10, 2025",

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
                totalExpenses = 85_000.0,
                totalIncome = 200_000.0,
                displayedTotalExpenses = 85_000.0,
                displayedTotalIncome = 200_000.0,
                incomesByCategory = persistentListOf(
                    CategorySummary(1, "Зарплата", "payments", 0xFF4ECDC4, 150_000.0, 75),
                    CategorySummary(2, "Фриланс", "work", 0xFF45B7D1, 50_000.0, 25),
                ),
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
                dailyIncome = persistentListOf(
                    DailyTotal(1738800000000, 10000.0),
                    DailyTotal(1738886400000, 5000.0),
                    DailyTotal(1738972800000, 15000.0),
                    DailyTotal(1739059200000, 0.0),
                    DailyTotal(1739145600000, 20000.0),
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
                    title = "Income by day",
                    dateRangeLabel = "Feb 6 - Feb 10, 2025",

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
                totalExpenses = 1_200_000.0,
                totalIncome = 2_400_000.0,
                displayedTotalExpenses = 1_200_000.0,
                displayedTotalIncome = 2_400_000.0,
                expensesByCategory = persistentListOf(
                    CategorySummary(1, "Еда", "restaurant", 0xFFFF6B6B, 500_000.0, 42),
                    CategorySummary(2, "Транспорт", "directions_car", 0xFF4ECDC4, 300_000.0, 25),
                    CategorySummary(3, "Развлечения", "sports_esports", 0xFF45B7D1, 200_000.0, 17),
                    CategorySummary(4, "Покупки", "shopping_bag", 0xFFF7B801, 200_000.0, 16),
                ),
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
                monthlyExpenses = persistentListOf(
                    MonthlyTotal(2025, 1, 80_000.0, "Янв"),
                    MonthlyTotal(2025, 2, 95_000.0, "Фев"),
                    MonthlyTotal(2025, 3, 110_000.0, "Мар"),
                    MonthlyTotal(2025, 4, 75_000.0, "Апр"),
                    MonthlyTotal(2025, 5, 120_000.0, "Май"),
                    MonthlyTotal(2025, 6, 90_000.0, "Июн"),
                    MonthlyTotal(2025, 7, 100_000.0, "Июл"),
                    MonthlyTotal(2025, 8, 85_000.0, "Авг"),
                    MonthlyTotal(2025, 9, 105_000.0, "Сен"),
                    MonthlyTotal(2025, 10, 95_000.0, "Окт"),
                    MonthlyTotal(2025, 11, 115_000.0, "Ноя"),
                    MonthlyTotal(2025, 12, 130_000.0, "Дек"),
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
                    title = "Expenses by month",
                    dateRangeLabel = "2025",

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
