package com.example.todo_list.ai

import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskPriority
import org.junit.Assert.*
import org.junit.Test

class TaskFlowCrossValidatorTest {

    @Test
    fun testValidateSchedulePlan_discardsHallucinatedTaskId() {
        val knownTasks = listOf(
            TaskItem(id = "valid-id-1", title = "Real Task 1", category = "Work", time = "09:00 AM"),
            TaskItem(id = "valid-id-2", title = "Real Task 2", category = "Personal", time = "11:00 AM")
        )

        // Raw plan returned by LLM contains 1 real task and 1 hallucinated phantom task
        val rawPlan = SchedulePlanResult(
            summary = "Sample optimization",
            focusStatusRecommendation = "🎯 Deep Work",
            slots = listOf(
                ScheduledTaskSlot(
                    taskId = "valid-id-1",
                    taskTitle = "Real Task 1",
                    originalTime = "09:00 AM",
                    suggestedTime = "09:00 AM",
                    category = "Work",
                    rationale = "Grounded"
                ),
                ScheduledTaskSlot(
                    taskId = "phantom-id-999",
                    taskTitle = "Hallucinated Phantom Task",
                    originalTime = "01:00 PM",
                    suggestedTime = "01:00 PM",
                    category = "Imaginary",
                    rationale = "AI Hallucination"
                )
            ),
            conflicts = emptyList(),
            gaps = emptyList()
        )

        val validated = TaskFlowCrossValidator.validateSchedulePlan(rawPlan, knownTasks)

        // Only the valid task must pass through; the phantom task must be discarded!
        assertEquals(1, validated.slots.size)
        assertEquals("valid-id-1", validated.slots[0].taskId)
        assertEquals("Real Task 1", validated.slots[0].taskTitle)
    }

    @Test
    fun testValidatePrioritySuggestions_discardsNonExistentTask() {
        val knownTasks = listOf(
            TaskItem(id = "real-1", title = "Write Code", category = "Work", time = "10:00 AM")
        )

        val rawResult = PrioritySuggestionResult(
            summary = "Priorities",
            suggestions = listOf(
                TaskPrioritySuggestion(
                    taskId = "real-1",
                    taskTitle = "Write Code",
                    currentPriority = "NONE",
                    suggestedPriority = TaskPriority.HIGH,
                    rationale = "Real task"
                ),
                TaskPrioritySuggestion(
                    taskId = "hallucinated-2",
                    taskTitle = "Never Existed Task",
                    currentPriority = "NONE",
                    suggestedPriority = TaskPriority.LOW,
                    rationale = "Fake"
                )
            )
        )

        val validated = TaskFlowCrossValidator.validatePrioritySuggestions(rawResult, knownTasks)
        assertEquals(1, validated.suggestions.size)
        assertEquals("real-1", validated.suggestions[0].taskId)
    }
}
