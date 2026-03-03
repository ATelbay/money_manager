package com.atelbay.money_manager.data.exchangerate.model

data class NbkExchangeRateRemoteModel(
    val usdToKzt: Double,
    val source: String = "NBK",
)
