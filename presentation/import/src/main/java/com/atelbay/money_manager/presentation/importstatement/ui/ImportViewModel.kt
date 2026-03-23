package com.atelbay.money_manager.presentation.importstatement.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.ImportState
import com.atelbay.money_manager.core.model.TransactionOverride
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountsUseCase
import com.atelbay.money_manager.domain.auth.repository.AuthRepository
import com.atelbay.money_manager.domain.categories.usecase.GetCategoriesUseCase
import com.atelbay.money_manager.domain.importstatement.usecase.ImportProgressCollector
import com.atelbay.money_manager.domain.importstatement.usecase.ImportStepEvent
import com.atelbay.money_manager.domain.importstatement.usecase.ImportTransactionsUseCase
import com.atelbay.money_manager.domain.importstatement.usecase.ParseStatementUseCase
import com.atelbay.money_manager.domain.importstatement.usecase.SubmitParserCandidateUseCase
import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import com.atelbay.money_manager.core.ui.theme.AppStrings
import com.atelbay.money_manager.domain.importstatement.usecase.AiMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val parseStatementUseCase: ParseStatementUseCase,
    private val importTransactionsUseCase: ImportTransactionsUseCase,
    private val userPreferences: UserPreferences,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val submitParserCandidateUseCase: SubmitParserCandidateUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts

    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId: StateFlow<Long?> = _selectedAccountId

    /** Debug-only: emits a message when AI fallback is used. */
    private val _debugAiEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val debugAiEvent = _debugAiEvent.asSharedFlow()

    /** Debug progress collector — always created, only consumed in debug UI. */
    val debugCollector = ListImportProgressCollector()

    /** Tracks the AI-generated config from the last parse, if any. */
    private var lastAiGeneratedConfig: ParserConfig? = null
    private var lastSampleRows: String? = null

    init {
        viewModelScope.launch {
            val allAccounts = getAccountsUseCase().first()
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

    fun onPdfSelected(bytes: ByteArray, strings: AppStrings) {
        viewModelScope.launch {
            _state.value = ImportState.Parsing
            try {
                parseAndPreview(listOf(bytes to "application/pdf"), strings)
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: strings.errorReadingPdf)
            }
        }
    }

    fun onPhotoTaken(imageBytes: ByteArray, strings: AppStrings) {
        viewModelScope.launch {
            _state.value = ImportState.Parsing
            try {
                parseAndPreview(listOf(imageBytes to "image/jpeg"), strings)
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: strings.errorUnknown)
            }
        }
    }

    private suspend fun parseAndPreview(blobs: List<Pair<ByteArray, String>>, strings: AppStrings) {
        debugCollector.clear()
        val parseResult = parseStatementUseCase(blobs, debugCollector)
        val result = parseResult.importResult
        lastAiGeneratedConfig = parseResult.aiGeneratedConfig
        lastSampleRows = parseResult.sampleRows

        when (parseResult.aiMethod) {
            AiMethod.REGEX_GENERATED -> _debugAiEvent.tryEmit(
                "AI regex generated for: ${parseResult.aiGeneratedConfig?.bankId}"
            )
            AiMethod.TABLE_GENERATED -> _debugAiEvent.tryEmit(
                "AI table config generated for: ${parseResult.aiGeneratedTableConfig?.bankId}"
            )
            AiMethod.FULL_PARSE -> _debugAiEvent.tryEmit("AI full parse (Gemini)")
            AiMethod.NONE -> { /* regex matched, no AI used */ }
        }

        if (result.newTransactions.isEmpty() && result.total == 0) {
            val errorMessage = result.errors.firstOrNull() ?: strings.errorNoTransactionsFound
            _state.value = ImportState.Error(errorMessage)
        } else if (result.newTransactions.isEmpty() && result.errors.isNotEmpty()) {
            _state.value = ImportState.Error(result.errors.first())
        } else {
            val expenseCategories = getCategoriesUseCase(TransactionType.EXPENSE).first()
            val incomeCategories = getCategoriesUseCase(TransactionType.INCOME).first()
            _categories.value = expenseCategories + incomeCategories
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

    fun importTransactions(strings: AppStrings) {
        val current = _state.value
        if (current !is ImportState.Preview) return

        val accountId = _selectedAccountId.value
        if (accountId == null) {
            _state.value = ImportState.Error(strings.errorSelectAccountForImport)
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

                // Submit AI-generated config as candidate (fire-and-forget)
                val config = lastAiGeneratedConfig
                val sample = lastSampleRows
                if (config != null && sample != null) {
                    launch {
                        try {
                            val userId = authRepository.observeCurrentUser().first()?.userId
                            submitParserCandidateUseCase(config, sample, userId)
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to submit parser candidate, ignoring")
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: strings.errorImport)
            }
        }
    }

    fun reset() {
        _state.value = ImportState.Idle
        lastAiGeneratedConfig = null
        lastSampleRows = null
        debugCollector.clear()
    }
}

class ListImportProgressCollector : ImportProgressCollector {
    private val _eventsFlow = MutableStateFlow<List<ImportStepEvent>>(emptyList())
    val eventsFlow: StateFlow<List<ImportStepEvent>> = _eventsFlow.asStateFlow()

    override fun emit(event: ImportStepEvent) {
        _eventsFlow.update { it + event }
    }

    fun clear() {
        _eventsFlow.value = emptyList()
    }
}
