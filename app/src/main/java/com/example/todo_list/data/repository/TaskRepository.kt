package com.example.todo_list.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.todo_list.data.local.AppDatabase
import com.example.todo_list.data.local.entity.toEntity
import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskListCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Single repository for managing tasks and categories in TaskFlow.
 * Mediates between Room SQLite DAOs and UI/ViewModels with Coroutines and Flow.
 */
class TaskRepository(private val database: AppDatabase) {

    private val taskDao = database.taskDao()
    private val categoryDao = database.categoryDao()

    /**
     * Continuous reactive Flow of all tasks mapped to domain model.
     */
    val tasks: Flow<List<TaskItem>> = taskDao.getAllTasksFlow().map { entities ->
        entities.map { it.toModel() }
    }

    /**
     * Continuous reactive Flow of all categories mapped to domain model.
     */
    val categories: Flow<List<TaskListCategory>> = categoryDao.getAllCategoriesFlow().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun insertTask(task: TaskItem) = withContext(Dispatchers.IO) {
        taskDao.insertTask(task.toEntity())
    }

    suspend fun updateTask(task: TaskItem) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task.toEntity())
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

    suspend fun clearCompletedTasks() = withContext(Dispatchers.IO) {
        taskDao.deleteCompletedTasks()
    }

    suspend fun clearAllTasks() = withContext(Dispatchers.IO) {
        taskDao.deleteAllTasks()
    }

    suspend fun insertCategory(category: TaskListCategory) = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category.toEntity())
    }

    suspend fun deleteCategory(category: TaskListCategory) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategoryById(category.id)
    }

    suspend fun deleteCategoryWithMigration(categoryToDelete: TaskListCategory, targetCategoryName: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            taskDao.migrateCategory(categoryToDelete.name, targetCategoryName)
            categoryDao.deleteCategoryById(categoryToDelete.id)
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
