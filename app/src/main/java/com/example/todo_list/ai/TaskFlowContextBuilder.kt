package com.example.todo_list.ai

import com.example.todo_list.manager.TimePreferencesManager
import com.example.todo_list.manager.UserProfileManager
import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskListCategory
import com.example.todo_list.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Serializes the user's complete real-time TaskFlow database into a structured, grounded JSON context.
 * Enables the LLM to inspect tasks, categories, profile focus status, and schedule constraints with zero hallucination.
 */
object TaskFlowContextBuilder {

    fun buildUserContext(
        tasks: List<TaskItem>,
        categories: List<TaskListCategory> = TaskListCategory.DEFAULT_CATEGORIES,
        profile: UserProfile = UserProfileManager.profile
    ): String {
        val root = JSONObject()

        // 1. System Environment & Time Constraints
        val tzId = TimePreferencesManager.selectedLocation.timeZoneId
        val tz = TimeZone.getTimeZone(tzId)
        val now = Date()

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply { timeZone = tz }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = tz }
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault()).apply { timeZone = tz }

        val environment = JSONObject().apply {
            put("currentTime", timeFormat.format(now))
            put("currentDate", dateFormat.format(now))
            put("dayOfWeek", dayOfWeekFormat.format(now))
            put("timezone", tzId)
        }
        root.put("environment", environment)

        // 2. User Profile & Preferences
        val profileJson = JSONObject().apply {
            put("name", profile.name)
            put("username", profile.username)
            put("focusStatus", profile.focusStatus)
            put("dailyTaskGoal", profile.dailyTaskGoal)
            put("morningDigestTime", profile.morningDigestTime)
        }
        root.put("userProfile", profileJson)

        // 3. User Categories
        val categoriesArray = JSONArray()
        categories.forEach { cat ->
            val catJson = JSONObject().apply {
                put("name", cat.name)
                put("activeTaskCount", tasks.count { !it.isCompleted && it.category.equals(cat.name, ignoreCase = true) })
            }
            categoriesArray.put(catJson)
        }
        root.put("categories", categoriesArray)

        // 4. Pending Tasks
        val pendingTasks = tasks.filter { !it.isCompleted }
        val pendingArray = JSONArray()
        pendingTasks.forEach { task ->
            val taskJson = JSONObject().apply {
                put("taskId", task.id)
                put("title", task.title)
                put("category", task.category)
                put("scheduledDate", task.date)
                put("scheduledTime", task.time)
                put("timeInMinutes", task.getTimeInMinutes())
                put("priority", task.priority)
            }
            pendingArray.put(taskJson)
        }
        root.put("pendingTasks", pendingArray)

        // 5. Completed Tasks
        val completedTasks = tasks.filter { it.isCompleted }
        val completedArray = JSONArray()
        completedTasks.forEach { task ->
            val taskJson = JSONObject().apply {
                put("taskId", task.id)
                put("title", task.title)
                put("category", task.category)
                put("scheduledTime", task.time)
            }
            completedArray.put(taskJson)
        }
        root.put("completedTasksToday", completedArray)

        // 6. Summary Metrics
        val summary = JSONObject().apply {
            put("totalPendingCount", pendingTasks.size)
            put("totalCompletedCount", completedTasks.size)
            put("dailyGoal", profile.dailyTaskGoal)
            val progressPercent = if (profile.dailyTaskGoal > 0) {
                ((completedTasks.size.toFloat() / profile.dailyTaskGoal) * 100).toInt().coerceAtMost(100)
            } else 0
            put("goalProgressPercent", progressPercent)
        }
        root.put("metrics", summary)

        return root.toString(2)
    }
}
