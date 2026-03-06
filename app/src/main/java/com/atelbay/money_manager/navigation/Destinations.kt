package com.atelbay.money_manager.navigation

import com.atelbay.money_manager.presentation.settings.ui.CurrencyPickerSide
import kotlinx.serialization.Serializable

@Serializable
data object Onboarding

@Serializable
data object CreateAccount

@Serializable
data object Home

@Serializable
data class TransactionEdit(val id: Long? = null)

@Serializable
data object CategoryList

@Serializable
data class CategoryEdit(val id: Long? = null)

@Serializable
data object Statistics

@Serializable
data object AccountList

@Serializable
data class AccountEdit(val id: Long? = null)

@Serializable
data object Settings

@Serializable
data object Import

@Serializable
data class CurrencyPicker(
    val activeSide: CurrencyPickerSide = CurrencyPickerSide.BASE,
)

@Serializable
data object SignIn
