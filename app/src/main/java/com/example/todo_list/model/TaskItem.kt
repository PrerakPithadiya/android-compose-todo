package com.example.todo_list.model

data class TaskItem(
    val id: String,
    val title: String,
    val category: String, // "Work", "Personal", etc.
    val time: String, // "09:30 AM"
    val isCompleted: Boolean = false
)
