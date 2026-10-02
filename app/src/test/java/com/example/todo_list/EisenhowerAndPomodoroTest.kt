package com.example.todo_list

import com.example.todo_list.manager.PomodoroMode
import com.example.todo_list.manager.PomodoroTimerManager
import com.example.todo_list.model.EisenhowerQuadrant
import com.example.todo_list.model.TaskItem
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Eisenhower Matrix quadrants and Pomodoro Focus Timer models & state logic.
 */
class EisenhowerAndPomodoroTest {

    @Test
    fun testEisenhowerQuadrantParsing() {
        assertEquals(EisenhowerQuadrant.DO_FIRST, EisenhowerQuadrant.fromString("DO_FIRST"))
        assertEquals(EisenhowerQuadrant.SCHEDULE, EisenhowerQuadrant.fromString("SCHEDULE"))
        assertEquals(EisenhowerQuadrant.DELEGATE, EisenhowerQuadrant.fromString("DELEGATE"))
        assertEquals(EisenhowerQuadrant.ELIMINATE, EisenhowerQuadrant.fromString("ELIMINATE"))
        // Case-insensitivity & fallback
        assertEquals(EisenhowerQuadrant.DO_FIRST, EisenhowerQuadrant.fromString("do_first"))
        assertEquals(EisenhowerQuadrant.DO_FIRST, EisenhowerQuadrant.fromString("unknown_quadrant"))
        assertEquals(EisenhowerQuadrant.SCHEDULE, EisenhowerQuadrant.fromString("Schedule"))
    }

    @Test
    fun testTaskItemQuadrantAndPomodoroDefaults() {
        val task = TaskItem(
            id = "test-1",
            title = "Test Task",
            category = "Work",
            time = "10:00 AM"
        )

        assertEquals("DO_FIRST", task.eisenhowerQuadrant)
        assertEquals(EisenhowerQuadrant.DO_FIRST, task.getEisenhowerQuadrantEnum())
        assertEquals(0, task.pomodoroSessionsCompleted)
        assertEquals(1, task.pomodoroEstimatedSessions)

        val updatedTask = task.copy(
            eisenhowerQuadrant = "SCHEDULE",
            pomodoroSessionsCompleted = 2,
            pomodoroEstimatedSessions = 4
        )

        assertEquals(EisenhowerQuadrant.SCHEDULE, updatedTask.getEisenhowerQuadrantEnum())
        assertEquals(2, updatedTask.pomodoroSessionsCompleted)
        assertEquals(4, updatedTask.pomodoroEstimatedSessions)
    }

    @Test
    fun testPomodoroTimerDefaultsAndFormatting() {
        assertEquals(25, PomodoroMode.WORK.defaultMinutes)
        assertEquals(5, PomodoroMode.SHORT_BREAK.defaultMinutes)
        assertEquals(15, PomodoroMode.LONG_BREAK.defaultMinutes)
    }

    @Test
    fun testPomodoroSwitchToMode() {
        PomodoroTimerManager.switchToMode(PomodoroMode.LONG_BREAK)
        assertEquals(PomodoroMode.LONG_BREAK, PomodoroTimerManager.currentMode)
        assertEquals(PomodoroTimerManager.longBreakDurationMinutes * 60 * 1000L, PomodoroTimerManager.remainingMillis)
        assertFalse(PomodoroTimerManager.isRunning)

        PomodoroTimerManager.switchToMode(PomodoroMode.SHORT_BREAK)
        assertEquals(PomodoroMode.SHORT_BREAK, PomodoroTimerManager.currentMode)
        assertEquals(PomodoroTimerManager.shortBreakDurationMinutes * 60 * 1000L, PomodoroTimerManager.remainingMillis)
        assertFalse(PomodoroTimerManager.isRunning)

        PomodoroTimerManager.switchToMode(PomodoroMode.WORK)
        assertEquals(PomodoroMode.WORK, PomodoroTimerManager.currentMode)
        assertEquals(PomodoroTimerManager.workDurationMinutes * 60 * 1000L, PomodoroTimerManager.remainingMillis)
        assertFalse(PomodoroTimerManager.isRunning)
    }
}
