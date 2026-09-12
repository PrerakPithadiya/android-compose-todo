package com.example.todo_list.ai

import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskPriority
import com.example.todo_list.utils.TimeFormatHelper

/**
 * Anti-Hallucination Cross-Validation Gatekeeper.
 * Mathematically ensures that all suggestions, task IDs, and schedules returned by the LLM
 * are strictly grounded in existing SQLite database records, discarding any phantom or hallucinated data.
 */
object TaskFlowCrossValidator {

    /**
     * Validates and filters schedule slots against the user's known tasks.
     * Discards any hallucinated task IDs that do not exist in the database.
     */
    fun validateSchedulePlan(
        rawPlan: SchedulePlanResult,
        knownTasks: List<TaskItem>
    ): SchedulePlanResult {
        val taskMapById = knownTasks.associateBy { it.id }
        val taskMapByTitle = knownTasks.associateBy { it.title.lowercase().trim() }

        val validatedSlots = mutableListOf<ScheduledTaskSlot>()

        for (slot in rawPlan.slots) {
            // 1. Verify existence by ID or exact title
            val matchingTask = taskMapById[slot.taskId]
                ?: taskMapByTitle[slot.taskTitle.lowercase().trim()]

            if (matchingTask != null) {
                // Verify time format validity
                val isValidTime = TimeFormatHelper.parseToMinutes(slot.suggestedTime) >= 0
                val safeSuggestedTime = if (isValidTime) slot.suggestedTime else matchingTask.time

                validatedSlots.add(
                    slot.copy(
                        taskId = matchingTask.id,
                        taskTitle = matchingTask.title,
                        category = matchingTask.category,
                        originalTime = matchingTask.time,
                        suggestedTime = safeSuggestedTime,
                        isTimeChanged = safeSuggestedTime != matchingTask.time
                    )
                )
            } else {
                // Discard hallucinated task!
                try {
                    android.util.Log.w("TaskFlowCrossValidator", "Discarded hallucinated task: ${slot.taskTitle} (ID: ${slot.taskId})")
                } catch (_: Throwable) {
                    // Ignored in unit test JVM environments
                }
            }
        }

        // Validate conflicts
        val validatedConflicts = rawPlan.conflicts.mapNotNull { conflict ->
            val validIds = conflict.conflictingTaskIds.filter { taskMapById.containsKey(it) }
            if (validIds.size >= 2) {
                conflict.copy(
                    conflictingTaskIds = validIds,
                    conflictingTaskTitles = validIds.mapNotNull { taskMapById[it]?.title }
                )
            } else null
        }

        return rawPlan.copy(
            slots = validatedSlots,
            conflicts = validatedConflicts
        )
    }

    /**
     * Validates and filters priority suggestions against the user's known tasks.
     * Discards any phantom task IDs.
     */
    fun validatePrioritySuggestions(
        rawResult: PrioritySuggestionResult,
        knownTasks: List<TaskItem>
    ): PrioritySuggestionResult {
        val taskMapById = knownTasks.associateBy { it.id }
        val taskMapByTitle = knownTasks.associateBy { it.title.lowercase().trim() }

        val validatedSuggestions = mutableListOf<TaskPrioritySuggestion>()

        for (suggestion in rawResult.suggestions) {
            val matchingTask = taskMapById[suggestion.taskId]
                ?: taskMapByTitle[suggestion.taskTitle.lowercase().trim()]

            if (matchingTask != null) {
                validatedSuggestions.add(
                    suggestion.copy(
                        taskId = matchingTask.id,
                        taskTitle = matchingTask.title,
                        currentPriority = matchingTask.priority,
                        suggestedPriority = suggestion.suggestedPriority
                    )
                )
            } else {
                // Discard hallucinated priority
                try {
                    android.util.Log.w("TaskFlowCrossValidator", "Discarded hallucinated priority for: ${suggestion.taskTitle}")
                } catch (_: Throwable) {
                    // Ignored in unit test JVM environments
                }
            }
        }

        return rawResult.copy(
            suggestions = validatedSuggestions
        )
    }

    /**
     * Sanitizes chat results to ensure only real task IDs are returned as relevant references.
     */
    fun validateChatResult(
        chatResult: AssistantChatResult,
        knownTasks: List<TaskItem>
    ): AssistantChatResult {
        val validIdsSet = knownTasks.map { it.id }.toSet()
        val filteredIds = chatResult.relevantTaskIds.filter { validIdsSet.contains(it) }
        return chatResult.copy(relevantTaskIds = filteredIds)
    }
}
