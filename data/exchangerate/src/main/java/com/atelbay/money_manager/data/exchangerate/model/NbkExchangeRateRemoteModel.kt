package com.atelbay.money_manager.data.exchangerate.model

data class NbkExchangeRateRemoteModel(
    val rates: Map<String, Double>,
    val source: String = "NBK",
)
