package com.atelbay.money_manager.presentation.settings.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.repository.ExchangeRateRepository
import com.atelbay.money_manager.domain.exchangerate.usecase.GetUsdKztRateUseCase
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
    private val getUsdKztRateUseCase: GetUsdKztRateUseCase,
    private val exchangeRateRepository: ExchangeRateRepository,
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
            getUsdKztRateUseCase(),
        ) { baseCurrencyCode, targetCurrencyCode, rate ->
            Triple(
                SupportedCurrencies.fromCode(baseCurrencyCode, SupportedCurrencies.defaultBase),
                SupportedCurrencies.fromCode(targetCurrencyCode, SupportedCurrencies.defaultTarget),
                rate,
            )
        }
            .onEach { (base, target, rate) ->
                _state.update { current ->
                    current.copy(
                        baseCurrency = base,
                        targetCurrency = target,
                        rateDisplay = buildRateDisplay(base, target, rate),
                        lastUpdatedDisplay = rate?.let(::formatLastUpdated).orEmpty(),
                        rateErrorMessage = if (rate != null) null else current.rateErrorMessage,
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
                exchangeRateRepository.fetchAndStoreRate()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                refreshError = e
            }

            _state.update {
                it.copy(
                    isRefreshingRate = false,
                    rateErrorMessage = refreshError?.let { "Не удалось обновить курс USD/KZT" },
                )
            }
        }
    }

    private fun buildRateDisplay(
        base: SupportedCurrency,
        target: SupportedCurrency,
        rate: ExchangeRate?,
    ): String {
        if (rate == null) return ""
        // System only stores usdToKzt; show that rate regardless of display currency selection
        return when {
            base.code == "KZT" ->
                "1 KZT = ${numberFormatter.format(1.0 / rate.usdToKzt)} USD"
            else ->
                "1 USD = ${numberFormatter.format(rate.usdToKzt)} KZT"
        }
    }

    private fun formatLastUpdated(rate: ExchangeRate): String {
        return Instant.ofEpochMilli(rate.fetchedAt)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)
    }
}
