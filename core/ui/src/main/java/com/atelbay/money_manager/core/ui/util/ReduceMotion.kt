package com.atelbay.money_manager.core.ui.util

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalReduceMotion = compositionLocalOf { false }

@Composable
fun isReduceMotionEnabled(): Boolean {
    val context = LocalContext.current
    val durationScale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return durationScale == 0f
}
