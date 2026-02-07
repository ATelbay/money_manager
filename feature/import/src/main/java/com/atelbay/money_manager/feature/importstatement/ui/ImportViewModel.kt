package com.atelbay.money_manager.feature.importstatement.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.entity.AccountEntity
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.ImportState
import com.atelbay.money_manager.core.model.TransactionOverride
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.feature.importstatement.domain.usecase.ImportTransactionsUseCase
import com.atelbay.money_manager.feature.importstatement.domain.usecase.ParseStatementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val parseStatementUseCase: ParseStatementUseCase,
    private val importTransactionsUseCase: ImportTransactionsUseCase,
    private val userPreferences: UserPreferences,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories

    private val _accounts = MutableStateFlow<List<AccountEntity>>(emptyList())
    val accounts: StateFlow<List<AccountEntity>> = _accounts

    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId: StateFlow<Long?> = _selectedAccountId

    init {
        viewModelScope.launch {
            val allAccounts = accountDao.observeAll().first()
            _accounts.value = allAccounts
            val preferred = userPreferences.selectedAccountId.first()
            _selectedAccountId.value = when {
                preferred != null && allAccounts.any { it.id == preferred } -> preferred
                allAccounts.isNotEmpty() -> allAccounts.first().id
                else -> null
            }
        }
    }

    fun selectAccount(accountId: Long) {
        _selectedAccountId.value = accountId
    }

    fun onPdfSelected(uri: Uri) {
        viewModelScope.launch {
            _state.value = ImportState.Parsing
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes == null || bytes.isEmpty()) {
                    _state.value = ImportState.Error("Не удалось прочитать PDF")
                    return@launch
                }
                parseAndPreview(listOf(bytes to "application/pdf"))
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Ошибка чтения PDF")
            }
        }
    }

    fun onPhotoTaken(imageBytes: ByteArray) {
        viewModelScope.launch {
            _state.value = ImportState.Parsing
            try {
                parseAndPreview(listOf(imageBytes to "image/jpeg"))
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    private suspend fun parseAndPreview(blobs: List<Pair<ByteArray, String>>) {
        val result = parseStatementUseCase(blobs)
        if (result.newTransactions.isEmpty() && result.errors.isNotEmpty()) {
            _state.value = ImportState.Error(result.errors.first())
        } else {
            val allCategories = categoryDao.getByType("expense") + categoryDao.getByType("income")
            _categories.value = allCategories
            _state.value = ImportState.Preview(
                result = result,
                overrides = emptyMap(),
            )
        }
    }

    private fun updateOverride(index: Int, update: (TransactionOverride) -> TransactionOverride) {
        val current = _state.value
        if (current is ImportState.Preview) {
            val existing = current.overrides[index] ?: TransactionOverride()
            _state.value = current.copy(
                overrides = current.overrides + (index to update(existing)),
            )
        }
    }

    fun updateAmount(index: Int, amount: Double) {
        updateOverride(index) { it.copy(amount = amount) }
    }

    fun updateType(index: Int, type: TransactionType) {
        updateOverride(index) { it.copy(type = type, categoryId = null) }
    }

    fun updateDetails(index: Int, details: String) {
        updateOverride(index) { it.copy(details = details) }
    }

    fun updateDate(index: Int, date: LocalDate) {
        updateOverride(index) { it.copy(date = date) }
    }

    fun updateCategory(index: Int, categoryId: Long) {
        updateOverride(index) { it.copy(categoryId = categoryId) }
    }

    fun importTransactions() {
        val current = _state.value
        if (current !is ImportState.Preview) return

        val accountId = _selectedAccountId.value
        if (accountId == null) {
            _state.value = ImportState.Error("Выберите счёт для импорта")
            return
        }

        viewModelScope.launch {
            _state.value = ImportState.Importing
            try {
                val imported = importTransactionsUseCase(
                    transactions = current.result.newTransactions,
                    accountId = accountId,
                    overrides = current.overrides,
                )
                _state.value = ImportState.Success(imported)
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Ошибка импорта")
            }
        }
    }

    fun reset() {
        _state.value = ImportState.Idle
    }
}
