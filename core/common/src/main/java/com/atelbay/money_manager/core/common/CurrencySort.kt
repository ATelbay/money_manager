package com.atelbay.money_manager.core.common

private val PRIORITY_ORDER = listOf(
    // Reserve currencies
    "USD", "EUR", "GBP", "JPY", "CNY", "CHF", "AUD", "CAD",
    // Regional
    "KZT", "RUB", "UAH", "UZS", "KGS", "GEL", "TRY",
)

private val PRIORITY_INDEX = PRIORITY_ORDER.withIndex().associate { (i, code) -> code to i }

fun buildSortedCurrencyList(apiCodes: Set<String>): List<String> {
    val allCodes = apiCodes + "KZT" // KZT is always available as the base currency
    return allCodes.sortedWith(compareBy<String> { PRIORITY_INDEX[it] ?: Int.MAX_VALUE }.thenBy { it })
}
