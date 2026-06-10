package com.atelbay.money_manager.presentation.budgets.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.model.money.parseToMinorUnitsOrNull
import com.atelbay.money_manager.core.model.money.toMajorPlainString
import com.atelbay.money_manager.domain.budgets.repository.BudgetRepository
import com.atelbay.money_manager.domain.budgets.usecase.SaveBudgetUseCase
import com.atelbay.money_manager.domain.categories.usecase.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val budgetRepository: BudgetRepository,
    private val saveBudgetUseCase: SaveBudgetUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
) : ViewModel() {

    private val budgetId: Long? = savedStateHandle.get<Long>("id")

    private val _state = MutableStateFlow(BudgetEditState(budgetId = budgetId))
    val state: StateFlow<BudgetEditState> = _state.asStateFlow()

    init {
        loadExpenseCategories()
        if (budgetId != null) {
            loadExistingBudget(budgetId)
        }
    }

    private fun loadExpenseCategories() {
        combine(
            getCategoriesUseCase(TransactionType.EXPENSE),
            budgetRepository.observeAll(),
        ) { categories, budgets ->
            // Exclude categories that already have a budget (except the one being edited) so a "new"
            // budget can't silently overwrite an existing one and an edit can't hit the unique index.
            val occupied = budgets.filter { it.id != budgetId }.map { it.categoryId }.toSet()
            categories.filter { it.id !in occupied }
        }
            .onEach { categories ->
                _state.update {
                    it.copy(
                        expenseCategories = categories.toImmutableList(),
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadExistingBudget(id: Long) {
        viewModelScope.launch {
            val budget = budgetRepository.getById(id)
            if (budget != null) {
                _state.update {
                    it.copy(
                        categoryId = budget.categoryId,
                        categoryName = budget.categoryName,
                        categoryIcon = budget.categoryIcon,
                        categoryColor = budget.categoryColor,
                        monthlyLimit = budget.monthlyLimit.toMajorPlainString(),
                    )
                }
            }
        }
    }

    fun selectCategory(category: Category) {
        _state.update {
            it.copy(
                categoryId = category.id,
                categoryName = category.name,
                categoryIcon = category.icon,
                categoryColor = category.color,
                categoryError = null,
                showCategoryPicker = false,
            )
        }
    }

    fun updateLimit(limit: String) {
        val filtered = limit.filter { it.isDigit() || it == '.' }
        _state.update { it.copy(monthlyLimit = filtered, limitError = null) }
    }

    fun toggleCategoryPicker(show: Boolean) {
        _state.update { it.copy(showCategoryPicker = show) }
    }

    fun save(
        onComplete: () -> Unit,
        categoryError: String,
        limitError: String,
    ) {
        val current = _state.value
        var hasError = false

        if (current.categoryId == 0L) {
            _state.update { it.copy(categoryError = categoryError) }
            hasError = true
        }

        val limit = current.monthlyLimit.parseToMinorUnitsOrNull()
        if (limit == null || limit <= 0L) {
            _state.update { it.copy(limitError = limitError) }
            hasError = true
        }

        if (hasError) return

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                saveBudgetUseCase(
                    categoryId = current.categoryId,
                    monthlyLimit = limit!!,
                    budgetId = current.budgetId,
                )
                onComplete()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveError = e.message) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(saveError = null) }
    }
}
