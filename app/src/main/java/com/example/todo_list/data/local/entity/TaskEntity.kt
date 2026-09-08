package com.example.todo_list.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.todo_list.model.TaskItem

/**
 * Room Entity representing a task item stored in SQLite.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String,
    val date: String = "Today",
    val time: String,
    val isCompleted: Boolean = false,
    val epochDay: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toModel(): TaskItem {
        return TaskItem(
            id = id,
            title = title,
            category = category,
            date = date,
            time = time,
            isCompleted = isCompleted,
            epochDay = epochDay
        )
    }

    companion object {
        fun fromModel(model: TaskItem, createdAt: Long = System.currentTimeMillis()): TaskEntity {
            return TaskEntity(
                id = model.id,
                title = model.title,
                category = model.category,
                date = model.date,
                time = model.time,
                isCompleted = model.isCompleted,
                epochDay = model.epochDay,
                createdAt = createdAt
            )
        }
    }
}

fun TaskItem.toEntity(createdAt: Long = System.currentTimeMillis()): TaskEntity =
    TaskEntity.fromModel(this, createdAt)
