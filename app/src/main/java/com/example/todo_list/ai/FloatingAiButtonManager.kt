package com.example.todo_list.ai

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.todo_list.R

/**
 * Curated Apple iOS HIG color themes for the Floating AI Assistant Shortcut Button.
 */
enum class FloatingAiColor(
    val id: String,
    val displayName: String,
    val startColorHex: Long,
    val endColorHex: Long,
    val strokeColorHex: Long
) {
    PURPLE("purple", "Royal Purple", 0xFF7C3AED, 0xFFA855F7, 0x55FFFFFF),
    BLUE("blue", "System Blue", 0xFF007AFF, 0xFF0A84FF, 0x55FFFFFF),
    CORAL("coral", "Sunset Coral", 0xFFFF5E3A, 0xFFFF9500, 0x55FFFFFF),
    EMERALD("emerald", "Emerald Mint", 0xFF34C759, 0xFF30D158, 0x55FFFFFF),
    CYAN("cyan", "Neon Cyan", 0xFF06B6D4, 0xFF3B82F6, 0x55FFFFFF),
    OBSIDIAN("obsidian", "Dark Minimal", 0xFF374151, 0xFF1F2937, 0x55FFFFFF),
    GOLD("gold", "Champagne Gold", 0xFFF59E0B, 0xFFD97706, 0x55FFFFFF),
    ROSE("rose", "System Rose", 0xFFEC4899, 0xFFF43F5E, 0x55FFFFFF);

    val startColor: Color get() = Color(startColorHex)
    val endColor: Color get() = Color(endColorHex)
    val strokeColor: Color get() = Color(strokeColorHex)

    companion object {
        val DEFAULT = PURPLE
        fun fromId(id: String?): FloatingAiColor =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}

/**
 * Feature-appropriate icon glyphs strictly matching the AI Assistant and Smart Task functionalities.
 */
enum class FloatingAiGlyph(
    val id: String,
    val displayName: String,
    val description: String,
    @get:DrawableRes val drawableResId: Int,
    val iconVector: ImageVector
) {
    SPARKLE(
        id = "sparkle",
        displayName = "AI Sparkle",
        description = "Apple Intelligence / Generative AI stars",
        drawableResId = R.drawable.ic_floating_ai_sparkle,
        iconVector = Icons.Default.AutoAwesome
    ),
    CHAT(
        id = "chat",
        displayName = "Assistant Chat",
        description = "Direct conversational chatbot messaging",
        drawableResId = R.drawable.ic_floating_ai_chat,
        iconVector = Icons.AutoMirrored.Filled.Chat
    ),
    BOT(
        id = "bot",
        displayName = "Smart Agent",
        description = "Dedicated on-device AI bot assistant",
        drawableResId = R.drawable.ic_floating_ai_bot,
        iconVector = Icons.Default.SmartToy
    ),
    BOLT(
        id = "bolt",
        displayName = "Quick Capture",
        description = "Instant natural language task logging",
        drawableResId = R.drawable.ic_floating_ai_bolt,
        iconVector = Icons.Default.Bolt
    ),
    CHECK(
        id = "check",
        displayName = "Smart Tasks",
        description = "TaskFlow schedule manager & checklist",
        drawableResId = R.drawable.ic_floating_ai_check,
        iconVector = Icons.Default.CheckCircle
    ),
    BRAIN(
        id = "brain",
        displayName = "Intelligence",
        description = "Deep cognitive schedule & priority logic",
        drawableResId = R.drawable.ic_floating_ai_brain,
        iconVector = Icons.Default.Psychology
    );

    companion object {
        val DEFAULT = SPARKLE
        fun fromId(id: String?): FloatingAiGlyph =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}

/**
 * Coordinates settings, state, docking mathematics, styling, and service lifecycle
 * for the Floating AI Assistant Shortcut Button.
 */
object FloatingAiButtonManager {

    private const val PREFS_NAME = "taskflow_floating_ai_prefs"
    private const val KEY_ENABLED = "floating_ai_shortcut_enabled"
    private const val KEY_DOCKED_RIGHT = "floating_ai_docked_right"
    private const val KEY_Y_RATIO = "floating_ai_y_ratio"
    private const val KEY_COLOR_ID = "floating_ai_color_id"
    private const val KEY_GLYPH_ID = "floating_ai_glyph_id"

    const val ACTION_UPDATE_STYLE = "com.example.todo_list.UPDATE_FLOATING_AI_STYLE"

    private var sharedPreferences: SharedPreferences? = null

    var isFloatingEnabled by mutableStateOf(false)
        private set

    var selectedColor by mutableStateOf(FloatingAiColor.DEFAULT)
        private set

    var selectedGlyph by mutableStateOf(FloatingAiGlyph.DEFAULT)
        private set

    fun initialize(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isFloatingEnabled = sharedPreferences?.getBoolean(KEY_ENABLED, false) ?: false
            val colorId = sharedPreferences?.getString(KEY_COLOR_ID, FloatingAiColor.DEFAULT.id)
            selectedColor = FloatingAiColor.fromId(colorId)
            val glyphId = sharedPreferences?.getString(KEY_GLYPH_ID, FloatingAiGlyph.DEFAULT.id)
            selectedGlyph = FloatingAiGlyph.fromId(glyphId)
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

    fun setFloatingEnabled(context: Context, enabled: Boolean) {
        isFloatingEnabled = enabled
        sharedPreferences?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
        syncService(context.applicationContext)
    }

    fun setColor(context: Context, color: FloatingAiColor) {
        selectedColor = color
        sharedPreferences?.edit()?.putString(KEY_COLOR_ID, color.id)?.apply()
        notifyStyleChanged(context)
    }

    fun setGlyph(context: Context, glyph: FloatingAiGlyph) {
        selectedGlyph = glyph
        sharedPreferences?.edit()?.putString(KEY_GLYPH_ID, glyph.id)?.apply()
        notifyStyleChanged(context)
    }

    private fun notifyStyleChanged(context: Context) {
        if (isFloatingEnabled && canDrawOverlays(context)) {
            val intent = Intent(context, FloatingAiOverlayService::class.java).apply {
                action = ACTION_UPDATE_STYLE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

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
        } catch (_: Exception) {}
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
            ?.putFloat(KEY_Y_RATIO, yRatio.coerceIn(0.15f, 0.85f))
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
     * Clamps the Y position so the button strictly stays between the top navigation bar
     * and the bottom navigation bar, preventing any visual overlap.
     *
     * Default Boundaries:
     * - topMargin: 160 (safely below status bar + large title nav bar + search field)
     * - bottomMargin: 115 (safely above 56dp bottom bar + center docked Plus button + insets)
     */
    fun clampYPosition(
        currentY: Int,
        buttonHeight: Int,
        screenHeight: Int,
        topMargin: Int = 160,
        bottomMargin: Int = 115
    ): Int {
        val minY = topMargin
        val maxY = (screenHeight - buttonHeight - bottomMargin).coerceAtLeast(minY)
        return currentY.coerceIn(minY, maxY)
    }
}
