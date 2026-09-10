package com.example.todo_list.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.todo_list.data.local.AppDatabase
import com.example.todo_list.data.local.entity.TaskEntity
import com.example.todo_list.data.local.entity.toEntity
import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskListCategory
import com.example.todo_list.security.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        taskDao.insertTask(task.copy(userId = resolvedUserId).toEntity())
    }

    suspend fun updateTask(task: TaskItem) = withContext(Dispatchers.IO) {
        val resolvedUserId = if (task.userId.isNotBlank()) {
            task.userId
        } else {
            AuthManager.currentUserId ?: ""
        }
        taskDao.updateTask(task.copy(userId = resolvedUserId).toEntity())
    }

    suspend fun deleteTask(task: TaskItem) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task.toEntity())
    }

    suspend fun deleteTaskById(taskId: String) = withContext(Dispatchers.IO) {
        taskDao.deleteTaskById(taskId)
    }

    suspend fun toggleTaskComplete(task: TaskItem): TaskItem = withContext(Dispatchers.IO) {
        val updatedCompleted = !task.isCompleted
        taskDao.updateTaskCompletion(task.id, updatedCompleted)
        task.copy(isCompleted = updatedCompleted)
    }

    suspend fun clearCompletedTasks(userId: String = AuthManager.currentUserId ?: "") = withContext(Dispatchers.IO) {
        if (userId.isNotBlank()) {
            taskDao.deleteCompletedTasksForUser(userId)
        } else {
            taskDao.deleteCompletedTasks()
        }
    }

    suspend fun clearAllTasks(userId: String = AuthManager.currentUserId ?: "") = withContext(Dispatchers.IO) {
        if (userId.isNotBlank()) {
            taskDao.deleteAllTasksForUser(userId)
        } else {
            taskDao.deleteAllTasks()
        }
    }

    suspend fun insertCategory(category: TaskListCategory) = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category.toEntity())
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
    }

    suspend fun seedStarterTasksForUser(userId: String) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        if (taskDao.getTaskCountForUser(userId) == 0) {
            val todayEpoch = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
            val starterTasks = listOf(
                TaskEntity(UUID.randomUUID().toString(), "Review design specs", "Work", "Today", "09:30 AM", false, todayEpoch, userId, 1000L),
                TaskEntity(UUID.randomUUID().toString(), "Grocery shopping", "Personal", "Today", "11:00 AM", true, todayEpoch, userId, 2000L),
                TaskEntity(UUID.randomUUID().toString(), "Team sync at 2 PM", "Work", "Today", "02:00 PM", false, todayEpoch, userId, 3000L),
                TaskEntity(UUID.randomUUID().toString(), "Health checkup", "Health", "Today", "03:30 PM", false, todayEpoch, userId, 4000L),
                TaskEntity(UUID.randomUUID().toString(), "Study Compose layout", "Study", "Today", "06:45 PM", false, todayEpoch, userId, 5000L)
            )
            taskDao.insertTasks(starterTasks)
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
