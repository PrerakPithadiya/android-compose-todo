package com.example.todo_list.ai

import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskPriority
import com.example.todo_list.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

class LocalScheduleOptimizerTest {

    @Test
    fun testConflictDetection_multipleTasksAtSameTime() {
        val tasks = listOf(
            TaskItem(id = "1", title = "Team sync", category = "Work", date = "Today", time = "02:00 PM"),
            TaskItem(id = "2", title = "Dentist appointment", category = "Health", date = "Today", time = "02:00 PM"),
            TaskItem(id = "3", title = "Evening Walk", category = "Personal", date = "Today", time = "06:00 PM")
        )
        val profile = UserProfile()

        val plan = LocalScheduleOptimizer.optimizeDailySchedule(tasks, profile)

        // Conflict should be detected at 02:00 PM
        assertEquals(1, plan.conflicts.size)
        assertEquals("02:00 PM", plan.conflicts[0].timeSlot)
        assertEquals(2, plan.conflicts[0].conflictingTaskIds.size)
        assertTrue(plan.conflicts[0].conflictingTaskIds.contains("1"))
        assertTrue(plan.conflicts[0].conflictingTaskIds.contains("2"))

        // Conflicted task should be staggered in suggested slots
        val slot1 = plan.slots.find { it.taskId == "1" }
        val slot2 = plan.slots.find { it.taskId == "2" }
        assertNotNull(slot1)
        assertNotNull(slot2)
        assertNotEquals(slot1?.suggestedTime, slot2?.suggestedTime)
    }

    @Test
    fun testPriorityCalculation_workVsPersonal() {
        val tasks = listOf(
            TaskItem(id = "1", title = "Urgent Client Review", category = "Work", date = "Today", time = "09:30 AM"),
            TaskItem(id = "2", title = "Read Android Docs", category = "Study", date = "Today", time = "03:00 PM"),
            TaskItem(id = "3", title = "Buy Groceries", category = "Personal", date = "Today", time = "07:00 PM")
        )
        val profile = UserProfile()

        val result = LocalScheduleOptimizer.suggestPriorities(tasks, profile)
        assertEquals(3, result.suggestions.size)

        val urgentWork = result.suggestions.find { it.taskId == "1" }
        val studyTask = result.suggestions.find { it.taskId == "2" }
        val personalTask = result.suggestions.find { it.taskId == "3" }

        assertEquals(TaskPriority.HIGH, urgentWork?.suggestedPriority)
        assertEquals(TaskPriority.MEDIUM, studyTask?.suggestedPriority)
        assertEquals(TaskPriority.LOW, personalTask?.suggestedPriority)
    }

    @Test
    fun testEmptyTaskList_handledGracefully() {
        val tasks = emptyList<TaskItem>()
        val profile = UserProfile()

        val plan = LocalScheduleOptimizer.optimizeDailySchedule(tasks, profile)
        assertTrue(plan.slots.isEmpty())
        assertTrue(plan.conflicts.isEmpty())

        val prio = LocalScheduleOptimizer.suggestPriorities(tasks, profile)
        assertTrue(prio.suggestions.isEmpty())
    }
}
