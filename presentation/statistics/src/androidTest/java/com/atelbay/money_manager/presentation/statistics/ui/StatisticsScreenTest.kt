package com.atelbay.money_manager.presentation.statistics.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayMode
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.domain.statistics.model.CategorySummary
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class StatisticsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun month_chart_renders_title_range_and_scroll_container() {
        composeRule.setContent {
            MoneyManagerTheme(dynamicColor = false) {
                StatisticsScreen(
                    state = StatisticsState(
                        isLoading = false,
                        period = StatsPeriod.MONTH,
                        transactionType = TransactionType.EXPENSE,
                        displayedTotalExpenses = 300.0,
                        displayedExpensesByCategory = persistentListOf(
                            StatisticsCategoryDisplayItem(
                                category = CategorySummary(1L, "Food", "restaurant", 0L, 300.0, 100),
                                displayAmount = 300.0,
                                displayPercentage = 100,
                            ),
                        ),
                        currencyUiState = StatisticsCurrencyUiState(
                            moneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                            displayMode = AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY,
                        ),
                        chart = StatisticsChartState(
                            isScrollable = true,
                            points = persistentListOf(
                                StatisticsChartPoint(1L, "14", 100.0),
                                StatisticsChartPoint(2L, "15", 200.0),
                                StatisticsChartPoint(3L, "16", 300.0, isToday = true),
                            ),
                        ),
                    ),
                    onPeriodChange = {},
                    onTransactionTypeChange = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithTag("statistics:chartTitle").assertTextEquals("Expenses by day")
        composeRule.onNodeWithTag("statistics:chartDateRange").assertTextEquals("Feb 15 - Mar 16, 2026")
        composeRule.onNodeWithTag("statistics:monthChartContainer").assert(hasScrollAction())
    }
}
