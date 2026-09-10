package com.example.todo_list.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Room Entity representing a registered user in SQLite.
 * Stores user credentials, salted password hash, screen lock preferences,
 * and profile details for multi-user support.
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["phone"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val username: String, // e.g. "@prerak"
    val phone: String, // e.g. "+91 9876543210"
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    // Screen Lock settings per user
    val lockType: String = "pin_4", // "none", "pin_4", "pin_6", "alphanumeric", "pattern"
    val lockHash: String? = null,
    val lockSalt: String? = null,
    val isLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val lockTimeoutMs: Long = 0L,
    // Profile information
    val avatarPresetId: Int = 0,
    val customAvatarUri: String? = null,
    val bio: String = "Productivity Architect • Building minimal, powerful tools ⚡",
    val focusStatus: String = "🎯 Deep Work",
    val userTier: String = "TaskFlow Pro",
    val memberSince: String = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date()),
    val dailyTaskGoal: Int = 5,
    val morningDigestTime: String = "08:00 AM",
    val autoCloudSync: Boolean = true,
    // Gamification stats
    val xpPoints: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
