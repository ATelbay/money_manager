package com.atelbay.money_manager.presentation.recurring.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.model.Frequency
import com.atelbay.money_manager.core.model.RecurringTransaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountsUseCase
import com.atelbay.money_manager.domain.categories.usecase.GetCategoriesUseCase
import com.atelbay.money_manager.domain.recurring.repository.RecurringTransactionRepository
import com.atelbay.money_manager.domain.recurring.usecase.SaveRecurringTransactionUseCase
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
class RecurringEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recurringRepository: RecurringTransactionRepository,
    private val saveRecurringTransactionUseCase: SaveRecurringTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
) : ViewModel() {

    private val recurringId: Long? = savedStateHandle.get<Long?>("id")?.takeIf { it != 0L }

    private val _state = MutableStateFlow(RecurringEditState(recurringId = recurringId))
    val state: StateFlow<RecurringEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val accounts = getAccountsUseCase().first()
            val firstAccount = accounts.firstOrNull()
            _state.update {
                it.copy(
                    accounts = accounts.toImmutableList(),
                    accountId = firstAccount?.id ?: 0,
                    accountName = firstAccount?.name.orEmpty(),
                )
            }

            if (recurringId != null) {
                val existing = recurringRepository.getById(recurringId)
                if (existing != null) {
                    val accountForRecurring = accounts.find { a -> a.id == existing.accountId }
                    _state.update {
                        it.copy(
                            amount = existing.amount.toBigDecimal().stripTrailingZeros().toPlainString(),
                            type = existing.type,
                            categoryId = existing.categoryId,
                            categoryName = existing.categoryName,
                            categoryIcon = existing.categoryIcon,
                            categoryColor = existing.categoryColor,
                            accountId = existing.accountId,
                            accountName = accountForRecurring?.name.orEmpty(),
                            frequency = existing.frequency,
                            startDate = existing.startDate,
                            endDate = existing.endDate,
                            dayOfMonth = existing.dayOfMonth ?: 1,
                            dayOfWeek = existing.dayOfWeek ?: 1,
                            note = existing.note.orEmpty(),
                        )
                    }
                    loadCategories(existing.type)
                    return@launch
                }
            }

            loadCategories(TransactionType.EXPENSE)
        }
    }

    private fun loadCategories(type: TransactionType) {
        getCategoriesUseCase(type)
            .onEach { categories ->
                _state.update {
                    it.copy(
                        categories = categories.toImmutableList(),
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateAmount(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        _state.update { it.copy(amount = filtered, amountError = null) }
    }

    fun selectType(type: TransactionType) {
        if (_state.value.type == type) return
        _state.update { it.copy(type = type, categoryId = 0, categoryName = "", categoryIcon = "", categoryError = null) }
        loadCategories(type)
    }

    fun selectCategory(categoryId: Long) {
        val category = _state.value.categories.find { it.id == categoryId } ?: return
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

    fun selectAccount(accountId: Long) {
        val account = _state.value.accounts.find { it.id == accountId } ?: return
        _state.update { it.copy(accountId = account.id, accountName = account.name) }
    }

    fun selectFrequency(frequency: Frequency) {
        _state.update { it.copy(frequency = frequency) }
    }

    fun setStartDate(date: Long) {
        _state.update { it.copy(startDate = date, showStartDatePicker = false) }
    }

    fun setEndDate(date: Long?) {
        _state.update { it.copy(endDate = date, showEndDatePicker = false) }
    }

    fun setDayOfMonth(day: Int) {
        _state.update { it.copy(dayOfMonth = day) }
    }

    fun setDayOfWeek(day: Int) {
        _state.update { it.copy(dayOfWeek = day) }
    }

    fun updateNote(note: String) {
        _state.update { it.copy(note = note) }
    }

    fun toggleCategoryPicker(show: Boolean) {
        _state.update { it.copy(showCategoryPicker = show) }
    }

    fun toggleStartDatePicker(show: Boolean) {
        _state.update { it.copy(showStartDatePicker = show) }
    }

    fun toggleEndDatePicker(show: Boolean) {
        _state.update { it.copy(showEndDatePicker = show) }
    }

    fun save(onComplete: () -> Unit, errorMessage: String) {
        val current = _state.value
        var hasError = false

        val amount = current.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _state.update { it.copy(amountError = errorMessage) }
            hasError = true
        }

        if (current.categoryId == 0L) {
            _state.update { it.copy(categoryError = "Select a category") }
            hasError = true
        }

        if (hasError) return

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                saveRecurringTransactionUseCase(
                    RecurringTransaction(
                        id = current.recurringId ?: 0,
                        amount = amount!!,
                        type = current.type,
                        categoryId = current.categoryId,
                        categoryName = current.categoryName,
                        categoryIcon = current.categoryIcon,
                        categoryColor = current.categoryColor,
                        accountId = current.accountId,
                        accountName = current.accountName,
                        note = current.note.ifBlank { null },
                        frequency = current.frequency,
                        startDate = current.startDate,
                        endDate = current.endDate,
                        dayOfMonth = if (current.frequency == Frequency.MONTHLY) current.dayOfMonth else null,
                        dayOfWeek = if (current.frequency == Frequency.WEEKLY) current.dayOfWeek else null,
                        lastGeneratedDate = null,
                        isActive = true,
                        createdAt = System.currentTimeMillis(),
                    )
                )
                onComplete()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveError = e.message) }
            }
        }
    }
}
