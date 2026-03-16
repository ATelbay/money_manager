package com.atelbay.money_manager.core.ui.util

data class AggregateCurrencyDisplayResolution(
    val displayCurrency: String?,
    val displayMode: AggregateCurrencyDisplayMode,
)

enum class AggregateCurrencyDisplayMode {
    CONVERTED,
    ORIGINAL_SINGLE_CURRENCY,
    UNAVAILABLE,
}

object AggregateCurrencyDisplayResolver {
    fun resolve(
        baseCurrency: String,
        scopedCurrencies: Iterable<String>,
        canDisplayInBaseCurrency: Boolean,
    ): AggregateCurrencyDisplayResolution {
        val normalizedBaseCurrency = baseCurrency.normalizeCurrencyCode()
        val normalizedScopedCurrencies = scopedCurrencies
            .map { it.normalizeCurrencyCode() }
            .toSet()

        if (normalizedScopedCurrencies.isEmpty()) {
            return AggregateCurrencyDisplayResolution(
                displayCurrency = normalizedBaseCurrency,
                displayMode = AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY,
            )
        }

        if (canDisplayInBaseCurrency) {
            return AggregateCurrencyDisplayResolution(
                displayCurrency = normalizedBaseCurrency,
                displayMode = AggregateCurrencyDisplayMode.CONVERTED,
            )
        }

        if (normalizedScopedCurrencies.size == 1) {
            return AggregateCurrencyDisplayResolution(
                displayCurrency = normalizedScopedCurrencies.single(),
                displayMode = AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY,
            )
        }

        return AggregateCurrencyDisplayResolution(
            displayCurrency = null,
            displayMode = AggregateCurrencyDisplayMode.UNAVAILABLE,
        )
    }
}
