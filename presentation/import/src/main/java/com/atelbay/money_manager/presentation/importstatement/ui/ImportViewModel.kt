package com.atelbay.money_manager.presentation.importstatement.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.ImportState
import com.atelbay.money_manager.core.model.TableParserProfile
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
import com.atelbay.money_manager.core.remoteconfig.RegexParserProfile
import com.atelbay.money_manager.core.ui.theme.AppStrings
import com.atelbay.money_manager.domain.importstatement.usecase.AiMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    /** Survives configuration change AND process death so the user's account choice isn't lost. */
    val selectedAccountId: StateFlow<Long?> =
        savedStateHandle.getStateFlow(KEY_SELECTED_ACCOUNT, null)

    /** Debug-only: emits a message when AI fallback is used. */
    private val _debugAiEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val debugAiEvent = _debugAiEvent.asSharedFlow()

    /** Debug progress collector — always created, only consumed in debug UI. */
    val debugCollector = ListImportProgressCollector()

    /** Tracks the AI-generated configs from the last parse, if any. */
    private var lastAiGeneratedConfig: RegexParserProfile? = null
    private var lastSampleRows: String? = null
    private var lastAiGeneratedTableConfig: TableParserProfile? = null
    private var lastSampleTableRows: List<List<String>>? = null
    private var lastAiMethod: AiMethod = AiMethod.NONE

    /** Stores the last blobs so the user can retry parsing without re-selecting the file. */
    private var lastBlobs: List<Pair<ByteArray, String>>? = null

    /** The last successfully-built preview, so a failed import can be retried without re-parsing. */
    private var lastPreview: ImportState.Preview? = null
    private var lastImportFailed = false

    /** The in-flight parse/import job, so it can be cancelled by the user. */
    private var activeJob: Job? = null

    init {
        viewModelScope.launch {
            val allAccounts = getAccountsUseCase().first()
            _accounts.value = allAccounts
            // Only seed a default when nothing was restored (fresh screen, not a config/process change).
            if (savedStateHandle.get<Long?>(KEY_SELECTED_ACCOUNT) == null) {
                val preferred = userPreferences.selectedAccountId.first()
                savedStateHandle[KEY_SELECTED_ACCOUNT] = when {
                    preferred != null && allAccounts.any { it.id == preferred } -> preferred
                    allAccounts.isNotEmpty() -> allAccounts.first().id
                    else -> null
                }
            }
        }
    }

    fun selectAccount(accountId: Long) {
        savedStateHandle[KEY_SELECTED_ACCOUNT] = accountId
    }

    fun onPdfSelected(bytes: ByteArray, strings: AppStrings) {
        startParse(listOf(bytes to "application/pdf"), strings, strings.errorReadingPdf)
    }

    fun onPhotoTaken(imageBytes: ByteArray, strings: AppStrings) {
        startParse(listOf(imageBytes to "image/jpeg"), strings, strings.errorUnknown)
    }

    private fun startParse(
        blobs: List<Pair<ByteArray, String>>,
        strings: AppStrings,
        fallbackError: String,
    ) {
        if (_state.value == ImportState.Parsing) return
        _state.value = ImportState.Parsing
        activeJob = viewModelScope.launch {
            try {
                parseAndPreview(blobs, strings)
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: fallbackError)
            }
        }
    }

    private suspend fun parseAndPreview(blobs: List<Pair<ByteArray, String>>, strings: AppStrings) {
        lastBlobs = blobs
        lastImportFailed = false
        debugCollector.clear()
        val parseResult = parseStatementUseCase(blobs, debugCollector)
        val result = parseResult.importResult
        lastAiGeneratedConfig = parseResult.aiGeneratedConfig
        lastSampleRows = parseResult.sampleRows
        lastAiGeneratedTableConfig = parseResult.aiGeneratedTableConfig
        lastSampleTableRows = parseResult.sampleTableRows
        lastAiMethod = parseResult.aiMethod

        when (parseResult.aiMethod) {
            AiMethod.REGEX_GENERATED -> _debugAiEvent.tryEmit(
                "AI regex generated for: ${parseResult.aiGeneratedConfig?.bankId}",
            )
            AiMethod.TABLE_GENERATED -> _debugAiEvent.tryEmit(
                "AI table config generated for: ${parseResult.aiGeneratedTableConfig?.bankId}",
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
            // Note: an all-duplicates result (newTransactions empty, total > 0) deliberately goes to
            // Preview — ImportPreview renders a friendly "already imported" message in that case.
            val expenseCategories = getCategoriesUseCase(TransactionType.EXPENSE).first()
            val incomeCategories = getCategoriesUseCase(TransactionType.INCOME).first()
            _categories.value = expenseCategories + incomeCategories
            val preview = ImportState.Preview(result = result, overrides = emptyMap())
            lastPreview = preview
            _state.value = preview
        }
    }

    private fun updateOverride(index: Int, update: (TransactionOverride) -> TransactionOverride) {
        val current = _state.value
        if (current is ImportState.Preview) {
            val existing = current.overrides[index] ?: TransactionOverride()
            val updated = current.copy(overrides = current.overrides + (index to update(existing)))
            lastPreview = updated
            _state.value = updated
        }
    }

    fun updateAmount(index: Int, amount: Long) {
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

        val accountId = selectedAccountId.value
        if (accountId == null) {
            _state.value = ImportState.Error(strings.errorSelectAccountForImport)
            return
        }

        lastPreview = current
        // Flip state synchronously so a second tap sees a non-Preview state and bails out.
        _state.value = ImportState.Importing
        activeJob = viewModelScope.launch {
            try {
                val imported = importTransactionsUseCase(
                    transactions = current.result.newTransactions,
                    accountId = accountId,
                    overrides = current.overrides,
                )
                lastImportFailed = false
                lastBlobs = null // release PDF/image bytes once safely persisted
                _state.value = ImportState.Success(imported)
                persistAndSubmitConfigs()
            } catch (e: Exception) {
                lastImportFailed = true
                _state.value = ImportState.Error(e.message ?: strings.errorImport)
            }
        }
    }

    /** Caches AI-generated configs and submits them as shared candidates (fire-and-forget). */
    private fun persistAndSubmitConfigs() {
        when (lastAiMethod) {
            AiMethod.REGEX_GENERATED -> lastAiGeneratedConfig?.let { config ->
                viewModelScope.launch { parseStatementUseCase.cacheAiConfig(config) }
            }
            AiMethod.TABLE_GENERATED -> lastAiGeneratedTableConfig?.let { tableConfig ->
                viewModelScope.launch { parseStatementUseCase.cacheTableConfig(tableConfig) }
            }
            else -> { /* no config to cache */ }
        }

        viewModelScope.launch {
            val userId = authRepository.observeCurrentUser().first()?.userId
            val config = lastAiGeneratedConfig
            val sample = lastSampleRows
            if (config != null && sample != null) {
                try {
                    submitParserCandidateUseCase(config, sample, userId)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to submit parser candidate, ignoring")
                }
            }

            val tableConfig = lastAiGeneratedTableConfig
            val tableSample = lastSampleTableRows
            if (tableConfig != null && tableSample != null) {
                try {
                    submitParserCandidateUseCase.submitTableConfig(tableConfig, tableSample, userId)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to submit table parser candidate, ignoring")
                }
            }
        }
    }

    /** Cancels an in-flight parse or import. An import rolls back atomically via the DB transaction. */
    fun cancel() {
        val current = _state.value
        activeJob?.cancel()
        activeJob = null
        _state.value = when (current) {
            is ImportState.Importing -> lastPreview ?: ImportState.Idle
            else -> ImportState.Idle
        }
    }

    fun retry(strings: AppStrings) {
        // A failed import retries the DB write with the same edits — no costly AI re-parse.
        if (lastImportFailed) {
            val preview = lastPreview
            if (preview != null) {
                lastImportFailed = false
                _state.value = preview
                importTransactions(strings)
                return
            }
        }
        val blobs = lastBlobs ?: return
        startParse(blobs, strings, strings.errorUnknown)
    }

    fun reset() {
        activeJob?.cancel()
        activeJob = null
        _state.value = ImportState.Idle
        lastBlobs = null
        lastPreview = null
        lastImportFailed = false
        lastAiGeneratedConfig = null
        lastSampleRows = null
        lastAiGeneratedTableConfig = null
        lastSampleTableRows = null
        lastAiMethod = AiMethod.NONE
        debugCollector.clear()
    }

    private companion object {
        const val KEY_SELECTED_ACCOUNT = "import_selected_account_id"
    }
}

class ListImportProgressCollector : ImportProgressCollector {
    private val _eventsFlow = MutableStateFlow<List<ImportStepEvent>>(emptyList())
    val eventsFlow: StateFlow<List<ImportStepEvent>> = _eventsFlow.asStateFlow()

    override fun emit(event: ImportStepEvent) {
        _eventsFlow.value = _eventsFlow.value + event
    }

    fun clear() {
        _eventsFlow.value = emptyList()
    }
}
