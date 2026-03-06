package com.atelbay.money_manager.data.exchangerate.model

data class ExchangeRateCacheModel(
    val quotes: Map<String, Double>,
    val fetchedAt: Long,
    val source: String?,
)
