package com.atelbay.money_manager

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.WARN) {
            FirebaseCrashlytics.getInstance().log("${tag ?: "NoTag"}: $message")
        }
        // Only record real exceptions. A non-throwable error was already captured by the
        // log() call above; synthesizing a RuntimeException here only produces noisy,
        // untriageable crashes rooted in CrashlyticsTree.
        if (priority >= Log.ERROR && t != null) {
            FirebaseCrashlytics.getInstance().recordException(t)
        }
    }
}
