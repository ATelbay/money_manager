package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayMode
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.core.ui.util.MoneyDisplayPresentation

data class StatisticsCurrencyUiState(
    val moneyDisplay: MoneyDisplayPresentation = MoneyDisplayFormatter.format(
        MoneyDisplayFormatter.unavailable(),
    ),
    val displayMode: AggregateCurrencyDisplayMode = AggregateCurrencyDisplayMode.UNAVAILABLE,
) {
    val isUnavailable: Boolean
        get() = displayMode == AggregateCurrencyDisplayMode.UNAVAILABLE
}
