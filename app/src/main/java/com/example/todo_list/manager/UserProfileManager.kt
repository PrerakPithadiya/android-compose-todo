package com.example.todo_list.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.todo_list.model.AchievementBadge
import com.example.todo_list.model.AvatarPreset
import com.example.todo_list.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reactive Singleton Manager for persistent User Profile, Gamification XP,
 * Streaks, and Apple-style Achievements.
 */
object UserProfileManager {
    private const val PREFS_NAME = "taskflow_user_profile_prefs"

    private const val KEY_NAME = "key_profile_name"
    private const val KEY_USERNAME = "key_profile_username"
    private const val KEY_EMAIL = "key_profile_email"
    private const val KEY_PHONE = "key_profile_phone"
    private const val KEY_BIO = "key_profile_bio"
    private const val KEY_AVATAR_PRESET_ID = "key_profile_avatar_preset_id"
    private const val KEY_CUSTOM_AVATAR_URI = "key_profile_custom_avatar_uri"
    private const val KEY_FOCUS_STATUS = "key_profile_focus_status"
    private const val KEY_DAILY_GOAL = "key_profile_daily_goal"
    private const val KEY_MORNING_DIGEST = "key_profile_morning_digest"
    private const val KEY_XP_POINTS = "key_profile_xp_points"
    private const val KEY_CURRENT_STREAK = "key_profile_current_streak"
    private const val KEY_BEST_STREAK = "key_profile_best_streak"
    private const val KEY_LAST_SYNC_TIME = "key_profile_last_sync"
    private const val KEY_UNLOCKED_BADGES = "key_profile_unlocked_badges"

    private var prefs: SharedPreferences? = null

    // Reactive Compose State
    var profile by mutableStateOf(UserProfile())
        private set

    var xpPoints by mutableIntStateOf(3850)
        private set

    var currentStreak by mutableIntStateOf(14)
        private set

    var bestStreak by mutableIntStateOf(28)
        private set

    var lastSyncTimestamp by mutableStateOf("Just now")
        private set

