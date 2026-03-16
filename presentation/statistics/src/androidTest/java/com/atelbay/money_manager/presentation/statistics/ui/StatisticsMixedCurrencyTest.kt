package com.atelbay.money_manager.presentation.statistics.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayMode
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.domain.statistics.model.CategorySummary
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class StatisticsMixedCurrencyTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun unresolved_mixed_currency_shows_fallback_surfaces() {
        composeRule.setContent {
            MoneyManagerTheme(dynamicColor = false) {
                StatisticsScreen(
                    state = StatisticsState(
                        isLoading = false,
                        displayedExpensesByCategory = persistentListOf(
                            StatisticsCategoryDisplayItem(
                                category = CategorySummary(1L, "Food", "restaurant", 0L, 0.0, 0),
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
                            title = "Expenses by day",
                            dateRangeLabel = "Feb 15 - Mar 16, 2026",
                            points = persistentListOf(
                                StatisticsChartPoint(1L, "15", null),
                                StatisticsChartPoint(2L, "16", null, isToday = true),
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

        composeRule.onNodeWithTag("statistics:pieChartUnavailable").fetchSemanticsNode()
        composeRule.onNodeWithTag("statistics:barChartUnavailable").fetchSemanticsNode()
        composeRule.onNodeWithTag("statistics:category_1").fetchSemanticsNode()
        composeRule.onNodeWithText("Недостаточно курсов для конвертации").fetchSemanticsNode()
    }
}
