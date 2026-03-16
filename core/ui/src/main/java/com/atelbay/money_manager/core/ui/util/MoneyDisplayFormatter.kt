package com.atelbay.money_manager.core.ui.util

import java.text.NumberFormat
import java.util.Locale

enum class CurrencyDisplayContextMode {
    RESOLVED_SINGLE,
    RESOLVED_SINGLE_AMBIGUOUS,
    UNAVAILABLE,
}

enum class MoneyDisplayMode {
    SYMBOL_FIRST,
    SYMBOL_PLUS_CODE,
    CODE_ONLY,
    UNAVAILABLE,
}

data class CurrencyDisplayContext(
    val mode: CurrencyDisplayContextMode,
    val displayCurrencyCode: String? = null,
    val displayCurrencySymbol: String? = null,
    val fallbackLabelMode: MoneyDisplayMode,
    val reason: String? = null,
) {
    init {
        if (mode == CurrencyDisplayContextMode.UNAVAILABLE) {
            require(fallbackLabelMode == MoneyDisplayMode.UNAVAILABLE) {
                "Unavailable currency context must use unavailable display mode."
            }
        } else {
            require(!displayCurrencyCode.isNullOrBlank()) {
                "Resolved currency contexts require a currency code."
            }
        }

        if (fallbackLabelMode == MoneyDisplayMode.SYMBOL_FIRST ||
            fallbackLabelMode == MoneyDisplayMode.SYMBOL_PLUS_CODE
        ) {
            require(!displayCurrencySymbol.isNullOrBlank()) {
                "Symbol-based display modes require a currency symbol."
            }
        }
    }
}

data class MoneyDisplayPresentation(
    val primaryLabel: String,
    val secondaryLabel: String? = null,
    val displayMode: MoneyDisplayMode,
)

val MoneyDisplayPresentation.isUnavailable: Boolean
    get() = displayMode == MoneyDisplayMode.UNAVAILABLE

fun MoneyDisplayPresentation.inlineCurrencyLabel(): String =
    when (displayMode) {
        MoneyDisplayMode.SYMBOL_FIRST,
        MoneyDisplayMode.CODE_ONLY,
        MoneyDisplayMode.UNAVAILABLE,
        -> primaryLabel

        MoneyDisplayMode.SYMBOL_PLUS_CODE -> buildString {
            append(primaryLabel)
            secondaryLabel?.takeIf { it.isNotBlank() }?.let {
                append(' ')
                append(it)
            }
        }
    }

fun MoneyDisplayPresentation.formatAmount(
    amount: Double,
    sign: String? = null,
    formatter: NumberFormat = defaultMoneyNumberFormat(),
): String {
    if (isUnavailable) return primaryLabel

    return buildString {
        sign?.takeIf { it.isNotEmpty() }?.let(::append)
        append(inlineCurrencyLabel())
        append(' ')
        append(formatter.format(amount))
    }
}

fun MoneyDisplayPresentation.supportingText(fallback: String? = null): String? =
    secondaryLabel ?: fallback

fun defaultMoneyNumberFormat(): NumberFormat =
    NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

object MoneyDisplayFormatter {

    fun resolve(currencyCode: String): CurrencyDisplayContext {
        val normalizedCode = currencyCode.normalizeCurrencyCode()
        val supportedCurrency = SupportedCurrencyDisplayRegistry.lookup(normalizedCode)

        return when {
            supportedCurrency == null -> CurrencyDisplayContext(
                mode = CurrencyDisplayContextMode.RESOLVED_SINGLE_AMBIGUOUS,
                displayCurrencyCode = normalizedCode,
                fallbackLabelMode = MoneyDisplayMode.CODE_ONLY,
            )

            supportedCurrency.isAmbiguous -> CurrencyDisplayContext(
                mode = CurrencyDisplayContextMode.RESOLVED_SINGLE_AMBIGUOUS,
                displayCurrencyCode = normalizedCode,
                displayCurrencySymbol = supportedCurrency.symbol,
                fallbackLabelMode = MoneyDisplayMode.SYMBOL_PLUS_CODE,
            )

            else -> CurrencyDisplayContext(
                mode = CurrencyDisplayContextMode.RESOLVED_SINGLE,
                displayCurrencyCode = normalizedCode,
                displayCurrencySymbol = supportedCurrency.symbol,
                fallbackLabelMode = MoneyDisplayMode.SYMBOL_FIRST,
            )
        }
    }

    fun unavailable(reason: String? = null): CurrencyDisplayContext =
        CurrencyDisplayContext(
            mode = CurrencyDisplayContextMode.UNAVAILABLE,
            fallbackLabelMode = MoneyDisplayMode.UNAVAILABLE,
            reason = reason?.takeUnless { it.isBlank() },
        )

    fun format(context: CurrencyDisplayContext): MoneyDisplayPresentation =
        when (context.fallbackLabelMode) {
            MoneyDisplayMode.SYMBOL_FIRST -> MoneyDisplayPresentation(
                primaryLabel = requireNotNull(context.displayCurrencySymbol),
                displayMode = MoneyDisplayMode.SYMBOL_FIRST,
            )

            MoneyDisplayMode.SYMBOL_PLUS_CODE -> MoneyDisplayPresentation(
                primaryLabel = requireNotNull(context.displayCurrencySymbol),
                secondaryLabel = requireNotNull(context.displayCurrencyCode),
                displayMode = MoneyDisplayMode.SYMBOL_PLUS_CODE,
            )

            MoneyDisplayMode.CODE_ONLY -> MoneyDisplayPresentation(
                primaryLabel = requireNotNull(context.displayCurrencyCode),
                displayMode = MoneyDisplayMode.CODE_ONLY,
            )

            MoneyDisplayMode.UNAVAILABLE -> MoneyDisplayPresentation(
                primaryLabel = UNAVAILABLE_LABEL,
                secondaryLabel = context.reason,
                displayMode = MoneyDisplayMode.UNAVAILABLE,
            )
        }

    fun resolveAndFormat(currencyCode: String): MoneyDisplayPresentation = format(resolve(currencyCode))

    private const val UNAVAILABLE_LABEL = "-"
}
