package com.example.todo_list.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.todo_list.data.local.dao.CategoryDao
import com.example.todo_list.data.local.dao.TaskDao
import com.example.todo_list.data.local.dao.UserDao
import com.example.todo_list.data.local.entity.CategoryEntity
import com.example.todo_list.data.local.entity.TaskEntity
import com.example.todo_list.data.local.entity.UserEntity
import com.example.todo_list.model.TaskListCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android Jetpack Room Database for TaskFlow.
 * Serves as the single local source of truth for users, tasks, and categories.
 */
@Database(
    entities = [TaskEntity::class, CategoryEntity::class, UserEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userDao(): UserDao

    companion object {
        private const val DATABASE_NAME = "taskflow_database.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN priority TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

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
                .addMigrations(MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default categories on background thread
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

            if (categoryDao.getCategoryCount() == 0) {
                val defaultCategories = TaskListCategory.DEFAULT_CATEGORIES.mapIndexed { index, cat ->
                    CategoryEntity.fromModel(cat, displayOrder = index)
                }
                categoryDao.insertCategories(defaultCategories)
            }
        }
    }
}
