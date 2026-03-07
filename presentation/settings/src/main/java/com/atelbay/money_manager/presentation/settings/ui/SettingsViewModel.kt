package com.atelbay.money_manager.presentation.settings.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.data.sync.LoginSyncOrchestrator
import com.atelbay.money_manager.data.sync.SyncManager
import com.atelbay.money_manager.data.sync.SyncStatus
import com.atelbay.money_manager.domain.auth.usecase.ObserveAuthUserUseCase
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.repository.ExchangeRateRepository
import com.atelbay.money_manager.domain.exchangerate.usecase.ObserveExchangeRateUseCase
import com.atelbay.money_manager.domain.transactions.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val observeExchangeRateUseCase: ObserveExchangeRateUseCase,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val transactionRepository: TransactionRepository,
    private val observeAuthUser: ObserveAuthUserUseCase,
    private val syncManager: SyncManager,
    private val loginSyncOrchestrator: LoginSyncOrchestrator,
    application: Application,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    private val timeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private val numberFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    init {
        val versionName = try {
            application.packageManager
                .getPackageInfo(application.packageName, 0)
                .versionName ?: ""
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
        _state.update { it.copy(appVersion = versionName) }

        observeAuthUser()
            .onEach { user -> _state.update { it.copy(currentUser = user) } }
            .launchIn(viewModelScope)

        syncManager.syncStatus
            .onEach { status ->
                val lastSyncDisplay = when (status) {
                    is SyncStatus.Synced -> formatSyncDate(status.lastSyncedAt)
                    is SyncStatus.Failed -> status.lastSyncedAt?.let(::formatSyncDate).orEmpty()
                    else -> ""
                }
                _state.update { it.copy(syncStatus = status, lastSyncDisplay = lastSyncDisplay) }
            }
            .launchIn(viewModelScope)

        userPreferences.themeMode
            .onEach { mode ->
                _state.update {
                    it.copy(
                        themeMode = when (mode) {
                            "light" -> ThemeMode.LIGHT
                            "dark" -> ThemeMode.DARK
                            else -> ThemeMode.SYSTEM
                        },
                    )
                }
            }
            .launchIn(viewModelScope)

        userPreferences.languageCode
            .map { code -> AppLanguage.entries.firstOrNull { it.code == code } ?: AppLanguage.RUSSIAN }
            .onEach { lang -> _state.update { it.copy(appLanguage = lang) } }
            .launchIn(viewModelScope)

        combine(
            userPreferences.baseCurrency,
            userPreferences.targetCurrency,
            observeExchangeRateUseCase(),
        ) { baseCurrencyCode, targetCurrencyCode, rate ->
            Triple(baseCurrencyCode, targetCurrencyCode, rate)
        }
            .onEach { (baseCurrencyCode, targetCurrencyCode, rate) ->
                val base = sanitizeCurrency(
                    code = baseCurrencyCode,
                    fallback = SupportedCurrencies.defaultBase,
                    persist = userPreferences::setBaseCurrency,
                )
                val target = sanitizeCurrency(
                    code = targetCurrencyCode,
                    fallback = SupportedCurrencies.defaultTarget,
                    persist = userPreferences::setTargetCurrency,
                )
                _state.update { current ->
                    current.copy(
                        baseCurrency = base,
                        targetCurrency = target,
                        rateDisplay = buildRateDisplay(base, target, rate),
                        hasRateSnapshot = rate != null,
                        lastUpdatedDisplay = rate?.let(::formatLastUpdated).orEmpty(),
                        rateErrorMessage = if (rate != null) null else current.rateErrorMessage,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun retrySync() {
        loginSyncOrchestrator.retrySync()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            val value = when (mode) {
                ThemeMode.SYSTEM -> "system"
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
            }
            userPreferences.setThemeMode(value)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            userPreferences.setLanguageCode(language.code)
            _state.update { it.copy(appLanguage = language) }
        }
    }

    fun setBaseCurrency(currency: SupportedCurrency) {
        viewModelScope.launch {
            userPreferences.setBaseCurrency(currency.code)
        }
    }

    fun setTargetCurrency(currency: SupportedCurrency) {
        viewModelScope.launch {
            userPreferences.setTargetCurrency(currency.code)
        }
    }

    fun refreshExchangeRate() {
        if (_state.value.isRefreshingRate) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isRefreshingRate = true,
                    rateErrorMessage = null,
                )
            }

            var refreshError: Throwable? = null
            try {
                exchangeRateRepository.fetchAndStoreQuotes()
                userPreferences.resetQuoteRefreshFailureCount()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                refreshError = e
                val failureCount = userPreferences.incrementQuoteRefreshFailureCount()
                if (failureCount >= FAILURE_THRESHOLD_FOR_AUTO_SWITCH) {
                    autoSwitchCurrencyPair()
                    userPreferences.resetQuoteRefreshFailureCount()
                }
            }

            _state.update {
                it.copy(
                    isRefreshingRate = false,
                    rateErrorMessage = refreshError?.let { "Не удалось обновить курс" },
                )
            }
        }
    }

    /**
     * Auto-selects base/target currency pair from all-time transaction frequency.
     *
     * - base = top-1 currency by transaction count (fallback: USD)
     * - target = top-2 currency by transaction count (fallback: EUR)
     *
     * Tie-breaking: when multiple currencies have equal transaction counts,
     * alphabetical order by currency code is used (enforced by DAO ORDER BY).
     */
    private suspend fun autoSwitchCurrencyPair() {
        val topCurrencies = try {
            transactionRepository.getTopCurrenciesByUsage()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }

        val base = topCurrencies.getOrNull(0) ?: DEFAULT_FALLBACK_BASE
        val rawTarget = topCurrencies.getOrNull(1) ?: DEFAULT_FALLBACK_TARGET
        val target = if (rawTarget == base) {
            if (base == DEFAULT_FALLBACK_TARGET) DEFAULT_FALLBACK_BASE else DEFAULT_FALLBACK_TARGET
        } else {
            rawTarget
        }

        userPreferences.setBaseCurrency(base)
        userPreferences.setTargetCurrency(target)
    }

    private fun buildRateDisplay(
        base: SupportedCurrency,
        target: SupportedCurrency,
        rate: ExchangeRate?,
    ): String? {
        if (rate == null) return null
        if (base.code == target.code) return "1 ${base.code} = 1.00 ${target.code}"
        val baseToKzt = rate.quotes[base.code] ?: return null
        val targetToKzt = rate.quotes[target.code] ?: return null
        val convertedRate = baseToKzt / targetToKzt
        return "1 ${base.code} = ${numberFormatter.format(convertedRate)} ${target.code}"
    }

    private suspend fun sanitizeCurrency(
        code: String,
        fallback: SupportedCurrency,
        persist: suspend (String) -> Unit,
    ): SupportedCurrency {
        val normalized = code.trim().uppercase()
        return if (SupportedCurrencies.isSupported(normalized)) {
            SupportedCurrencies.fromCode(normalized, fallback)
        } else {
            persist(fallback.code)
            fallback
        }
    }

    private fun formatLastUpdated(rate: ExchangeRate): String {
        return Instant.ofEpochMilli(rate.fetchedAt)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)
    }

    private fun formatSyncDate(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)
    }

    private companion object {
        const val FAILURE_THRESHOLD_FOR_AUTO_SWITCH = 3
        const val DEFAULT_FALLBACK_BASE = "USD"
        const val DEFAULT_FALLBACK_TARGET = "EUR"
    }
}
