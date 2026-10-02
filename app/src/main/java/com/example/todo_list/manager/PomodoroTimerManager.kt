package com.example.todo_list.manager

import android.content.Context
import android.media.RingtoneManager
import android.os.CountDownTimer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.todo_list.model.TaskItem
import com.example.todo_list.utils.HapticManager
import com.example.todo_list.utils.HapticType

/**
 * Pomodoro Session Mode
 */
enum class PomodoroMode(val label: String, val defaultMinutes: Int) {
    WORK("Focus", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15)
}

/**
 * Global singleton state manager for the Pomodoro Focus Timer.
 * Manages timer state, active task link, work/break cycle progression,
 * notifications, and sound/haptics.
 */
object PomodoroTimerManager {

    private const val PREFS_NAME = "taskflow_pomodoro_prefs"
    private const val KEY_WORK_MINS = "work_minutes"
    private const val KEY_SHORT_BREAK_MINS = "short_break_minutes"
    private const val KEY_LONG_BREAK_MINS = "long_break_minutes"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_AUTO_START_BREAKS = "auto_start_breaks"

    // Configuration
    var workDurationMinutes by mutableIntStateOf(25)
        private set
    var shortBreakDurationMinutes by mutableIntStateOf(5)
        private set
    var longBreakDurationMinutes by mutableIntStateOf(15)
        private set
    var isSoundEnabled by mutableStateOf(true)
        private set
    var autoStartBreaks by mutableStateOf(false)
        private set

    // Active Timer State
    var currentMode by mutableStateOf(PomodoroMode.WORK)
        private set
    var isRunning by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var remainingMillis by mutableLongStateOf(25 * 60 * 1000L)
        private set
    var totalSessionMillis by mutableLongStateOf(25 * 60 * 1000L)
        private set

    // Cycle tracking: 4 work intervals before a long break
    var completedPomodorosInCycle by mutableIntStateOf(0)
        private set
    var totalCompletedToday by mutableIntStateOf(0)
        private set

    // Bound Task for focused work
    var activeTask by mutableStateOf<TaskItem?>(null)
        private set

    // Notification / UI prompt trigger when timer finishes
    var justCompletedMode by mutableStateOf<PomodoroMode?>(null)

    private var countDownTimer: CountDownTimer? = null
    private var appContext: Context? = null

    fun initialize(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        workDurationMinutes = prefs.getInt(KEY_WORK_MINS, 25)
        shortBreakDurationMinutes = prefs.getInt(KEY_SHORT_BREAK_MINS, 5)
        longBreakDurationMinutes = prefs.getInt(KEY_LONG_BREAK_MINS, 15)
        isSoundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        autoStartBreaks = prefs.getBoolean(KEY_AUTO_START_BREAKS, false)

        resetTimerToMode(PomodoroMode.WORK)
    }

    fun updateDurations(
        workMins: Int,
        shortBreakMins: Int,
        longBreakMins: Int,
        soundEnabled: Boolean = isSoundEnabled,
        autoBreaks: Boolean = autoStartBreaks
    ) {
        workDurationMinutes = workMins.coerceIn(1, 120)
        shortBreakDurationMinutes = shortBreakMins.coerceIn(1, 60)
        longBreakDurationMinutes = longBreakMins.coerceIn(1, 60)
        isSoundEnabled = soundEnabled
        autoStartBreaks = autoBreaks

        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            putInt(KEY_WORK_MINS, workDurationMinutes)
            putInt(KEY_SHORT_BREAK_MINS, shortBreakDurationMinutes)
            putInt(KEY_LONG_BREAK_MINS, longBreakDurationMinutes)
            putBoolean(KEY_SOUND_ENABLED, isSoundEnabled)
            putBoolean(KEY_AUTO_START_BREAKS, autoStartBreaks)
            apply()
        }

        if (!isRunning) {
            resetTimerToMode(currentMode)
        }
    }

    fun startFocusOnTask(task: TaskItem?, startImmediately: Boolean = true) {
        activeTask = task
        currentMode = PomodoroMode.WORK
        resetTimerToMode(PomodoroMode.WORK)
        if (startImmediately) {
            startTimer()
        }
    }

    fun startTimer() {
        if (isRunning) return
        isRunning = true
        isPaused = false
        justCompletedMode = null

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remainingMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
            }

            override fun onFinish() {
                remainingMillis = 0L
                isRunning = false
                isPaused = false
                onSessionCompleted()
            }
        }.start()
    }

    fun pauseTimer() {
        if (!isRunning) return
        countDownTimer?.cancel()
        isRunning = false
        isPaused = true
    }

    fun resetTimer() {
        countDownTimer?.cancel()
        isRunning = false
        isPaused = false
        resetTimerToMode(currentMode)
    }

    fun skipToNext() {
        countDownTimer?.cancel()
        isRunning = false
        isPaused = false

        when (currentMode) {
            PomodoroMode.WORK -> {
                // Advance to short or long break
                val nextMode = if ((completedPomodorosInCycle + 1) % 4 == 0) {
                    PomodoroMode.LONG_BREAK
                } else {
                    PomodoroMode.SHORT_BREAK
                }
                currentMode = nextMode
                resetTimerToMode(nextMode)
            }
            PomodoroMode.SHORT_BREAK, PomodoroMode.LONG_BREAK -> {
                currentMode = PomodoroMode.WORK
                resetTimerToMode(PomodoroMode.WORK)
            }
        }
    }

    fun switchToMode(mode: PomodoroMode, startImmediately: Boolean = false) {
        countDownTimer?.cancel()
        isRunning = false
        isPaused = false
        currentMode = mode
        resetTimerToMode(mode)
        if (startImmediately) {
            startTimer()
        }
    }

    fun clearActiveTask() {
        activeTask = null
    }

    private fun resetTimerToMode(mode: PomodoroMode) {
        val durationMins = when (mode) {
            PomodoroMode.WORK -> workDurationMinutes
            PomodoroMode.SHORT_BREAK -> shortBreakDurationMinutes
            PomodoroMode.LONG_BREAK -> longBreakDurationMinutes
        }
        totalSessionMillis = durationMins * 60 * 1000L
        remainingMillis = totalSessionMillis
    }

    private fun onSessionCompleted() {
        appContext?.let { ctx ->
            HapticManager.perform(ctx, HapticType.SUCCESS)
            if (isSoundEnabled) {
                playAlertSound(ctx)
            }
        }

        justCompletedMode = currentMode

        if (currentMode == PomodoroMode.WORK) {
            completedPomodorosInCycle++
            totalCompletedToday++

            // Advance to next break
            val nextBreak = if (completedPomodorosInCycle % 4 == 0) {
                PomodoroMode.LONG_BREAK
            } else {
                PomodoroMode.SHORT_BREAK
            }
            currentMode = nextBreak
            resetTimerToMode(nextBreak)

            if (autoStartBreaks) {
                startTimer()
            }
        } else {
            // Break finished -> back to work
            currentMode = PomodoroMode.WORK
            resetTimerToMode(PomodoroMode.WORK)
        }
    }

    private fun playAlertSound(context: Context) {
        try {
            val alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, alert)
            ringtone?.play()
        } catch (_: Exception) {}
    }

    fun formatRemainingTime(): String {
        val totalSecs = (remainingMillis + 999) / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%02d:%02d".format(mins, secs)
    }

    fun getProgressRatio(): Float {
        if (totalSessionMillis <= 0L) return 0f
        val elapsed = totalSessionMillis - remainingMillis
        return (elapsed.toFloat() / totalSessionMillis).coerceIn(0f, 1f)
    }
}
