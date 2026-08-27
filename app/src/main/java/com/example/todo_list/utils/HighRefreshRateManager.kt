package com.example.todo_list.utils

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.View
import android.view.Window
import android.view.WindowManager

/**
 * HighRefreshRateManager enforces ultra-smooth 120Hz (or highest supported hardware refresh rate)
 * across the application, preventing OS-level throttling or Variable Refresh Rate (VRR) downclocking.
 */
object HighRefreshRateManager {

    /**
     * Enables maximum supported display refresh rate (120Hz / 144Hz / 90Hz)
     * on the given Activity window.
     */
    fun enableHighRefreshRate(activity: Activity) {
        try {
            val window = activity.window ?: return

            // 1. Immediate application on window creation
            apply120HzRefreshRate(window, activity)

            // 2. DecorView post-attachment enforcement
            window.decorView.post {
                try {
                    apply120HzRefreshRate(window, activity)
                    applySurfaceFrameRate(window.decorView, 120f)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Re-applies 120Hz parameters when the activity resumes or regains focus.
     */
    fun onResume(activity: Activity) {
        enableHighRefreshRate(activity)
    }

    private fun apply120HzRefreshRate(window: Window, context: Context) {
        try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    context.display ?: displayManager?.getDisplay(Display.DEFAULT_DISPLAY) ?: window.decorView.display
                } catch (e: Exception) {
                    displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
                }
            } else {
                @Suppress("DEPRECATION")
                displayManager?.getDisplay(Display.DEFAULT_DISPLAY) ?: window.windowManager.defaultDisplay
            }

            if (display == null) return

            val modes = display.supportedModes ?: return
            if (modes.isEmpty()) return

            // 1. Find the highest refresh rate available on the hardware (prefer 120Hz or higher)
            val maxRefreshRate = modes.maxOfOrNull { it.refreshRate } ?: 60f

            // 2. Select the mode that matches this highest refresh rate with maximum physical resolution
            val targetModes = modes.filter { it.refreshRate >= maxRefreshRate - 0.5f }
            val bestMode = targetModes.maxByOrNull { it.physicalWidth * it.physicalHeight } ?: modes.firstOrNull()

            val lp = window.attributes

            // 3. Lock window to the 120Hz / highest display mode (API 23+)
            if (bestMode != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                lp.preferredDisplayModeId = bestMode.modeId
            }

            // 4. Set preferred refresh rate (API 23+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                lp.preferredRefreshRate = maxRefreshRate
            }

            // 5. Apply extended min/max refresh rate parameters via reflection if present on device
            try {
                val minRateField = lp.javaClass.getField("preferredMinDisplayRefreshRate")
                minRateField.isAccessible = true
                minRateField.setFloat(lp, maxRefreshRate)
            } catch (_: Exception) {}

            try {
                val maxRateField = lp.javaClass.getField("preferredMaxDisplayRefreshRate")
                maxRateField.isAccessible = true
                maxRateField.setFloat(lp, maxRefreshRate)
            } catch (_: Exception) {}

            window.attributes = lp
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applySurfaceFrameRate(decorView: View, frameRate: Float) {
        try {
            // Check for View.setFrameRate(float, int) if available on Android 11+ / 14+
            val method = decorView.javaClass.getMethod("setFrameRate", Float::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            method.invoke(decorView, frameRate, 0)
        } catch (_: Exception) {}
    }
}
