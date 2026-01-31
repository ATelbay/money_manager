package com.atelbay.money_manager.feature.categories.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.feature.categories.domain.usecase.DeleteCategoryUseCase
import com.atelbay.money_manager.feature.categories.domain.usecase.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    getCategoriesUseCase: GetCategoriesUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryListState())
    val state: StateFlow<CategoryListState> = _state.asStateFlow()

    init {
        combine(
            getCategoriesUseCase(TransactionType.EXPENSE),
            getCategoriesUseCase(TransactionType.INCOME),
        ) { expenses, incomes ->
            _state.update {
                it.copy(
                    expenseCategories = expenses.toImmutableList(),
                    incomeCategories = incomes.toImmutableList(),
                    isLoading = false,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun selectType(type: TransactionType) {
        _state.update { it.copy(selectedType = type) }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            deleteCategoryUseCase(id)
        }
    }
}
