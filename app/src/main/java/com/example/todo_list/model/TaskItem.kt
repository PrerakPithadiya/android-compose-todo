package com.example.todo_list.model

data class TaskItem(
    val id: String,
    val title: String,
    val category: String, // "Work", "Personal", "Health", "Study"
    val date: String = "Today", // "Today", "Tomorrow", "Oct 25, 2026"
    val time: String, // "09:30 AM"
    val isCompleted: Boolean = false,
    val epochDay: Long = 0L // Helper for date sorting
) {
    fun getTimeInMinutes(): Int {
        return try {
            val parts = time.trim().split(" ")
            val timeParts = parts[0].split(":")
            var hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val amPm = parts.getOrNull(1)?.uppercase() ?: "AM"

            if (amPm == "PM" && hour < 12) hour += 12
            if (amPm == "AM" && hour == 12) hour = 0

            hour * 60 + minute
        } catch (e: Exception) {
            0
        }
    }

    fun getSortValue(): Long {
        return (epochDay * 1440) + getTimeInMinutes()
    }
}
