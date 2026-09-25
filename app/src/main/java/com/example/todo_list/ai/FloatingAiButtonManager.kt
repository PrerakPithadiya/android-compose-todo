package com.example.todo_list.ai

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Coordinates settings, state, docking mathematics, and service lifecycle
 * for the Floating AI Assistant Shortcut Button.
 */
object FloatingAiButtonManager {

    private const val PREFS_NAME = "taskflow_floating_ai_prefs"
    private const val KEY_ENABLED = "floating_ai_shortcut_enabled"
    private const val KEY_DOCKED_RIGHT = "floating_ai_docked_right"
    private const val KEY_Y_RATIO = "floating_ai_y_ratio"

    private var sharedPreferences: SharedPreferences? = null

    var isFloatingEnabled by mutableStateOf(false)
        private set

    fun initialize(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isFloatingEnabled = sharedPreferences?.getBoolean(KEY_ENABLED, false) ?: false
        }
        syncService(context.applicationContext)
    }

    /**
     * Checks if the app has system permission to display over other apps.
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Updates the persistent enabled state and launches or terminates the background overlay service.
     */
    fun setFloatingEnabled(context: Context, enabled: Boolean) {
        isFloatingEnabled = enabled
        sharedPreferences?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
        syncService(context.applicationContext)
    }

    /**
     * Starts or stops the overlay service based on the current enabled state and overlay permission.
     */
    fun syncService(context: Context) {
        if (isFloatingEnabled && canDrawOverlays(context)) {
            startOverlayService(context)
        } else if (!isFloatingEnabled) {
            stopOverlayService(context)
        }
    }

    fun startOverlayService(context: Context) {
        try {
            val intent = Intent(context, FloatingAiOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {
            // Safety guard for background start restrictions
        }
    }

    fun stopOverlayService(context: Context) {
        try {
            val intent = Intent(context, FloatingAiOverlayService::class.java)
            context.stopService(intent)
        } catch (_: Exception) {}
    }

    fun savePosition(isRightSide: Boolean, yRatio: Float) {
        sharedPreferences?.edit()
            ?.putBoolean(KEY_DOCKED_RIGHT, isRightSide)
            ?.putFloat(KEY_Y_RATIO, yRatio.coerceIn(0.1f, 0.9f))
            ?.apply()
    }

    fun getDockedPosition(): Pair<Boolean, Float> {
        val isRight = sharedPreferences?.getBoolean(KEY_DOCKED_RIGHT, true) ?: true
        val yRatio = sharedPreferences?.getFloat(KEY_Y_RATIO, 0.5f) ?: 0.5f
        return Pair(isRight, yRatio)
    }

    /**
     * Mathematical docking solver:
     * Calculates the target X position when the button is released.
     * The button NEVER stays in the center:
     * - If its center is left of the screen midpoint -> snaps to left border (0).
     * - If its center is right of or at the screen midpoint -> snaps to right border (screenWidth - buttonWidth).
     */
    fun calculateDockedTargetX(
        currentX: Int,
        buttonWidth: Int,
        screenWidth: Int
    ): Int {
        val centerX = currentX + buttonWidth / 2
        return if (centerX < screenWidth / 2) {
            0
        } else {
            (screenWidth - buttonWidth).coerceAtLeast(0)
        }
    }

    /**
     * Clamps the Y position so the button never falls behind the system status bar,
     * display cutout, or gesture navigation bar.
     */
    fun clampYPosition(
        currentY: Int,
        buttonHeight: Int,
        screenHeight: Int,
        topMargin: Int = 60,
        bottomMargin: Int = 80
    ): Int {
        val minY = topMargin
        val maxY = (screenHeight - buttonHeight - bottomMargin).coerceAtLeast(minY)
        return currentY.coerceIn(minY, maxY)
    }
}
