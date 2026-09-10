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
    val userId: String = "" // User ID for multi-user isolation
) {
    fun getTimeInMinutes(): Int {
        return TimeFormatHelper.parseToMinutes(time)
    }

    fun getSortValue(): Long {
        return (epochDay * 1440) + getTimeInMinutes()
    }
}