    var achievements by mutableStateOf(AchievementBadge.DEFAULT_ACHIEVEMENTS)
        private set

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            reloadState()
        }
    }

    private fun reloadState() {
        prefs?.let { p ->
            val name = p.getString(KEY_NAME, "Prerak Pithadiya") ?: "Prerak Pithadiya"
            val username = p.getString(KEY_USERNAME, "@prerak") ?: "@prerak"
            val email = p.getString(KEY_EMAIL, "prerak@taskflow.app") ?: "prerak@taskflow.app"
            val phone = p.getString(KEY_PHONE, "+1 (555) 382-9012") ?: "+1 (555) 382-9012"
            val bio = p.getString(
                KEY_BIO,
                "Productivity Architect & Android Developer • Building minimal, powerful tools ⚡"
            ) ?: "Productivity Architect & Android Developer • Building minimal, powerful tools ⚡"
            val avatarPresetId = p.getInt(KEY_AVATAR_PRESET_ID, 0)
            val customAvatarUri = p.getString(KEY_CUSTOM_AVATAR_URI, null)
            val focusStatus = p.getString(KEY_FOCUS_STATUS, "🎯 Deep Work") ?: "🎯 Deep Work"
            val dailyGoal = p.getInt(KEY_DAILY_GOAL, 5)
            val morningDigest = p.getString(KEY_MORNING_DIGEST, "08:00 AM") ?: "08:00 AM"

            profile = UserProfile(
                name = name,
                username = username,
                email = email,
                phone = phone,
                bio = bio,
                avatarPresetId = avatarPresetId,
                customAvatarUri = customAvatarUri,
                focusStatus = focusStatus,
                dailyTaskGoal = dailyGoal,
                morningDigestTime = morningDigest
            )

            xpPoints = p.getInt(KEY_XP_POINTS, 3850)
            currentStreak = p.getInt(KEY_CURRENT_STREAK, 14)
            bestStreak = p.getInt(KEY_BEST_STREAK, 28)
            lastSyncTimestamp = p.getString(KEY_LAST_SYNC_TIME, "Just now") ?: "Just now"

            // Reload unlocked badges
            val unlockedSet = p.getStringSet(KEY_UNLOCKED_BADGES, null)
            if (unlockedSet != null) {
                achievements = achievements.map { badge ->
                    if (unlockedSet.contains(badge.id)) {
                        badge.copy(isUnlocked = true)
                    } else {
                        badge
                    }
                }
            }
        }
    }

    fun updateProfile(
        name: String,
        username: String,
        email: String,
        phone: String,
        bio: String
    ) {
        val updated = profile.copy(
            name = name.trim(),
            username = if (username.startsWith("@")) username.trim() else "@${username.trim()}",
            email = email.trim(),
            phone = phone.trim(),
            bio = bio.trim()
        )
        profile = updated
        prefs?.edit()
            ?.putString(KEY_NAME, updated.name)
            ?.putString(KEY_USERNAME, updated.username)
            ?.putString(KEY_EMAIL, updated.email)
            ?.putString(KEY_PHONE, updated.phone)
            ?.putString(KEY_BIO, updated.bio)
            ?.apply()
    }

    fun setAvatarPreset(presetId: Int) {
        val updated = profile.copy(avatarPresetId = presetId, customAvatarUri = null)
        profile = updated
        prefs?.edit()
            ?.putInt(KEY_AVATAR_PRESET_ID, presetId)
            ?.remove(KEY_CUSTOM_AVATAR_URI)
            ?.apply()
    }

    fun setCustomAvatarUri(uri: String?) {
        val updated = profile.copy(customAvatarUri = uri)
        profile = updated
        if (uri != null) {
            prefs?.edit()?.putString(KEY_CUSTOM_AVATAR_URI, uri)?.apply()
        } else {
            prefs?.edit()?.remove(KEY_CUSTOM_AVATAR_URI)?.apply()
        }
    }

    fun setFocusStatus(status: String) {
        val updated = profile.copy(focusStatus = status)
        profile = updated
        prefs?.edit()?.putString(KEY_FOCUS_STATUS, status)?.apply()
    }

    fun setDailyGoal(goal: Int) {
        val updated = profile.copy(dailyTaskGoal = goal)
        profile = updated
        prefs?.edit()?.putInt(KEY_DAILY_GOAL, goal)?.apply()
    }

    fun setMorningDigestTime(time: String) {
        val updated = profile.copy(morningDigestTime = time)
        profile = updated
        prefs?.edit()?.putString(KEY_MORNING_DIGEST, time)?.apply()
    }

    fun addXp(amount: Int) {
        val newXp = xpPoints + amount
        xpPoints = newXp
        prefs?.edit()?.putInt(KEY_XP_POINTS, newXp)?.apply()
    }

    fun getAvatarUrl(): String {
        return profile.customAvatarUri ?: AvatarPreset.getById(profile.avatarPresetId).avatarUrl
    }

    /**
     * Compute current level based on XP (500 XP per level).
     */
    fun getLevel(): Int = (xpPoints / 500) + 1

    /**
     * Current progress towards the next level [0..499].
     */
    fun getLevelCurrentXp(): Int = xpPoints % 500

    /**
     * Total XP needed per level.
     */
    fun getLevelMaxXp(): Int = 500

    /**
     * Level progress ratio [0f..1f].
     */
    fun getLevelProgressRatio(): Float = getLevelCurrentXp().toFloat() / getLevelMaxXp()

    /**
     * Level title string based on user mastery.
     */
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
        prefs?.edit()?.clear()?.apply()
        profile = UserProfile()
        xpPoints = 3850
        currentStreak = 14
        bestStreak = 28
        lastSyncTimestamp = "Just now"
        achievements = AchievementBadge.DEFAULT_ACHIEVEMENTS
    }
}
