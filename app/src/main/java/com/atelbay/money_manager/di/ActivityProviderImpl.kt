package com.atelbay.money_manager.di

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.atelbay.money_manager.core.auth.ActivityProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityProviderImpl @Inject constructor(
    @ApplicationContext context: Context,
) : ActivityProvider {

    override var currentActivity: Activity? = null
        private set

    init {
        (context as Application).registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    currentActivity = activity
                }

                override fun onActivityPaused(activity: Activity) {
                    if (currentActivity === activity) currentActivity = null
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }
}
