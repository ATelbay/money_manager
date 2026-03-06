package com.atelbay.money_manager.presentation.settings.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.domain.exchangerate.repository.ExchangeRateRepository
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRateSnapshot
import com.atelbay.money_manager.domain.exchangerate.usecase.ConvertAmountUseCase
import com.atelbay.money_manager.domain.exchangerate.usecase.GetExchangeRateSnapshotUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
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
    private val getExchangeRateSnapshotUseCase: GetExchangeRateSnapshotUseCase,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val convertAmountUseCase: ConvertAmountUseCase,
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

        combine(
            userPreferences.baseCurrency,
            userPreferences.targetCurrency,
            getExchangeRateSnapshotUseCase(),
        ) { baseCurrencyCode, targetCurrencyCode, snapshot ->
            Triple(
                SupportedCurrencies.fromCode(baseCurrencyCode, SupportedCurrencies.defaultBase),
                SupportedCurrencies.fromCode(targetCurrencyCode, SupportedCurrencies.defaultTarget),
                snapshot,
            )
        }
            .onEach { (base, target, snapshot) ->
                _state.update { current ->
                    current.copy(
                        baseCurrency = base,
                        targetCurrency = target,
                        rateDisplay = buildRateDisplay(base, target, snapshot),
                        lastUpdatedDisplay = snapshot?.let(::formatLastUpdated).orEmpty(),
                        rateErrorMessage = if (snapshot != null) null else current.rateErrorMessage,
                    )
                }
            }
            .launchIn(viewModelScope)
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
                exchangeRateRepository.fetchAndStoreRates()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                refreshError = e
            }

            _state.update {
                it.copy(
                    isRefreshingRate = false,
                    rateErrorMessage = refreshError?.let { "Не удалось обновить курсы валют" },
                )
            }
        }
    }

    private fun buildRateDisplay(
        base: SupportedCurrency,
        target: SupportedCurrency,
        snapshot: ExchangeRateSnapshot?,
    ): String {
        if (base.code == target.code) {
            return "1 ${base.code} = ${numberFormatter.format(1.0)} ${target.code}"
        }
        if (snapshot == null) return ""

        val converted = convertAmountUseCase(
            amount = 1.0,
            sourceCurrency = base.code,
            targetCurrency = target.code,
            snapshot = snapshot,
        ) ?: return ""

        return "1 ${base.code} = ${numberFormatter.format(converted)} ${target.code}"
    }

    private fun formatLastUpdated(snapshot: ExchangeRateSnapshot): String {
        return Instant.ofEpochMilli(snapshot.fetchedAt)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)
    }
}
