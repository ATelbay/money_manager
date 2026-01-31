package com.atelbay.money_manager.feature.categories.ui.list

import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CategoryListState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val expenseCategories: ImmutableList<Category> = persistentListOf(),
    val incomeCategories: ImmutableList<Category> = persistentListOf(),
    val isLoading: Boolean = true,
)
