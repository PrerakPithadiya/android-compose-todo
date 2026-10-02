package com.example.todo_list

import com.example.todo_list.manager.AnalyticsEngine
import com.example.todo_list.model.GrowthTrend
import com.example.todo_list.model.LifeDomain
import com.example.todo_list.model.ReviewPeriod
import com.example.todo_list.model.TaskItem
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying AnalyticsEngine calculation accuracy, domain aggregation,
 * MoM trend detection, and adaptive feedback messages.
 */
class AnalyticsEngineTest {

    private val sampleTasks = listOf(
        TaskItem(id = "1", title = "Write Android Tests", category = "Work", isCompleted = true, pomodoroSessionsCompleted = 2, time = "10:00 AM"),
        TaskItem(id = "2", title = "Read Jetpack Docs", category = "Study", isCompleted = true, pomodoroSessionsCompleted = 1, time = "11:00 AM"),
        TaskItem(id = "3", title = "Morning Gym Run", category = "Health", isCompleted = true, pomodoroSessionsCompleted = 1, time = "07:00 AM"),
        TaskItem(id = "4", title = "Guitar Practice", category = "Personal", isCompleted = false, pomodoroSessionsCompleted = 0, time = "08:00 PM"),
        TaskItem(id = "5", title = "Architecture Refactor", category = "Work", isCompleted = true, pomodoroSessionsCompleted = 2, time = "02:00 PM")
    )

    @Test
    fun testDomainMapping() {
        assertEquals(LifeDomain.PROFESSIONAL, LifeDomain.fromCategoryName("Work"))
        assertEquals(LifeDomain.PROFESSIONAL, LifeDomain.fromCategoryName("Coding Project"))
        assertEquals(LifeDomain.STUDY, LifeDomain.fromCategoryName("Study"))
        assertEquals(LifeDomain.STUDY, LifeDomain.fromCategoryName("Books & Reading"))
        assertEquals(LifeDomain.FITNESS, LifeDomain.fromCategoryName("Health"))
        assertEquals(LifeDomain.FITNESS, LifeDomain.fromCategoryName("Gym Fitness"))
        assertEquals(LifeDomain.HOBBIES, LifeDomain.fromCategoryName("Personal"))
        assertEquals(LifeDomain.HOBBIES, LifeDomain.fromCategoryName("Music & Fun"))
    }

    @Test
    fun testWorkOutputAndCompletionRate() {
        val report = AnalyticsEngine.generateReport(
            tasks = sampleTasks,
            period = ReviewPeriod.WEEKLY,
            simulatedPriorCompletionRate = 60.0,
            pomodoroFocusMinutes = 25
        )

        assertEquals(5, report.totalTasksCount)
        assertEquals(4, report.completedTasksCount)
        assertEquals(80, report.completionRatePercentage) // 4 / 5 = 80%
        assertTrue(report.totalHoursLogged > 0.0)
        assertEquals(6, report.activeFocusSessions) // 2 + 1 + 1 + 0 + 2 = 6 sessions
    }

    @Test
    fun testDomainDistributionMetrics() {
        val report = AnalyticsEngine.generateReport(
            tasks = sampleTasks,
            period = ReviewPeriod.MONTHLY,
            simulatedPriorCompletionRate = 50.0
        )

        val workMetric = report.domainMetrics.first { it.domain == LifeDomain.PROFESSIONAL }
        assertEquals(2, workMetric.totalTasks)
        assertEquals(2, workMetric.completedTasks)
        assertEquals(40.0f, workMetric.percentageOfTotal, 0.01f) // 2 of 5 = 40%

        val personalMetric = report.domainMetrics.first { it.domain == LifeDomain.HOBBIES }
        assertEquals(1, personalMetric.totalTasks)
        assertEquals(0, personalMetric.completedTasks)
    }

    @Test
    fun testGrowthTrendDetectionAndAdaptiveRewards() {
        // Prior was 60%, current is 80% -> Growth of +20%
        val growthReport = AnalyticsEngine.generateReport(
            tasks = sampleTasks,
            period = ReviewPeriod.MONTHLY,
            simulatedPriorCompletionRate = 60.0
        )

        assertEquals(GrowthTrend.GROWTH, growthReport.trend)
        assertEquals(20.0, growthReport.growthPercentage, 0.01)
        assertTrue(growthReport.rewardPointsBonus >= 150)
        assertNotNull(growthReport.badgeUnlockedTitle)
        assertNull(growthReport.adaptiveGratitudePrompt)
        assertTrue(growthReport.adaptiveFeedbackTitle.contains("Surge"))
    }

    @Test
    fun testDipTrendDetectionAndSupportiveGratitudePrompt() {
        // Prior was 90%, current is 80% -> Dip of -10%
        val dipReport = AnalyticsEngine.generateReport(
            tasks = sampleTasks,
            period = ReviewPeriod.WEEKLY,
            simulatedPriorCompletionRate = 90.0
        )

        assertEquals(GrowthTrend.DIP, dipReport.trend)
        assertEquals(-10.0, dipReport.growthPercentage, 0.01)
        assertNotNull(dipReport.adaptiveGratitudePrompt) // Empathetic, supportive prompt
        assertNull(dipReport.badgeUnlockedTitle) // No punitive messaging
        assertTrue(dipReport.adaptiveFeedbackTitle.contains("Recharge"))
        assertTrue(dipReport.rewardPointsBonus > 0) // Gentle encouragement points
    }
}
