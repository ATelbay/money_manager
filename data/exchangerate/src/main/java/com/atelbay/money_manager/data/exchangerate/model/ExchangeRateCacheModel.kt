package com.atelbay.money_manager.data.exchangerate.model

data class ExchangeRateCacheModel(
    val fetchedAt: Long,
    val source: String?,
    val rates: Map<String, Double>,
)
