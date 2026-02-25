package com.atelbay.money_manager.presentation.transactions.ui.edit

import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class TransactionEditState(
    val transactionId: Long? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val selectedCategory: Category? = null,
    val categories: ImmutableList<Category> = persistentListOf(),
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val accountId: Long = 0,
    val amountError: String? = null,
    val categoryError: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val showCategorySheet: Boolean = false,
    val showDatePicker: Boolean = false,
) {
    val isEditing: Boolean get() = transactionId != null
}
