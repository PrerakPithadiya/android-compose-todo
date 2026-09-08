package com.example.todo_list.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.todo_list.model.TaskListCategory

/**
 * Room Entity representing a task list category stored in SQLite.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorHex: String,
    val iconName: String = "List",
    val isSystemDefault: Boolean = false,
    val displayOrder: Int = 0
) {
    fun toModel(): TaskListCategory {
        return TaskListCategory(
            id = id,
            name = name,
            colorHex = colorHex,
            iconName = iconName,
            isSystemDefault = isSystemDefault
        )
    }

    companion object {
        fun fromModel(model: TaskListCategory, displayOrder: Int = 0): CategoryEntity {
            return CategoryEntity(
                id = model.id,
                name = model.name,
                colorHex = model.colorHex,
                iconName = model.iconName,
                isSystemDefault = model.isSystemDefault,
                displayOrder = displayOrder
            )
        }
    }
}

fun TaskListCategory.toEntity(displayOrder: Int = 0): CategoryEntity =
    CategoryEntity.fromModel(this, displayOrder)
