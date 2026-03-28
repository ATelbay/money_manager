package com.atelbay.money_manager.navigation

import com.atelbay.money_manager.presentation.settings.ui.CurrencyPickerSide
import kotlinx.serialization.Serializable

@Serializable
data object Onboarding

@Serializable
data object OnboardingSetup

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
data class StatisticsCategoryTransactions(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val transactionType: String,
    val period: String,
    val startMillis: Long,
    val endMillis: Long,
)

@Serializable
data object AccountList

@Serializable
data class AccountEdit(val id: Long? = null)

@Serializable
data object Settings

@Serializable
data class Import(val pdfUri: String? = null)

@Serializable
data class CurrencyPicker(
    val activeSide: CurrencyPickerSide = CurrencyPickerSide.BASE,
)

@Serializable
data object SignIn
