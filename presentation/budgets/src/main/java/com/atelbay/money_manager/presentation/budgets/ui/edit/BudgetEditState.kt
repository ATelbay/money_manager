package com.atelbay.money_manager.presentation.budgets.ui.edit

import com.atelbay.money_manager.core.model.Category
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class BudgetEditState(
    val budgetId: Long? = null,
    val categoryId: Long = 0,
    val categoryName: String = "",
    val categoryIcon: String = "",
    val categoryColor: Long = 0L,
    val monthlyLimit: String = "",
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val showCategoryPicker: Boolean = false,
    val expenseCategories: ImmutableList<Category> = persistentListOf(),
    val limitError: String? = null,
    val categoryError: String? = null,
) {
    val isEditing: Boolean get() = budgetId != null
}
