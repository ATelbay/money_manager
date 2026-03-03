package com.atelbay.money_manager.data.exchangerate.model

data class ExchangeRateCacheModel(
    val usdToKzt: Double,
    val fetchedAt: Long,
    val source: String?,
)
