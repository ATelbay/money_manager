package com.atelbay.money_manager.core.auth

import android.app.Activity

/**
 * Provides access to the currently resumed Activity.
 * Required by CredentialManager which needs an Activity context.
 */
interface ActivityProvider {
    val currentActivity: Activity?
}
