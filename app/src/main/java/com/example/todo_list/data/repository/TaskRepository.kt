package com.example.todo_list.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.todo_list.data.local.AppDatabase
import com.example.todo_list.data.local.entity.TaskEntity
import com.example.todo_list.data.local.entity.toEntity
import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskListCategory
import com.example.todo_list.data.remote.SupabaseClient
import com.example.todo_list.security.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Single repository for managing tasks and categories in TaskFlow.
 * Supports multi-user isolation with per-user task queries and mutations.
 */
class TaskRepository(private val database: AppDatabase) {

    private val taskDao = database.taskDao()
    private val categoryDao = database.categoryDao()

    /**
     * Continuous reactive Flow of all tasks across the app (fallback / admin inspection).
     */
    val tasks: Flow<List<TaskItem>> = taskDao.getAllTasksFlow().map { entities ->
        entities.map { it.toModel() }
    }

    /**
     * Reactive Flow of tasks filtered specifically for the given user ID.
     */
    fun getTasksForUser(userId: String): Flow<List<TaskItem>> =
        taskDao.getTasksForUserFlow(userId).map { entities ->
            entities.map { it.toModel() }
        }

    /**
     * Continuous reactive Flow of all categories.
     */
    val categories: Flow<List<TaskListCategory>> = categoryDao.getAllCategoriesFlow().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun insertTask(task: TaskItem) = withContext(Dispatchers.IO) {
        val resolvedUserId = if (task.userId.isNotBlank()) {
            task.userId
        } else {
            AuthManager.currentUserId ?: ""
        }
        val entity = task.copy(userId = resolvedUserId).toEntity()
        taskDao.insertTask(entity)
        if (resolvedUserId.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                SupabaseClient.upsertTask(entity)
            }
        }
    }

    suspend fun updateTask(task: TaskItem) = withContext(Dispatchers.IO) {
        val resolvedUserId = if (task.userId.isNotBlank()) {
            task.userId
        } else {
            AuthManager.currentUserId ?: ""
        }
        val entity = task.copy(userId = resolvedUserId).toEntity()
        taskDao.updateTask(entity)
        if (resolvedUserId.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                SupabaseClient.upsertTask(entity)
            }
        }
    }

    suspend fun deleteTask(task: TaskItem) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task.toEntity())
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseClient.deleteTask(task.id)
        }
    }

    suspend fun deleteTaskById(taskId: String) = withContext(Dispatchers.IO) {
        taskDao.deleteTaskById(taskId)
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseClient.deleteTask(taskId)
        }
    }

    suspend fun toggleTaskComplete(task: TaskItem): TaskItem = withContext(Dispatchers.IO) {
        val updatedCompleted = !task.isCompleted
        taskDao.updateTaskCompletion(task.id, updatedCompleted)
        val updated = task.copy(isCompleted = updatedCompleted)
        val entity = updated.toEntity()
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseClient.upsertTask(entity)
        }
        updated
    }

    suspend fun updateTaskPriority(taskId: String, priority: String) = withContext(Dispatchers.IO) {
        taskDao.updateTaskPriority(taskId, priority)
        taskDao.getTaskById(taskId)?.let { entity ->
            CoroutineScope(Dispatchers.IO).launch {
                SupabaseClient.upsertTask(entity)
            }
        }
    }

    suspend fun updateTasksPriorities(priorities: Map<String, String>) = withContext(Dispatchers.IO) {
        priorities.forEach { (taskId, priority) ->
            taskDao.updateTaskPriority(taskId, priority)
        }
        CoroutineScope(Dispatchers.IO).launch {
            priorities.keys.forEach { taskId ->
                taskDao.getTaskById(taskId)?.let { SupabaseClient.upsertTask(it) }
            }
        }
    }

    suspend fun updateTaskSchedule(taskId: String, time: String, date: String, epochDay: Long) = withContext(Dispatchers.IO) {
        taskDao.updateTaskSchedule(taskId, time, date, epochDay)
        taskDao.getTaskById(taskId)?.let { entity ->
            CoroutineScope(Dispatchers.IO).launch {
                SupabaseClient.upsertTask(entity)
            }
        }
    }

    suspend fun clearCompletedTasks(userId: String = AuthManager.currentUserId ?: "") = withContext(Dispatchers.IO) {
        if (userId.isNotBlank()) {
            val completed = taskDao.getTasksForUserDirect(userId).filter { it.isCompleted }
            taskDao.deleteCompletedTasksForUser(userId)
            CoroutineScope(Dispatchers.IO).launch {
                completed.forEach { SupabaseClient.deleteTask(it.id) }
            }
        } else {
            taskDao.deleteCompletedTasks()
        }
    }

    suspend fun updateTaskQuadrant(taskId: String, quadrant: String) = withContext(Dispatchers.IO) {
        taskDao.updateTaskQuadrant(taskId, quadrant)
        taskDao.getTaskById(taskId)?.let { entity ->
            CoroutineScope(Dispatchers.IO).launch {
                SupabaseClient.upsertTask(entity)
            }
        }
    }

    suspend fun updateTaskPomodoro(taskId: String, completedSessions: Int, estimatedSessions: Int) = withContext(Dispatchers.IO) {
        taskDao.updateTaskPomodoroProgress(taskId, completedSessions, estimatedSessions)
        taskDao.getTaskById(taskId)?.let { entity ->
            CoroutineScope(Dispatchers.IO).launch {
                SupabaseClient.upsertTask(entity)
            }
        }
    }

    suspend fun clearAllTasks(userId: String = AuthManager.currentUserId ?: "") = withContext(Dispatchers.IO) {
        if (userId.isNotBlank()) {
            val all = taskDao.getTasksForUserDirect(userId)
            taskDao.deleteAllTasksForUser(userId)
            CoroutineScope(Dispatchers.IO).launch {
                all.forEach { SupabaseClient.deleteTask(it.id) }
            }
        } else {
            taskDao.deleteAllTasks()
        }
    }

    suspend fun insertCategory(category: TaskListCategory) = withContext(Dispatchers.IO) {
        val entity = category.toEntity()
        categoryDao.insertCategory(entity)
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseClient.upsertCategories(listOf(entity))
        }
    }

    suspend fun deleteCategory(category: TaskListCategory) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategoryById(category.id)
    }

    suspend fun deleteCategoryWithMigration(
        categoryToDelete: TaskListCategory,
        targetCategoryName: String,
        userId: String = AuthManager.currentUserId ?: ""
    ) = withContext(Dispatchers.IO) {
        database.withTransaction {
            if (userId.isNotBlank()) {
                taskDao.migrateCategoryForUser(userId, categoryToDelete.name, targetCategoryName)
            } else {
                taskDao.migrateCategory(categoryToDelete.name, targetCategoryName)
            }
            categoryDao.deleteCategoryById(categoryToDelete.id)
        }
        if (userId.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                val updatedTasks = taskDao.getTasksForUserDirect(userId)
                SupabaseClient.upsertTasks(updatedTasks)
            }
        }
    }

    suspend fun seedStarterTasksForUser(userId: String) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        if (taskDao.getTaskCountForUser(userId) == 0) {
            val todayEpoch = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
            val starterTasks = listOf(
                TaskEntity(UUID.randomUUID().toString(), "Review design specs", "Work", "Today", "09:30 AM", false, todayEpoch, userId, "HIGH", "DO_FIRST", 0, 1, 1000L),
                TaskEntity(UUID.randomUUID().toString(), "Grocery shopping", "Personal", "Today", "11:00 AM", true, todayEpoch, userId, "LOW", "DELEGATE", 0, 1, 2000L),
                TaskEntity(UUID.randomUUID().toString(), "Team sync at 2 PM", "Work", "Today", "02:00 PM", false, todayEpoch, userId, "HIGH", "DO_FIRST", 0, 1, 3000L),
                TaskEntity(UUID.randomUUID().toString(), "Health checkup", "Health", "Today", "03:30 PM", false, todayEpoch, userId, "MEDIUM", "SCHEDULE", 0, 1, 4000L),
                TaskEntity(UUID.randomUUID().toString(), "Study Compose layout", "Study", "Today", "06:45 PM", false, todayEpoch, userId, "MEDIUM", "SCHEDULE", 0, 1, 5000L)
            )
            taskDao.insertTasks(starterTasks)
            CoroutineScope(Dispatchers.IO).launch {
                SupabaseClient.upsertTasks(starterTasks)
            }
        }
    }

    /**
     * Synchronizes tasks between local Room SQLite and Supabase Cloud.
     * Restores missing tasks from cloud, pushes local changes.
     */
    suspend fun syncWithCloud(userId: String) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            val cloudTasks = SupabaseClient.fetchTasksForUser(userId)
            val localTasks = taskDao.getTasksForUserDirect(userId)
            val localTaskIds = localTasks.map { it.id }.toSet()

            // 1. Insert any cloud tasks that don't exist locally
            val missingLocally = cloudTasks.filter { it.id !in localTaskIds }
            if (missingLocally.isNotEmpty()) {
                taskDao.insertTasks(missingLocally)
            }

            // 2. Upload any local tasks that don't exist in cloud
            val cloudTaskIds = cloudTasks.map { it.id }.toSet()
            val missingInCloud = localTasks.filter { it.id !in cloudTaskIds }
            if (missingInCloud.isNotEmpty()) {
                SupabaseClient.upsertTasks(missingInCloud)
            }
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    suspend fun ensureInitialDataSeeded() = withContext(Dispatchers.IO) {
        AppDatabase.prepopulateDefaults(database)
    }

    companion object {
        @Volatile
        private var INSTANCE: TaskRepository? = null

        fun getInstance(context: Context): TaskRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TaskRepository(AppDatabase.getInstance(context.applicationContext)).also {
                    INSTANCE = it
                }
            }
        }
    }
}
