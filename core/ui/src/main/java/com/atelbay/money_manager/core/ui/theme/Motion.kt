package com.atelbay.money_manager.core.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset

object MoneyManagerMotion {
    // ── Duration tokens ──
    const val DurationShort: Int = 150
    const val DurationMedium: Int = 300
    const val DurationLong: Int = 500
    const val DurationExtraLong: Int = 800
    const val StaggerDelay: Long = 50L
    const val ReducedDuration: Int = 10

    // ── Easing tokens (M3) ──
    val EnterEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f) // EmphasizedDecelerate
    val ExitEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f) // EmphasizedAccelerate
    val StandardEasing: Easing = FastOutSlowInEasing

    // ── Navigation transitions ──

    fun drillInEnter(): EnterTransition =
        fadeIn(tween(DurationMedium, easing = EnterEasing)) +
            slideInHorizontally(tween(DurationMedium, easing = EnterEasing)) { it / 4 }

    fun drillInExit(): ExitTransition =
        fadeOut(tween(DurationMedium, easing = ExitEasing)) +
            slideOutHorizontally(tween(DurationMedium, easing = ExitEasing)) { -it / 4 }

    fun drillInPopEnter(): EnterTransition =
        fadeIn(tween(DurationMedium, easing = EnterEasing)) +
            slideInHorizontally(tween(DurationMedium, easing = EnterEasing)) { -it / 4 }

    fun drillInPopExit(): ExitTransition =
        fadeOut(tween(DurationMedium, easing = ExitEasing)) +
            slideOutHorizontally(tween(DurationMedium, easing = ExitEasing)) { it / 4 }

    fun tabEnter(): EnterTransition = fadeIn(tween(DurationMedium))

    fun tabExit(): ExitTransition = fadeOut(tween(DurationMedium))

    // ── Prebuilt animation specs ──

    val InteractionSpring: SpringSpec<Float> = spring(stiffness = 400f, dampingRatio = 0.6f)

    val ColorTransition: TweenSpec<Color> = tween(durationMillis = DurationMedium)

    val CounterSpec: TweenSpec<Float> = tween(durationMillis = DurationExtraLong)

    val ChartSpec: TweenSpec<Float> = tween(durationMillis = 600)

    val DonutSpec: TweenSpec<Float> = tween(durationMillis = DurationExtraLong)

    val StaggerFadeSpec: TweenSpec<Float> = tween(durationMillis = DurationMedium)

    val ItemFadeInSpec: TweenSpec<Float> = tween(durationMillis = 200)

    val ItemFadeOutSpec: TweenSpec<Float> = tween(durationMillis = DurationShort)

    val ItemPlacementSpec: SpringSpec<IntOffset> = spring(stiffness = 380f, dampingRatio = 0.8f)

    // ── Reduce Motion helpers ──

    const val MaxStaggerItems: Int = 10

    fun duration(baseMs: Int, reduceMotion: Boolean): Int =
        if (reduceMotion) ReducedDuration else baseMs

    fun staggerDelay(index: Int, reduceMotion: Boolean): Long = when {
        reduceMotion -> 0L
        index >= MaxStaggerItems -> MaxStaggerItems * StaggerDelay
        else -> index * StaggerDelay
    }
}
