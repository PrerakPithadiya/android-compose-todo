package com.example.todo_list.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.todo_list.data.local.dao.CategoryDao
import com.example.todo_list.data.local.dao.TaskDao
import com.example.todo_list.data.local.entity.CategoryEntity
import com.example.todo_list.data.local.entity.TaskEntity
import com.example.todo_list.model.TaskListCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android Jetpack Room Database for TaskFlow.
 * Serves as the single local source of truth for tasks and categories.
 */
@Database(
    entities = [TaskEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private const val DATABASE_NAME = "taskflow_database.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default categories and sample tasks on background thread
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            prepopulateDefaults(database)
                        }
                    }
                })
                .fallbackToDestructiveMigration(true)
                .build()
        }

        suspend fun prepopulateDefaults(database: AppDatabase) {
            val categoryDao = database.categoryDao()
            val taskDao = database.taskDao()

            if (categoryDao.getCategoryCount() == 0) {
                val defaultCategories = TaskListCategory.DEFAULT_CATEGORIES.mapIndexed { index, cat ->
                    CategoryEntity.fromModel(cat, displayOrder = index)
                }
                categoryDao.insertCategories(defaultCategories)
            }

            if (taskDao.getTaskCount() == 0) {
                val todayEpoch = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
                val starterTasks = listOf(
                    TaskEntity("1", "Review design specs", "Work", "Today", "09:30 AM", false, todayEpoch, 1000L),
                    TaskEntity("3", "Grocery shopping", "Personal", "Today", "11:00 AM", true, todayEpoch, 2000L),
                    TaskEntity("2", "Team sync at 2 PM", "Work", "Today", "02:00 PM", false, todayEpoch, 3000L),
                    TaskEntity("7", "Health checkup", "Health", "Today", "03:30 PM", false, todayEpoch, 4000L),
                    TaskEntity("4", "Update project timeline", "Work", "Today", "04:30 PM", false, todayEpoch, 5000L),
                    TaskEntity("5", "Prepare quarterly report", "Work", "Today", "06:00 PM", false, todayEpoch, 6000L),
                    TaskEntity("8", "Study Compose layout", "Study", "Today", "06:45 PM", false, todayEpoch, 7000L),
                    TaskEntity("6", "Evening gym session", "Health", "Today", "07:30 PM", false, todayEpoch, 8000L)
                )
                taskDao.insertTasks(starterTasks)
            }
        }
    }
}
