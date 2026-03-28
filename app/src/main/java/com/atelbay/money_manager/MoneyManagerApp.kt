package com.atelbay.money_manager

import android.app.Application
import com.atelbay.money_manager.data.sync.LoginSyncOrchestrator
import com.atelbay.money_manager.domain.recurring.usecase.GeneratePendingTransactionsUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MoneyManagerApp : Application() {

    @Inject
    lateinit var loginSyncOrchestrator: LoginSyncOrchestrator

    @Inject
    lateinit var generatePendingTransactionsUseCase: GeneratePendingTransactionsUseCase

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        loginSyncOrchestrator.start()
        appScope.launch {
            try {
                generatePendingTransactionsUseCase()
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate pending recurring transactions")
            }
        }
    }
}
