package com.atelbay.money_manager.presentation.statistics.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
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

@RunWith(AndroidJUnit4::class)
class StatisticsChartRenderTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun chart_title_and_date_range_are_displayed_when_data_is_available() {
        composeRule.setContent {
            MoneyManagerTheme(dynamicColor = false) {
                StatisticsScreen(
                    state = StatisticsState(
                        isLoading = false,
                        period = StatsPeriod.MONTH,
                        transactionType = TransactionType.EXPENSE,
                        displayedTotalExpenses = 600.0,
                        displayedExpensesByCategory = persistentListOf(
                            StatisticsCategoryDisplayItem(
                                category = CategorySummary(1L, "Food", "restaurant", 0L, 600.0, 100),
                                displayAmount = 600.0,
                                displayPercentage = 100,
                            ),
                        ),
                        currencyUiState = StatisticsCurrencyUiState(
                            moneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                            displayMode = AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY,
                        ),
                        chart = StatisticsChartState(
                            isScrollable = false,
                            points = persistentListOf(
                                StatisticsChartPoint(1L, "1", 100.0),
                                StatisticsChartPoint(2L, "2", 200.0),
                                StatisticsChartPoint(3L, "3", 300.0, isToday = true),
                            ),
                        ),
                    ),
                    onPeriodChange = {},
                    onTransactionTypeChange = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithTag("statistics:chartTitle").assertIsDisplayed()
        composeRule.onNodeWithTag("statistics:chartTitle").assertTextEquals("Expenses by day")
        composeRule.onNodeWithTag("statistics:chartDateRange").assertIsDisplayed()
        composeRule.onNodeWithTag("statistics:chartDateRange").assertTextEquals("Mar 1 - Mar 31, 2026")
    }

    @Test
    fun bar_chart_unavailable_overlay_shown_when_currency_is_unavailable() {
        composeRule.setContent {
            MoneyManagerTheme(dynamicColor = false) {
                StatisticsScreen(
                    state = StatisticsState(
                        isLoading = false,
                        period = StatsPeriod.MONTH,
                        transactionType = TransactionType.EXPENSE,
                        displayedExpensesByCategory = persistentListOf(
                            StatisticsCategoryDisplayItem(
                                category = CategorySummary(1L, "Transport", "directions_bus", 0L, 0.0, 0),
                                displayAmount = null,
                            ),
                        ),
                        currencyUiState = StatisticsCurrencyUiState(
                            moneyDisplay = MoneyDisplayFormatter.format(
                                MoneyDisplayFormatter.unavailable(),
                            ),
                            displayMode = AggregateCurrencyDisplayMode.UNAVAILABLE,
                        ),
                        chart = StatisticsChartState(
                            isScrollable = false,
                            points = persistentListOf(
                                StatisticsChartPoint(1L, "1", null),
                                StatisticsChartPoint(2L, "2", null),
                                StatisticsChartPoint(3L, "3", null, isToday = true),
                            ),
                        ),
                    ),
                    onPeriodChange = {},
                    onTransactionTypeChange = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithTag("statistics:barChartUnavailable").assertExists()
    }

    @Test
    fun bar_chart_unavailable_overlay_not_shown_when_data_is_available() {
        composeRule.setContent {
            MoneyManagerTheme(dynamicColor = false) {
                StatisticsScreen(
                    state = StatisticsState(
                        isLoading = false,
                        period = StatsPeriod.MONTH,
                        transactionType = TransactionType.EXPENSE,
                        displayedTotalExpenses = 450.0,
                        displayedExpensesByCategory = persistentListOf(
                            StatisticsCategoryDisplayItem(
                                category = CategorySummary(2L, "Shopping", "shopping_cart", 0L, 450.0, 100),
                                displayAmount = 450.0,
                                displayPercentage = 100,
                            ),
                        ),
                        currencyUiState = StatisticsCurrencyUiState(
                            moneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                            displayMode = AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY,
                        ),
                        chart = StatisticsChartState(
                            isScrollable = false,
                            points = persistentListOf(
                                StatisticsChartPoint(1L, "1", 150.0),
                                StatisticsChartPoint(2L, "2", 150.0),
                                StatisticsChartPoint(3L, "3", 150.0),
                            ),
                        ),
                    ),
                    onPeriodChange = {},
                    onTransactionTypeChange = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithTag("statistics:barChartUnavailable").assertDoesNotExist()
    }
}
