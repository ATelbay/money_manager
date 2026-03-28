package com.atelbay.money_manager.presentation.recurring.ui.edit

import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.Frequency
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class RecurringEditState(
    val recurringId: Long? = null,
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: Long = 0,
    val categoryName: String = "",
    val categoryIcon: String = "",
    val categoryColor: Long = 0xFF90A4AEL,
    val accountId: Long = 0,
    val accountName: String = "",
    val frequency: Frequency = Frequency.MONTHLY,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val dayOfMonth: Int = 1,
    val dayOfWeek: Int = 1,
    val note: String = "",
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val showCategoryPicker: Boolean = false,
    val showAccountPicker: Boolean = false,
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val categories: ImmutableList<Category> = persistentListOf(),
    val accounts: ImmutableList<Account> = persistentListOf(),
    val amountError: String? = null,
    val categoryError: String? = null,
    val accountError: String? = null,
    val dateError: String? = null,
    val saveError: String? = null,
) {
    val isEditing: Boolean get() = recurringId != null
}
