package com.example.todo_list.model

import androidx.compose.ui.graphics.Color
import com.example.todo_list.ui.theme.*

data class TaskListCategory(
    val id: String,
    val name: String,
    val colorHex: String,
    val iconName: String = "List",
    val isSystemDefault: Boolean = false
) {
    fun getColor(): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            getCategoryColor(name)
        }
    }

    fun getEmoji(): String {
        return when (iconName.lowercase()) {
            "work", "briefcase" -> "💼"
            "personal", "person" -> "👤"
            "health", "fitness" -> "🏋️"
            "study", "book" -> "📚"
            "shopping", "cart" -> "🛒"
            "star" -> "⭐"
            "flag" -> "🚩"
            "heart" -> "❤️"
            "home" -> "🏠"
            "money", "finance" -> "💰"
            "travel", "plane" -> "✈️"
            "music" -> "🎵"
            "code", "tech" -> "💻"
            else -> when (name.lowercase()) {
                "work" -> "💼"
                "personal" -> "👤"
                "health" -> "🏋️"
                "study" -> "📚"
                "shopping", "groceries" -> "🛒"
                "home" -> "🏠"
                "fitness" -> "🏋️"
                else -> "📁"
            }
        }
    }

    companion object {
        val DEFAULT_CATEGORIES = listOf(
            TaskListCategory("cat_work", "Work", "#007AFF", "Work", true),
            TaskListCategory("cat_personal", "Personal", "#AF52DE", "Personal", true),
            TaskListCategory("cat_health", "Health", "#34C759", "Health", true),
            TaskListCategory("cat_study", "Study", "#FF9500", "Study", true)
        )
    }
}
