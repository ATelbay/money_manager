package com.atelbay.money_manager.feature.transactions.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.feature.transactions.domain.usecase.DeleteTransactionUseCase
import com.atelbay.money_manager.feature.transactions.domain.usecase.GetCategoriesUseCase
import com.atelbay.money_manager.feature.transactions.domain.usecase.GetTransactionByIdUseCase
import com.atelbay.money_manager.feature.transactions.domain.usecase.SaveTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val accountDao: AccountDao,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val transactionId: Long? = savedStateHandle.get<Long>("id")

    private val _state = MutableStateFlow(TransactionEditState(transactionId = transactionId))
    val state: StateFlow<TransactionEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Load selected account (or fallback to first)
            val selectedId = userPreferences.selectedAccountId.first()
            val accounts = accountDao.observeAll().first()
            val account = if (selectedId != null) {
                accounts.find { it.id == selectedId }
            } else {
                accounts.firstOrNull()
            }
            if (account != null) {
                _state.update { it.copy(accountId = account.id) }
            }

            // Load existing transaction if editing
            if (transactionId != null) {
                val transaction = getTransactionByIdUseCase(transactionId).first()
                if (transaction != null) {
                    _state.update {
                        it.copy(
                            type = transaction.type,
                            amount = transaction.amount.toBigDecimal().stripTrailingZeros().toPlainString(),
                            date = transaction.date,
                            note = transaction.note.orEmpty(),
                            accountId = transaction.accountId,
                        )
                    }
                    // Load categories for this type then select the right one
                    loadCategoriesAndSelect(transaction.type, transaction.categoryId)
                    return@launch
                }
            }

            // Default: load expense categories
            loadCategories(TransactionType.EXPENSE)
        }
    }

    private suspend fun loadCategoriesAndSelect(type: TransactionType, categoryId: Long) {
        val categories = getCategoriesUseCase(type).first()
        _state.update {
            it.copy(
                categories = categories.toImmutableList(),
                selectedCategory = categories.find { c -> c.id == categoryId },
                isLoading = false,
            )
        }
    }

    private fun loadCategories(type: TransactionType) {
        getCategoriesUseCase(type)
            .onEach { categories ->
                _state.update {
                    it.copy(
                        categories = categories.toImmutableList(),
                        selectedCategory = if (it.selectedCategory?.type != type) null else it.selectedCategory,
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setType(type: TransactionType) {
        if (_state.value.type == type) return
        _state.update { it.copy(type = type, selectedCategory = null) }
        loadCategories(type)
    }

    fun setAmount(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        _state.update { it.copy(amount = filtered, amountError = null) }
    }

    fun selectCategory(category: Category) {
        _state.update { it.copy(selectedCategory = category, categoryError = null, showCategorySheet = false) }
    }

    fun setDate(date: Long) {
        _state.update { it.copy(date = date, showDatePicker = false) }
    }

    fun setNote(note: String) {
        _state.update { it.copy(note = note) }
    }

    fun toggleCategorySheet(show: Boolean) {
        _state.update { it.copy(showCategorySheet = show) }
    }

    fun toggleDatePicker(show: Boolean) {
        _state.update { it.copy(showDatePicker = show) }
    }

    fun deleteTransaction(onComplete: () -> Unit) {
        val id = transactionId ?: return
        viewModelScope.launch {
            deleteTransactionUseCase(id)
            onComplete()
        }
    }

    fun save(onComplete: () -> Unit) {
        val current = _state.value
        var hasError = false

        val amount = current.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _state.update { it.copy(amountError = "Введите корректную сумму") }
            hasError = true
        }

        if (current.selectedCategory == null) {
            _state.update { it.copy(categoryError = "Выберите категорию") }
            hasError = true
        }

        if (hasError) return

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            saveTransactionUseCase(
                Transaction(
                    id = current.transactionId ?: 0,
                    amount = amount!!,
                    type = current.type,
                    categoryId = current.selectedCategory!!.id,
                    categoryName = current.selectedCategory.name,
                    categoryIcon = current.selectedCategory.icon,
                    categoryColor = current.selectedCategory.color,
                    accountId = current.accountId,
                    note = current.note.ifBlank { null },
                    date = current.date,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            onComplete()
        }
    }
}
