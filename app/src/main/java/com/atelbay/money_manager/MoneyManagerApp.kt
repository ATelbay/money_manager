package com.atelbay.money_manager

import android.app.Application
import com.atelbay.money_manager.data.sync.LoginSyncOrchestrator
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MoneyManagerApp : Application() {

    @Inject
    lateinit var loginSyncOrchestrator: LoginSyncOrchestrator

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        loginSyncOrchestrator.start()
    }
}
