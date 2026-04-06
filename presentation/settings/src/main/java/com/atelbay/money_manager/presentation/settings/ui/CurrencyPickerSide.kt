package com.atelbay.money_manager.presentation.settings.ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CurrencyPickerSide {
    @SerialName("BASE")
    FIRST,
    @SerialName("TARGET")
    SECOND,
}
