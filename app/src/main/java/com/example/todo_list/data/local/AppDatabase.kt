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
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userDao(): UserDao

    companion object {
        private const val DATABASE_NAME = "taskflow_database.db"

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `eisenhowerQuadrant` TEXT NOT NULL DEFAULT 'DO_FIRST'")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `pomodoroSessionsCompleted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `pomodoroEstimatedSessions` INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `users` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `passwordSalt` TEXT NOT NULL,
                        `lockType` TEXT NOT NULL,
                        `lockHash` TEXT,
                        `lockSalt` TEXT,
                        `isLockEnabled` INTEGER NOT NULL,
                        `isBiometricEnabled` INTEGER NOT NULL,
                        `lockTimeoutMs` INTEGER NOT NULL,
                        `avatarPresetId` INTEGER NOT NULL,
                        `customAvatarUri` TEXT,
                        `bio` TEXT NOT NULL,
                        `focusStatus` TEXT NOT NULL,
                        `userTier` TEXT NOT NULL,
                        `memberSince` TEXT NOT NULL,
                        `dailyTaskGoal` INTEGER NOT NULL,
                        `morningDigestTime` TEXT NOT NULL,
                        `autoCloudSync` INTEGER NOT NULL,
                        `xpPoints` INTEGER NOT NULL,
                        `currentStreak` INTEGER NOT NULL,
                        `bestStreak` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `lastLoginAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_phone` ON `users` (`phone`)")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `userId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_userId` ON `tasks` (`userId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `priority` TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `users` ADD COLUMN `isFaceAuthEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `users` ADD COLUMN `isFingerprintAuthEnabled` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `users` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `passwordSalt` TEXT NOT NULL,
                        `lockType` TEXT NOT NULL,
                        `lockHash` TEXT,
                        `lockSalt` TEXT,
                        `isLockEnabled` INTEGER NOT NULL,
                        `isBiometricEnabled` INTEGER NOT NULL,
                        `lockTimeoutMs` INTEGER NOT NULL,
                        `avatarPresetId` INTEGER NOT NULL,
                        `customAvatarUri` TEXT,
                        `bio` TEXT NOT NULL,
                        `focusStatus` TEXT NOT NULL,
                        `userTier` TEXT NOT NULL,
                        `memberSince` TEXT NOT NULL,
                        `dailyTaskGoal` INTEGER NOT NULL,
                        `morningDigestTime` TEXT NOT NULL,
                        `autoCloudSync` INTEGER NOT NULL,
                        `xpPoints` INTEGER NOT NULL,
                        `currentStreak` INTEGER NOT NULL,
                        `bestStreak` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `lastLoginAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_phone` ON `users` (`phone`)")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `userId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `priority` TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_userId` ON `tasks` (`userId`)")
            }
        }

        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `users` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `passwordSalt` TEXT NOT NULL,
                        `lockType` TEXT NOT NULL,
                        `lockHash` TEXT,
                        `lockSalt` TEXT,
                        `isLockEnabled` INTEGER NOT NULL,
                        `isBiometricEnabled` INTEGER NOT NULL,
                        `isFaceAuthEnabled` INTEGER NOT NULL,
                        `isFingerprintAuthEnabled` INTEGER NOT NULL,
                        `lockTimeoutMs` INTEGER NOT NULL,
                        `avatarPresetId` INTEGER NOT NULL,
                        `customAvatarUri` TEXT,
                        `bio` TEXT NOT NULL,
                        `focusStatus` TEXT NOT NULL,
                        `userTier` TEXT NOT NULL,
                        `memberSince` TEXT NOT NULL,
                        `dailyTaskGoal` INTEGER NOT NULL,
                        `morningDigestTime` TEXT NOT NULL,
                        `autoCloudSync` INTEGER NOT NULL,
                        `xpPoints` INTEGER NOT NULL,
                        `currentStreak` INTEGER NOT NULL,
                        `bestStreak` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `lastLoginAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_phone` ON `users` (`phone`)")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `userId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `priority` TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_userId` ON `tasks` (`userId`)")
            }
        }

        val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `priority` TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE `users` ADD COLUMN `isFaceAuthEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `users` ADD COLUMN `isFingerprintAuthEnabled` INTEGER NOT NULL DEFAULT 0")
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_1_3, MIGRATION_1_4, MIGRATION_2_4, MIGRATION_4_5)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default categories directly into SQLite without recursive Room calls
                        TaskListCategory.DEFAULT_CATEGORIES.forEachIndexed { index, cat ->
                            db.execSQL(
                                "INSERT OR IGNORE INTO categories (id, name, colorHex, iconName, isSystemDefault, displayOrder) VALUES (?, ?, ?, ?, ?, ?)",
                                arrayOf(cat.id, cat.name, cat.colorHex, cat.iconName, if (cat.isSystemDefault) 1 else 0, index)
                            )
                        }
                    }
                })
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
