package com.atelbay.money_manager

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.WARN) {
            FirebaseCrashlytics.getInstance().log("${tag ?: "NoTag"}: $message")
        }
        if (priority >= Log.ERROR) {
            FirebaseCrashlytics.getInstance().recordException(
                t ?: RuntimeException("${tag ?: "NoTag"}: $message")
            )
        }
    }
}
