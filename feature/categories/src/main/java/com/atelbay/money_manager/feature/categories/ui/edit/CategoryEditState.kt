package com.atelbay.money_manager.feature.categories.ui.edit

import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CategoryEditState(
    val categoryId: Long? = null,
    val name: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedIcon: String = "more_horiz",
    val selectedColor: Long = 0xFF90A4AE,
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val availableIcons: ImmutableList<String> = persistentListOf(),
    val availableColors: ImmutableList<Long> = persistentListOf(),
) {
    val isEditing: Boolean get() = categoryId != null
}
