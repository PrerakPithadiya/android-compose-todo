package com.example.todo_list.ai

import com.example.todo_list.model.FocusStatus
import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskPriority
import com.example.todo_list.model.UserProfile
import com.example.todo_list.utils.TimeFormatHelper

/**
 * On-Device Deterministic Schedule & Priority Optimizer.
 * Operates 100% offline with zero latency, providing mathematical scheduling, conflict resolution,
 * and rule-based Eisenhower priority scoring strictly grounded in the user's local tasks.
 */
object LocalScheduleOptimizer {

    /**
     * Generates a fully grounded daily schedule plan with conflict detection and gap identification.
     */
    fun optimizeDailySchedule(
        tasks: List<TaskItem>,
        profile: UserProfile
    ): SchedulePlanResult {
        val pendingTasks = tasks.filter { !it.isCompleted }
        if (pendingTasks.isEmpty()) {
            return SchedulePlanResult(
                summary = "All scheduled tasks are completed! Enjoy your free time or relax.",
                focusStatusRecommendation = FocusStatus.COFFEE_BREAK.label,
                slots = emptyList(),
                conflicts = emptyList(),
                gaps = emptyList(),
                isFallback = true
            )
        }

        // 1. Detect conflicts (multiple tasks at identical or overlapping times)
        val tasksByTime = pendingTasks.groupBy { it.time.trim() }
        val conflicts = mutableListOf<ScheduleConflict>()

        tasksByTime.forEach { (timeStr, tasksAtTime) ->
            if (tasksAtTime.size > 1) {
                conflicts.add(
                    ScheduleConflict(
                        timeSlot = timeStr,
                        conflictingTaskIds = tasksAtTime.map { it.id },
                        conflictingTaskTitles = tasksAtTime.map { it.title },
                        resolutionSuggestion = "Multiple tasks scheduled at $timeStr. Consider staggering them with 30-minute buffers."
                    )
                )
            }
        }

        // 2. Sort chronologically
        val sortedTasks = pendingTasks.sortedBy { it.getSortValue() }

        // 3. Build scheduled slots with cognitive load spacing
        var lastMinute = -1
        val slots = mutableListOf<ScheduledTaskSlot>()

        sortedTasks.forEachIndexed { index, task ->
            val taskMin = task.getTimeInMinutes()
            val (suggestedTime, rationale) = if (taskMin <= lastMinute) {
                // Adjust conflicted or past-overlapping time by 30 mins
                val adjustedMin = (lastMinute + 30).coerceAtMost(1439)
                val newTimeStr = formatMinutesToAmPm(adjustedMin)
                Pair(newTimeStr, "Staggered from conflict with previous item; gives dedicated focus window.")
            } else {
                val rationaleText = when (task.category.lowercase()) {
                    "work" -> "Scheduled for peak focus and high-impact completion."
                    "study" -> "Dedicated learning block aligned with your productivity rhythm."
                    "health" -> "Health and wellness commitment prioritized on time."
                    else -> "Personal task scheduled during flexible afternoon/evening window."
                }
                Pair(task.time, rationaleText)
            }

            lastMinute = TimeFormatHelper.parseToMinutes(suggestedTime)

            slots.add(
                ScheduledTaskSlot(
                    taskId = task.id,
                    taskTitle = task.title,
                    originalTime = task.time,
                    suggestedTime = suggestedTime,
                    category = task.category,
                    rationale = rationale,
                    isTimeChanged = suggestedTime != task.time
                )
            )
        }

        // 4. Identify gaps (buffers > 90 mins between consecutive tasks)
        val gaps = mutableListOf<ScheduleGap>()
        for (i in 0 until slots.size - 1) {
            val endPrev = TimeFormatHelper.parseToMinutes(slots[i].suggestedTime) + 30 // assumed 30m duration
            val startNext = TimeFormatHelper.parseToMinutes(slots[i + 1].suggestedTime)
            if (startNext - endPrev >= 90) {
                gaps.add(
                    ScheduleGap(
                        startTime = formatMinutesToAmPm(endPrev),
                        endTime = formatMinutesToAmPm(startNext),
                        durationMinutes = startNext - endPrev,
                        recommendation = "Free productivity buffer: Ideal for deep focus, a walk, or lunch break."
                    )
                )
            }
        }

        // 5. Recommended Focus Status
        val workCount = pendingTasks.count { it.category.equals("Work", ignoreCase = true) }
        val focusRecommendation = when {
            workCount >= 3 -> FocusStatus.DEEP_WORK.label
            pendingTasks.size >= 5 -> FocusStatus.IN_A_FLOW.label
            else -> FocusStatus.SHIPPING.label
        }

        val conflictNotice = if (conflicts.isNotEmpty()) " Detected ${conflicts.size} time collision(s) that have been staggered." else ""
        val summary = "Optimized ${pendingTasks.size} pending tasks chronologically.$conflictNotice Keep up the momentum towards your daily goal of ${profile.dailyTaskGoal} tasks!"

        return SchedulePlanResult(
            summary = summary,
            focusStatusRecommendation = focusRecommendation,
            slots = slots,
            conflicts = conflicts,
            gaps = gaps,
            isFallback = true
        )
    }

