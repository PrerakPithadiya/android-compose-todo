package com.example.todo_list.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * TaskFlow Spring Physics & Motion Curves (Apple iOS HIG Specification).
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 7 & Section 16
 *
 * Formula: stiffness = (2 * PI / response)^2, mass = 1.0
 */
object TFMotion {
    // Press scale-down (to 0.97/0.98) and release (~0.16s response)
    fun <T> press(): SpringSpec<T> = spring(dampingRatio = 0.90f, stiffness = 1500f)

    // Toggles, chips, segmented thumb, checkbox fill (~0.35s response)
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.85f, stiffness = 1290f)

    // List reorder, card expansion, ring progress, inline panels (~0.50s response)
    fun <T> standard(): SpringSpec<T> = spring(dampingRatio = 0.825f, stiffness = 630f)

    // Modal sheet presentation/dismissal, nav push (~0.63s response)
    // Critical: dampingRatio = 1.0 (never < 0.95) to prevent gap bounce at screen edge
    fun <T> sheet(): SpringSpec<T> = spring(dampingRatio = 1.0f, stiffness = 400f)

    // Celebrations only: checkbox pop, XP badge, medal unlock (~0.40s response)
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.60f, stiffness = 700f)

    // Non-spring Apple standard easing curve
    val ease = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
}

val LocalReducedMotion = staticCompositionLocalOf { false }
