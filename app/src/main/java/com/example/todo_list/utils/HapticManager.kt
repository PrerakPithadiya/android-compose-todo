package com.example.todo_list.utils

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class HapticType {
    CLICK,
    SUCCESS,
    WARNING,
    ERROR,
    TICK
}

enum class HapticIntensity(val displayName: String, val clickDurationMs: Long, val amplitude: Int) {
    LIGHT("Light", 20L, 140),
    MEDIUM("Medium", 32L, 200),
    STRONG("Strong", 45L, 255)
}

object HapticManager {
    private const val PREFS_NAME = "taskflow_haptic_prefs"
    private const val KEY_HAPTICS_ENABLED = "key_haptics_enabled"
    private const val KEY_HAPTIC_INTENSITY = "key_haptic_intensity"

    private var prefs: SharedPreferences? = null

    var isHapticsEnabled by mutableStateOf(true)
        private set

    var hapticIntensity by mutableStateOf(HapticIntensity.STRONG)
        private set

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isHapticsEnabled = prefs?.getBoolean(KEY_HAPTICS_ENABLED, true) ?: true
            val savedIntensityName = prefs?.getString(KEY_HAPTIC_INTENSITY, HapticIntensity.STRONG.name)
            hapticIntensity = try {
                HapticIntensity.valueOf(savedIntensityName ?: HapticIntensity.STRONG.name)
            } catch (e: Exception) {
                HapticIntensity.STRONG
            }
        }
    }

    fun updateHapticsEnabled(enabled: Boolean, context: Context? = null) {
        isHapticsEnabled = enabled
        if (prefs == null && context != null) {
            initialize(context)
        }
        prefs?.edit()?.putBoolean(KEY_HAPTICS_ENABLED, enabled)?.apply()
    }

    fun updateHapticIntensity(intensity: HapticIntensity, context: Context? = null) {
        hapticIntensity = intensity
        if (prefs == null && context != null) {
            initialize(context)
        }
        prefs?.edit()?.putString(KEY_HAPTIC_INTENSITY, intensity.name)?.apply()
    }

    fun perform(context: Context?, type: HapticType = HapticType.CLICK) {
        if (prefs == null && context != null) {
            initialize(context)
        }
        if (!isHapticsEnabled || context == null) return

        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator == null || !vibrator.hasVibrator()) return

            val clickDuration = hapticIntensity.clickDurationMs
            val amp = hapticIntensity.amplitude

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    HapticType.CLICK -> {
                        VibrationEffect.createOneShot(clickDuration, amp)
                    }
                    HapticType.TICK -> {
                        VibrationEffect.createOneShot(20L, (amp * 0.6f).toInt().coerceAtLeast(1))
                    }
                    HapticType.SUCCESS -> {
                        // Strong double pulse
                        val timings = longArrayOf(0, clickDuration, 50, (clickDuration * 1.2f).toLong())
                        val amplitudes = intArrayOf(0, (amp * 0.8f).toInt(), 0, amp)
                        VibrationEffect.createWaveform(timings, amplitudes, -1)
                    }
                    HapticType.WARNING -> {
                        // Warning firm double pulse
                        val timings = longArrayOf(0, clickDuration + 10, 60, clickDuration + 15)
                        val amplitudes = intArrayOf(0, amp, 0, amp)
                        VibrationEffect.createWaveform(timings, amplitudes, -1)
                    }
                    HapticType.ERROR -> {
                        // Strong triple buzz
                        val timings = longArrayOf(0, 35, 45, 35, 45, 50)
                        val amplitudes = intArrayOf(0, amp, 0, amp, 0, amp)
                        VibrationEffect.createWaveform(timings, amplitudes, -1)
                    }
                }

                vibrator.vibrate(effect, audioAttributes)
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    HapticType.CLICK -> vibrator.vibrate(clickDuration)
                    HapticType.TICK -> vibrator.vibrate(20L)
                    HapticType.SUCCESS -> vibrator.vibrate(longArrayOf(0, clickDuration, 50, (clickDuration * 1.2f).toLong()), -1)
                    HapticType.WARNING -> vibrator.vibrate(longArrayOf(0, clickDuration + 10, 60, clickDuration + 15), -1)
                    HapticType.ERROR -> vibrator.vibrate(longArrayOf(0, 35, 45, 35, 45, 50), -1)
                }
            }
        } catch (e: Exception) {
            // Gracefully ignore vibration errors
        }
    }

    fun performClick(context: Context?) {
        perform(context, HapticType.CLICK)
    }

    fun performSuccess(context: Context?) {
        perform(context, HapticType.SUCCESS)
    }

    fun performWarning(context: Context?) {
        perform(context, HapticType.WARNING)
    }

    fun performError(context: Context?) {
        perform(context, HapticType.ERROR)
    }

    fun performTick(context: Context?) {
        perform(context, HapticType.TICK)
    }
}
