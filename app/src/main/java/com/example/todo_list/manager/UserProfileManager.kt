package com.example.todo_list.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.todo_list.data.local.AppDatabase
import com.example.todo_list.data.local.entity.UserEntity
import com.example.todo_list.model.AchievementBadge
import com.example.todo_list.model.AvatarPreset
import com.example.todo_list.model.UserProfile
import com.example.todo_list.security.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reactive Singleton Manager for persistent User Profile, Gamification XP,
 * Streaks, and Apple-style Achievements backed by Room SQLite 'users' table.
 */
object UserProfileManager {
    private const val PREFS_NAME = "taskflow_user_profile_prefs"

    private const val KEY_LAST_SYNC_TIME = "key_profile_last_sync"
    private const val KEY_UNLOCKED_BADGES = "key_profile_unlocked_badges"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    // Reactive Compose State
    var profile by mutableStateOf(UserProfile())
        private set

    var xpPoints by mutableIntStateOf(0)
        private set

    var currentStreak by mutableIntStateOf(0)
        private set

    var bestStreak by mutableIntStateOf(0)
        private set

    var lastSyncTimestamp by mutableStateOf("Just now")
        private set

    var achievements by mutableStateOf(AchievementBadge.DEFAULT_ACHIEVEMENTS)
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            lastSyncTimestamp = prefs?.getString(KEY_LAST_SYNC_TIME, "Just now") ?: "Just now"
        }
    }

    /**
     * Loads the profile and stats for the active user account from SQLite.
     */
    fun loadUserProfile(user: UserEntity?) {
        if (user == null) {
            profile = UserProfile()
            xpPoints = 0
            currentStreak = 0
            bestStreak = 0
            return
        }

        profile = UserProfile(
            name = user.name,
            username = user.username,
            email = user.email,
            phone = user.phone,
            bio = user.bio,
            avatarPresetId = user.avatarPresetId,
            customAvatarUri = user.customAvatarUri,
            focusStatus = user.focusStatus,
            userTier = user.userTier,
            memberSince = user.memberSince,
            dailyTaskGoal = user.dailyTaskGoal,
            morningDigestTime = user.morningDigestTime,
            autoCloudSync = user.autoCloudSync
        )
        xpPoints = user.xpPoints
        currentStreak = user.currentStreak
        bestStreak = user.bestStreak
    }

    private fun syncUserToDatabase() {
        val user = AuthManager.currentUser ?: return
        val ctx = appContext ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getInstance(ctx).userDao()
            dao.updateUserProfile(
                userId = user.id,
                name = profile.name,
                username = profile.username,
                email = profile.email,
                bio = profile.bio,
                avatarPresetId = profile.avatarPresetId,
                customAvatarUri = profile.customAvatarUri,
                focusStatus = profile.focusStatus,
                dailyGoal = profile.dailyTaskGoal
            )
            dao.updateUserStats(
                userId = user.id,
                xp = xpPoints,
                streak = currentStreak,
                bestStreak = bestStreak
            )
        }
    }

    fun updateProfile(
        name: String = profile.name,
        username: String = profile.username,
        email: String = profile.email,
        phone: String = profile.phone,
        bio: String = profile.bio
    ) {
        val updated = profile.copy(
            name = name,
            username = username,
            email = email,
            phone = phone,
            bio = bio
        )
        profile = updated
        syncUserToDatabase()
    }

    fun setAvatarPreset(presetId: Int) {
        val updated = profile.copy(avatarPresetId = presetId, customAvatarUri = null)
        profile = updated
        syncUserToDatabase()
    }

    fun setCustomAvatarUri(uri: String?) {
        val updated = profile.copy(customAvatarUri = uri)
        profile = updated
        syncUserToDatabase()
    }

    fun setFocusStatus(status: String) {
        val updated = profile.copy(focusStatus = status)
        profile = updated
        syncUserToDatabase()
    }

    fun setDailyGoal(goal: Int) {
        val updated = profile.copy(dailyTaskGoal = goal)
        profile = updated
        syncUserToDatabase()
    }

    fun setMorningDigestTime(time: String) {
        val updated = profile.copy(morningDigestTime = time)
        profile = updated
        syncUserToDatabase()
    }

    fun addXp(amount: Int) {
        val newXp = xpPoints + amount
        xpPoints = newXp
        val user = AuthManager.currentUser
        val ctx = appContext
        if (user != null && ctx != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val dao = AppDatabase.getInstance(ctx).userDao()
                dao.updateUserStats(user.id, newXp, currentStreak, bestStreak)
            }
        }
    }

    fun getAvatarUrl(): String {
        return profile.customAvatarUri ?: AvatarPreset.getById(profile.avatarPresetId).avatarUrl
    }

    fun getLevel(): Int = (xpPoints / 500) + 1

    fun getLevelCurrentXp(): Int = xpPoints % 500

    fun getLevelMaxXp(): Int = 500

    fun getLevelProgressRatio(): Float = getLevelCurrentXp().toFloat() / getLevelMaxXp()

    fun getLevelTitle(): String {
        return when (getLevel()) {
            1 -> "Focus Initiate"
            2 -> "Task Apprentice"
            3 -> "Workflow Artisan"
            4 -> "Productivity Pro"
            5 -> "Time Master"
            6 -> "Flow Specialist"
            7 -> "Execution Leader"
            8 -> "Grandmaster"
            else -> "Legendary Architect"
        }
    }

    fun unlockAchievement(badgeId: String) {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val dateStr = sdf.format(Date())

        achievements = achievements.map { badge ->
            if (badge.id == badgeId && !badge.isUnlocked) {
                addXp(badge.xpReward)
                badge.copy(isUnlocked = true, unlockedDate = dateStr, currentProgress = badge.maxProgress)
            } else {
                badge
            }
        }

        val unlockedIds = achievements.filter { it.isUnlocked }.map { it.id }.toSet()
        prefs?.edit()?.putStringSet(KEY_UNLOCKED_BADGES, unlockedIds)?.apply()
    }

    fun triggerCloudSync(onComplete: () -> Unit = {}) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = "Today at ${sdf.format(Date())}"
        lastSyncTimestamp = timeStr
        prefs?.edit()?.putString(KEY_LAST_SYNC_TIME, timeStr)?.apply()
        onComplete()
    }

    fun resetProfileToDefaults() {
        profile = UserProfile()
        xpPoints = 0
        currentStreak = 0
        bestStreak = 0
        lastSyncTimestamp = "Just now"
        achievements = AchievementBadge.DEFAULT_ACHIEVEMENTS
    }
}
