package com.example.todo_list.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todo_list.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Users in SQLite.
 * Manages accounts, credentials, screen locks, and profile states.
 */
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone OR REPLACE(phone, ' ', '') = REPLACE(:phone, ' ', '') LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("""
        SELECT * FROM users 
        WHERE LOWER(username) = LOWER(:identifier)
           OR LOWER(username) = LOWER('@' || :identifier)
           OR phone = :identifier
           OR REPLACE(phone, ' ', '') = REPLACE(:identifier, ' ', '')
        LIMIT 1
    """)
    suspend fun getUserByIdentifier(identifier: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("""
        UPDATE users 
        SET lockType = :lockType,
            lockHash = :lockHash,
            lockSalt = :lockSalt,
            isLockEnabled = :isLockEnabled,
            isBiometricEnabled = :isBiometricEnabled,
            lockTimeoutMs = :lockTimeoutMs
        WHERE id = :userId
    """)
    suspend fun updateUserLock(
        userId: String,
        lockType: String,
        lockHash: String?,
        lockSalt: String?,
        isLockEnabled: Boolean,
        isBiometricEnabled: Boolean,
        lockTimeoutMs: Long
    )

    @Query("""
        UPDATE users 
        SET name = :name,
            username = :username,
            email = :email,
            bio = :bio,
            avatarPresetId = :avatarPresetId,
            customAvatarUri = :customAvatarUri,
            focusStatus = :focusStatus,
            dailyTaskGoal = :dailyGoal
        WHERE id = :userId
    """)
    suspend fun updateUserProfile(
        userId: String,
        name: String,
        username: String,
        email: String,
        bio: String,
        avatarPresetId: Int,
        customAvatarUri: String?,
        focusStatus: String,
        dailyGoal: Int
    )

    @Query("""
        UPDATE users 
        SET xpPoints = :xp,
            currentStreak = :streak,
            bestStreak = :bestStreak
        WHERE id = :userId
    """)
    suspend fun updateUserStats(
        userId: String,
        xp: Int,
        streak: Int,
        bestStreak: Int
    )

    @Query("UPDATE users SET passwordHash = :hash, passwordSalt = :salt WHERE id = :userId")
    suspend fun updateUserPassword(
        userId: String,
        hash: String,
        salt: String
    )

    @Query("UPDATE users SET username = :username WHERE id = :userId")
    suspend fun updateUserUsername(
        userId: String,
        username: String
    )

    @Query("UPDATE users SET lastLoginAt = :timestamp WHERE id = :userId")
    suspend fun updateLastLogin(
        userId: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)
}
