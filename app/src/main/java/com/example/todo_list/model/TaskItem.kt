package com.example.todo_list.model

import com.example.todo_list.utils.TimeFormatHelper

data class TaskItem(
    val id: String,
    val title: String,
    val category: String, // "Work", "Personal", "Health", "Study"
    val date: String = "Today", // "Today", "Tomorrow", "Oct 25, 2026"
    val time: String, // "09:30 AM" or "09:30"
    val isCompleted: Boolean = false,
    val epochDay: Long = 0L, // Helper for date sorting
    val userId: String = "", // User ID for multi-user isolation
    val priority: String = "NONE", // "HIGH", "MEDIUM", "LOW", "NONE"
    val eisenhowerQuadrant: String = "DO_FIRST", // "DO_FIRST", "SCHEDULE", "DELEGATE", "ELIMINATE"
    val pomodoroSessionsCompleted: Int = 0,
    val pomodoroEstimatedSessions: Int = 1
) {
    fun getTimeInMinutes(): Int {
        return TimeFormatHelper.parseToMinutes(time)
    }

    fun getSortValue(): Long {
        return (epochDay * 1440) + getTimeInMinutes()
    }

    fun getPriorityEnum(): TaskPriority {
        return TaskPriority.fromString(priority)
    }

    fun getEisenhowerQuadrantEnum(): EisenhowerQuadrant {
        return EisenhowerQuadrant.fromString(eisenhowerQuadrant)
    }
}

enum class TaskPriority(
    val key: String,
    val label: String,
    val colorHex: Long,
    val badgeBgHex: Long
) {
    HIGH("HIGH", "High", 0xFFFF3B30, 0x1AFF3B30),
    MEDIUM("MEDIUM", "Medium", 0xFFFF9500, 0x1AFF9500),
    LOW("LOW", "Low", 0xFF007AFF, 0x1A007AFF),
    NONE("NONE", "None", 0xFF8E8E93, 0x1A8E8E93);

    companion object {
        fun fromString(value: String?): TaskPriority {
            return entries.firstOrNull { it.key.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

enum class EisenhowerQuadrant(
    val key: String,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val colorHex: Long,
    val badgeBgHex: Long
) {
    DO_FIRST(
        key = "DO_FIRST",
        title = "Do First",
        subtitle = "Urgent & Important",
        actionLabel = "Do Now",
        colorHex = 0xFFFF3B30, // System Red
        badgeBgHex = 0x1AFF3B30
    ),
    SCHEDULE(
        key = "SCHEDULE",
        title = "Schedule",
        subtitle = "Important & Not Urgent",
        actionLabel = "Plan",
        colorHex = 0xFF007AFF, // System Blue
        badgeBgHex = 0x1A007AFF
    ),
    DELEGATE(
        key = "DELEGATE",
        title = "Delegate",
        subtitle = "Urgent & Not Important",
        actionLabel = "Assign",
        colorHex = 0xFFFF9500, // System Orange
        badgeBgHex = 0x1AFF9500
    ),
    ELIMINATE(
        key = "ELIMINATE",
        title = "Eliminate",
        subtitle = "Not Urgent & Not Important",
        actionLabel = "Drop",
        colorHex = 0xFF8E8E93, // System Gray
        badgeBgHex = 0x1A8E8E93
    );

    companion object {
        fun fromString(value: String?): EisenhowerQuadrant {
            return entries.firstOrNull {
                it.key.equals(value, ignoreCase = true) ||
                it.title.equals(value, ignoreCase = true)
            } ?: DO_FIRST
        }
    }
}
