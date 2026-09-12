package com.example.todo_list.ai

import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskPriority

/**
 * Scheduled slot for a task in the optimized daily plan.
 */
data class ScheduledTaskSlot(
    val taskId: String,
    val taskTitle: String,
    val originalTime: String,
    val suggestedTime: String,
    val category: String,
    val rationale: String,
    val isTimeChanged: Boolean = originalTime != suggestedTime
)

/**
 * Time conflict detected between two or more tasks.
 */
data class ScheduleConflict(
    val timeSlot: String,
    val conflictingTaskIds: List<String>,
    val conflictingTaskTitles: List<String>,
    val resolutionSuggestion: String
)

/**
 * Recommended restorative buffer or focus gap.
 */
data class ScheduleGap(
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val recommendation: String // e.g. "Rest & Hydration buffer", "Deep Work focus block"
)

/**
 * Full schedule optimization plan.
 */
data class SchedulePlanResult(
    val summary: String,
    val focusStatusRecommendation: String,
    val slots: List<ScheduledTaskSlot>,
    val conflicts: List<ScheduleConflict>,
    val gaps: List<ScheduleGap>,
    val isFallback: Boolean = false
)

/**
 * Priority suggestion for a single task.
 */
data class TaskPrioritySuggestion(
    val taskId: String,
    val taskTitle: String,
    val currentPriority: String,
    val suggestedPriority: TaskPriority,
    val rationale: String
)

/**
 * Full priority ranking result.
 */
data class PrioritySuggestionResult(
    val summary: String,
    val suggestions: List<TaskPrioritySuggestion>,
    val isFallback: Boolean = false
)

/**
 * Response from the grounded conversational assistant.
 */
data class AssistantChatResult(
    val answer: String,
    val relevantTaskIds: List<String> = emptyList(),
    val quickFollowUps: List<String> = emptyList(),
    val isFallback: Boolean = false
)
