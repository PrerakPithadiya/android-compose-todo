package com.example.todo_list.model

import androidx.compose.ui.graphics.Color
import com.example.todo_list.ui.theme.AppleHealth
import com.example.todo_list.ui.theme.ApplePersonal
import com.example.todo_list.ui.theme.AppleStudy
import com.example.todo_list.ui.theme.SystemBlue

/**
 * Standardized High-Level Life Domains for Retrospective Analysis.
 */
enum class LifeDomain(
    val title: String,
    val emoji: String,
    val colorHex: Long
) {
    PROFESSIONAL("Professional", "💼", 0xFF007AFF),   // System Blue
    STUDY("Study & Learning", "📚", 0xFFFF9500),      // Apple Study / Orange
    FITNESS("Fitness & Health", "🏋️", 0xFF34C759),    // Apple Health / Green
    HOBBIES("Hobbies & Personal", "🎨", 0xFFAF52DE);  // Apple Personal / Purple

    fun getColor(): Color = Color(colorHex)

    companion object {
        fun fromCategoryName(name: String): LifeDomain {
            val lower = name.lowercase().trim()
            return when {
                lower.contains("work") || lower.contains("pro") || lower.contains("career") || lower.contains("business") || lower.contains("code") -> PROFESSIONAL
                lower.contains("study") || lower.contains("learn") || lower.contains("book") || lower.contains("read") || lower.contains("exam") -> STUDY
                lower.contains("health") || lower.contains("fit") || lower.contains("gym") || lower.contains("workout") || lower.contains("run") || lower.contains("sport") -> FITNESS
                else -> HOBBIES
            }
        }
    }
}

/**
 * Domain distribution metrics for charts and progress bars.
 */
data class DomainMetric(
    val domain: LifeDomain,
    val completedTasks: Int,
    val totalTasks: Int,
    val estimatedHours: Double,
    val percentageOfTotal: Float
)

/**
 * Retrospective Review Period: Weekly vs Monthly.
 */
enum class ReviewPeriod(val title: String, val comparisonLabel: String) {
    WEEKLY("Weekly Review", "vs Prior Week"),
    MONTHLY("Monthly Review", "vs Prior Month")
}

/**
 * Trend state for Month-over-Month or Week-over-Week.
 */
enum class GrowthTrend {
    GROWTH,
    CONSISTENT,
    DIP
}

/**
 * Complete aggregated retrospective data model.
 */
data class RetrospectiveReport(
    val period: ReviewPeriod,
    val totalHoursLogged: Double,
    val completedTasksCount: Int,
    val totalTasksCount: Int,
    val completionRatePercentage: Int,
    val activeFocusSessions: Int,
    val domainMetrics: List<DomainMetric>,
    val growthPercentage: Double,
    val trend: GrowthTrend,
    val adaptiveFeedbackTitle: String,
    val adaptiveFeedbackMessage: String,
    val adaptiveGratitudePrompt: String? = null,
    val rewardPointsBonus: Int = 0,
    val badgeUnlockedTitle: String? = null
)