    /**
     * Auto-suggests Eisenhower Priorities based on keywords, category, and scheduled deadlines.
     */
    fun suggestPriorities(
        tasks: List<TaskItem>,
        profile: UserProfile
    ): PrioritySuggestionResult {
        val pendingTasks = tasks.filter { !it.isCompleted }
        if (pendingTasks.isEmpty()) {
            return PrioritySuggestionResult(
                summary = "No pending tasks to prioritize. All caught up!",
                suggestions = emptyList(),
                isFallback = true
            )
        }

        val suggestions = pendingTasks.map { task ->
            val priority = calculatePriority(task)
            val rationale = generatePriorityRationale(task, priority)
            TaskPrioritySuggestion(
                taskId = task.id,
                taskTitle = task.title,
                currentPriority = task.priority,
                suggestedPriority = priority,
                rationale = rationale
            )
        }

        val highCount = suggestions.count { it.suggestedPriority == TaskPriority.HIGH }
        val medCount = suggestions.count { it.suggestedPriority == TaskPriority.MEDIUM }
        val lowCount = suggestions.count { it.suggestedPriority == TaskPriority.LOW }

        val summary = "Categorized ${suggestions.size} tasks into $highCount High (P1), $medCount Medium (P2), and $lowCount Low (P3) priorities."

        return PrioritySuggestionResult(
            summary = summary,
            suggestions = suggestions,
            isFallback = true
        )
    }

    /**
     * Answers common schedule queries using local grounded data.
     */
    fun answerScheduleQuery(
        query: String,
        tasks: List<TaskItem>,
        profile: UserProfile
    ): AssistantChatResult {
        val pending = tasks.filter { !it.isCompleted }
        val completed = tasks.filter { it.isCompleted }
        val lower = query.lowercase().trim()

        val relevantIds = mutableListOf<String>()
        val answer = when {
            lower.contains("next") || lower.contains("what should i") || lower.contains("upcoming") -> {
                val next = pending.minByOrNull { it.getSortValue() }
                if (next != null) {
                    relevantIds.add(next.id)
                    "Your next scheduled item is \"${next.title}\" (${next.category}) at ${next.time}. Focus on this to maintain productivity momentum!"
                } else {
                    "You have no pending tasks left on your schedule for today."
                }
            }
            lower.contains("conflict") || lower.contains("overlap") -> {
                val tasksByTime = pending.groupBy { it.time }
                val conflicting = tasksByTime.filter { it.value.size > 1 }
                if (conflicting.isNotEmpty()) {
                    val details = conflicting.entries.joinToString("; ") { (time, items) ->
                        items.forEach { relevantIds.add(it.id) }
                        "$time has ${items.size} tasks (${items.joinToString { it.title }})"
                    }
                    "Time conflicts detected: $details. Open the Plan tab to stagger them automatically."
                } else {
                    "No time collisions found! Your current schedule has distinct time slots for each pending task."
                }
            }
            lower.contains("progress") || lower.contains("goal") || lower.contains("how many") -> {
                val percent = if (profile.dailyTaskGoal > 0) ((completed.size.toFloat() / profile.dailyTaskGoal) * 100).toInt() else 100
                "You have completed ${completed.size} out of your daily goal of ${profile.dailyTaskGoal} tasks ($percent% done). You have ${pending.size} pending tasks left."
            }
            lower.contains("priority") || lower.contains("important") -> {
                val highPriority = pending.filter { calculatePriority(it) == TaskPriority.HIGH }
                if (highPriority.isNotEmpty()) {
                    highPriority.forEach { relevantIds.add(it.id) }
                    "Top priority tasks right now: ${highPriority.joinToString { "\"${it.title}\" (${it.time})" }}. Tackle these first!"
                } else {
                    val first = pending.firstOrNull()
                    if (first != null) {
                        relevantIds.add(first.id)
                        "No urgent P1 tasks detected. Your nearest task is \"${first.title}\" at ${first.time}."
                    } else {
                        "No pending tasks found."
                    }
                }
            }
            else -> {
                "Based on your workspace: You have ${pending.size} pending tasks and ${completed.size} completed tasks. Your focus status is ${profile.focusStatus}. Tap 'Optimize Schedule' or 'Auto-Prioritize' for instant planning."
            }
        }

        val followUps = listOf(
            "What should I do next?",
            "Check schedule conflicts",
            "How is my daily goal progress?",
            "Show high priority tasks"
        )

        return AssistantChatResult(
            answer = answer,
            relevantTaskIds = relevantIds,
            quickFollowUps = followUps,
            isFallback = true
        )
    }

    private fun calculatePriority(task: TaskItem): TaskPriority {
        val title = task.title.lowercase()
        val category = task.category.lowercase()
        val minute = task.getTimeInMinutes()

        return when {
            // High priority keywords or critical categories
            title.contains("urgent") || title.contains("asap") || title.contains("deadline") ||
                    title.contains("doctor") || title.contains("dentist") || title.contains("exam") ||
                    title.contains("sync") || title.contains("meeting") || title.contains("review") -> TaskPriority.HIGH

            // Morning work tasks are prime Deep Work
            category == "work" && minute < 720 -> TaskPriority.HIGH

            // Work / Study / Health are important
            category == "work" || category == "study" || category == "health" -> TaskPriority.MEDIUM

            // Personal chores or evening items
            else -> TaskPriority.LOW
        }
    }

    private fun generatePriorityRationale(task: TaskItem, priority: TaskPriority): String {
        return when (priority) {
            TaskPriority.HIGH -> "Time-critical commitment in ${task.category} at ${task.time}. Recommended for immediate or early execution."
            TaskPriority.MEDIUM -> "High-value ${task.category} milestone contributing to your daily productivity goals."
            TaskPriority.LOW -> "Flexible ${task.category} item suitable for afternoon or secondary focus time."
            TaskPriority.NONE -> "Unprioritized task."
        }
    }

    private fun formatMinutesToAmPm(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        val amPm = if (h < 12) "AM" else "PM"
        val displayH = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format("%02d:%02d %s", displayH, m, amPm)
    }
}
