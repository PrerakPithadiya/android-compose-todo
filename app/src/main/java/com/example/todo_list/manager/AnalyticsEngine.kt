package com.example.todo_list.manager

import com.example.todo_list.model.DomainMetric
import com.example.todo_list.model.GrowthTrend
import com.example.todo_list.model.LifeDomain
import com.example.todo_list.model.RetrospectiveReport
import com.example.todo_list.model.ReviewPeriod
import com.example.todo_list.model.TaskItem
import kotlin.math.roundToInt

/**
 * High-performance analytics calculation engine for weekly & monthly reviews,
 * domain breakdown distributions, MoM trend growth, and adaptive feedback.
 */
object AnalyticsEngine {

    /**
     * Generates a structured Retrospective Report for the specified period.
     * @param tasks The active user's task list.
     * @param period Weekly or Monthly review.
     * @param simulatedPriorCompletionRate Optional prior period completion rate to compute exact MoM growth.
     */
    fun generateReport(
        tasks: List<TaskItem>,
        period: ReviewPeriod,
        simulatedPriorCompletionRate: Double? = null,
        pomodoroFocusMinutes: Int = PomodoroTimerManager.workDurationMinutes
    ): RetrospectiveReport {
        val totalTasksCount = tasks.size
        val completedTasksCount = tasks.count { it.isCompleted }

        val completionRate = if (totalTasksCount > 0) {
            ((completedTasksCount.toDouble() / totalTasksCount.toDouble()) * 100.0).roundToInt()
        } else {
            0
        }

        // Active focus sessions calculation
        val totalPomodoroSessionsInTasks = tasks.sumOf { it.pomodoroSessionsCompleted }
        val activeFocusSessions = totalPomodoroSessionsInTasks.coerceAtLeast(PomodoroTimerManager.totalCompletedToday)

        // Estimated hours logged:
        // Focus time (active focus sessions * duration in hours) + base completed task duration (approx 20 mins per task)
        val focusSessionHours = (activeFocusSessions * pomodoroFocusMinutes) / 60.0
        val baseTaskHours = (completedTasksCount * 25) / 60.0
        val totalHoursLogged = ((focusSessionHours + baseTaskHours) * 10.0).roundToInt() / 10.0

        // Domain Breakdown Distribution
        val domainGroups = tasks.groupBy { LifeDomain.fromCategoryName(it.category) }
        val domainMetrics = LifeDomain.entries.map { domain ->
            val domainTasks = domainGroups[domain] ?: emptyList()
            val domainTotal = domainTasks.size
            val domainCompleted = domainTasks.count { it.isCompleted }
            val domainHours = ((domainCompleted * 25 + domainTasks.sumOf { it.pomodoroSessionsCompleted } * pomodoroFocusMinutes) / 60.0 * 10.0).roundToInt() / 10.0
            val percentage = if (totalTasksCount > 0) {
                (domainTotal.toFloat() / totalTasksCount.toFloat()) * 100f
            } else {
                0f
            }

            DomainMetric(
                domain = domain,
                completedTasks = domainCompleted,
                totalTasks = domainTotal,
                estimatedHours = domainHours,
                percentageOfTotal = percentage
            )
        }

        // Month-over-Month / Period-over-Period Growth Calculation
        val baselinePrior = simulatedPriorCompletionRate ?: when (period) {
            ReviewPeriod.WEEKLY -> 72.0
            ReviewPeriod.MONTHLY -> 68.0
        }

        val growthDelta = ((completionRate - baselinePrior) * 10.0).roundToInt() / 10.0
        val trend = when {
            growthDelta > 2.0 -> GrowthTrend.GROWTH
            growthDelta < -2.0 -> GrowthTrend.DIP
            else -> GrowthTrend.CONSISTENT
        }

        // Adaptive Feedback Mechanism
        val (title, message, gratitudePrompt, rewardBonus, badgeTitle) = when (trend) {
            GrowthTrend.GROWTH -> {
                val bonus = if (period == ReviewPeriod.MONTHLY) 350 else 150
                AdaptiveFeedback(
                    title = "Productivity Surge Detected! 🚀",
                    message = "Outstanding momentum! Your completion rate increased by +${growthDelta}% compared to the prior period. You've locked in deep focus.",
                    gratitudePrompt = null,
                    rewardBonus = bonus,
                    badgeTitle = if (period == ReviewPeriod.MONTHLY) "Monthly Vanguard Medal" else "Weekly Flow Champion"
                )
            }
            GrowthTrend.DIP -> {
                AdaptiveFeedback(
                    title = "A Gentle Moment to Recharge 🌿",
                    message = "Every rhythm has natural tides. Rest and pacing are vital for long-term endurance. Be kind to yourself today.",
                    gratitudePrompt = "Take 30 seconds to acknowledge one small win or effort you made this week that made you smile.",
                    rewardBonus = 50, // Supportive token points
                    badgeTitle = null
                )
            }
            GrowthTrend.CONSISTENT -> {
                AdaptiveFeedback(
                    title = "Rock-Solid Consistency ⚡",
                    message = "You are maintaining a steady, reliable rhythm across your goals with minimal variance. Steady habits compound into greatness.",
                    gratitudePrompt = null,
                    rewardBonus = 75,
                    badgeTitle = "Consistency Keystone"
                )
            }
        }

        return RetrospectiveReport(
            period = period,
            totalHoursLogged = totalHoursLogged,
            completedTasksCount = completedTasksCount,
            totalTasksCount = totalTasksCount,
            completionRatePercentage = completionRate,
            activeFocusSessions = activeFocusSessions,
            domainMetrics = domainMetrics,
            growthPercentage = growthDelta,
            trend = trend,
            adaptiveFeedbackTitle = title,
            adaptiveFeedbackMessage = message,
            adaptiveGratitudePrompt = gratitudePrompt,
            rewardPointsBonus = rewardBonus,
            badgeUnlockedTitle = badgeTitle
        )
    }

    private data class AdaptiveFeedback(
        val title: String,
        val message: String,
        val gratitudePrompt: String?,
        val rewardBonus: Int,
        val badgeTitle: String?
    )
}
