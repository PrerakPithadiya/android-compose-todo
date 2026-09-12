package com.example.todo_list.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todo_list.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Tasks.
 * Supports multi-user task queries and management.
 */
@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY createdAt ASC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY createdAt ASC")
    fun getTasksForUserFlow(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND category = :category ORDER BY createdAt ASC")
    fun getTasksForUserByCategoryFlow(userId: String, category: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks()

    @Query("DELETE FROM tasks WHERE userId = :userId AND isCompleted = 1")
    suspend fun deleteCompletedTasksForUser(userId: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM tasks WHERE userId = :userId")
    suspend fun deleteAllTasksForUser(userId: String)

    @Query("UPDATE tasks SET category = :targetCategory WHERE category = :oldCategory")
    suspend fun migrateCategory(oldCategory: String, targetCategory: String)

    @Query("UPDATE tasks SET category = :targetCategory WHERE userId = :userId AND category = :oldCategory")
    suspend fun migrateCategoryForUser(userId: String, oldCategory: String, targetCategory: String)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: String, isCompleted: Boolean)

    @Query("UPDATE tasks SET priority = :priority WHERE id = :taskId")
    suspend fun updateTaskPriority(taskId: String, priority: String)

    @Query("UPDATE tasks SET time = :time, date = :date, epochDay = :epochDay WHERE id = :taskId")
    suspend fun updateTaskSchedule(taskId: String, time: String, date: String, epochDay: Long)

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId")
    suspend fun getTaskCountForUser(userId: String): Int
}
