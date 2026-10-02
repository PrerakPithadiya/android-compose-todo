package com.example.todo_list.data.remote

import android.util.Log
import com.example.todo_list.data.local.entity.CategoryEntity
import com.example.todo_list.data.local.entity.TaskEntity
import com.example.todo_list.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Native Android HTTP Client for Supabase Cloud Database (PostgreSQL + PostgREST).
 * Powers cloud user persistence, account recovery across app uninstalls,
 * multi-device synchronization, and offline-first Room database sync.
 */
object SupabaseClient {
    private const val TAG = "SupabaseClient"

    private const val SUPABASE_URL = "https://hjumhnpdqrgnhskhrlcx.supabase.co"
    private const val REST_BASE = "$SUPABASE_URL/rest/v1"
    private const val ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhqdW1obnBkcXJnbmhza2hybGN4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA5NjkxNTMsImV4cCI6MjEwNjU0NTE1M30.YYcraDlnrSSlM4QvSEoSD-TizSBm0CmnWPSuV9Z5Zjg"

    private const val CONNECT_TIMEOUT_MS = 10000
    private const val READ_TIMEOUT_MS = 15000

    // ==========================================
    // USER AUTHENTICATION & PROFILE METHODS
    // ==========================================

    /**
     * Upserts a user record into the cloud database (creates or merges by primary key).
     */
    suspend fun upsertUser(user: UserEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("id", user.id)
                put("name", user.name)
                put("username", user.username)
                put("phone", user.phone)
                put("email", user.email)
                put("password_hash", user.passwordHash)
                put("password_salt", user.passwordSalt)
                put("lock_type", user.lockType)
                if (user.lockHash != null) put("lock_hash", user.lockHash)
                if (user.lockSalt != null) put("lock_salt", user.lockSalt)
                put("is_lock_enabled", user.isLockEnabled)
                put("is_biometric_enabled", user.isBiometricEnabled)
                put("is_face_auth_enabled", user.isFaceAuthEnabled)
                put("is_fingerprint_auth_enabled", user.isFingerprintAuthEnabled)
                put("lock_timeout_ms", user.lockTimeoutMs)
                put("avatar_preset_id", user.avatarPresetId)
                if (user.customAvatarUri != null) put("custom_avatar_uri", user.customAvatarUri)
                put("bio", user.bio)
                put("focus_status", user.focusStatus)
                put("user_tier", user.userTier)
                put("member_since", user.memberSince)
                put("daily_task_goal", user.dailyTaskGoal)
                put("morning_digest_time", user.morningDigestTime)
                put("auto_cloud_sync", user.autoCloudSync)
                put("xp_points", user.xpPoints)
                put("current_streak", user.currentStreak)
                put("best_streak", user.bestStreak)
                put("created_at", user.createdAt)
                put("last_login_at", user.lastLoginAt)
            }

            val headers = mapOf(
                "Prefer" to "resolution=merge-duplicates,return=representation"
            )
            val response = executeRequest("POST", "$REST_BASE/users", json.toString(), headers)
            response.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error upserting user to Supabase: ${e.message}", e)
            false
        }
    }

    /**
     * Looks up a user in the cloud database by username (with or without '@') or phone number.
     * Crucial for authenticating and restoring an account after the app was uninstalled!
     */
    suspend fun getUserByIdentifier(identifier: String): UserEntity? = withContext(Dispatchers.IO) {
        try {
            val cleanId = identifier.trim()
            val cleanUsername = if (cleanId.startsWith("@")) cleanId else "@$cleanId"
            val digitsOnly = cleanId.filter { it.isDigit() }
            val phoneWildcard = if (digitsOnly.length >= 7) digitsOnly.takeLast(10) else cleanId

            // Build PostgREST compound OR query:
            // or=(username.ilike.@id,username.ilike.id,phone.eq.id,phone.ilike.*phoneWildcard)
            val filters = mutableListOf(
                "username.ilike.${cleanUsername}",
                "username.ilike.${cleanId}",
                "phone.eq.${cleanId}"
            )
            if (phoneWildcard.isNotBlank()) {
                filters.add("phone.ilike.*$phoneWildcard")
            }
            val orQuery = URLEncoder.encode("(${filters.joinToString(",")})", "UTF-8")
            val url = "$REST_BASE/users?or=$orQuery&limit=1"

            val response = executeRequest("GET", url, null)
            if (response.isSuccess && response.body != null) {
                val array = JSONArray(response.body)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    return@withContext parseUserFromJson(obj)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up user by identifier '$identifier': ${e.message}", e)
            null
        }
    }

    /**
     * Looks up a user by their unique primary key ID.
     */
    suspend fun getUserById(userId: String): UserEntity? = withContext(Dispatchers.IO) {
        try {
            val encodedId = URLEncoder.encode(userId, "UTF-8")
            val url = "$REST_BASE/users?id=eq.$encodedId&limit=1"
            val response = executeRequest("GET", url, null)
            if (response.isSuccess && response.body != null) {
                val array = JSONArray(response.body)
                if (array.length() > 0) {
                    return@withContext parseUserFromJson(array.getJSONObject(0))
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by ID '$userId': ${e.message}", e)
            null
        }
    }

    suspend fun checkUsernameExists(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val clean = if (username.startsWith("@")) username.trim() else "@${username.trim()}"
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val url = "$REST_BASE/users?username=ilike.$encoded&limit=1"
            val response = executeRequest("GET", url, null)
            if (response.isSuccess && response.body != null) {
                val array = JSONArray(response.body)
                return@withContext array.length() > 0
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkPhoneExists(phone: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val digits = phone.filter { it.isDigit() }
            val match = if (digits.length >= 7) digits.takeLast(10) else phone.trim()
            val url = "$REST_BASE/users?phone=ilike.*$match&limit=1"
            val response = executeRequest("GET", url, null)
            if (response.isSuccess && response.body != null) {
                val array = JSONArray(response.body)
                return@withContext array.length() > 0
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateUserPassword(userId: String, passwordHash: String, passwordSalt: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("password_hash", passwordHash)
                    put("password_salt", passwordSalt)
                }
                val encodedId = URLEncoder.encode(userId, "UTF-8")
                val response = executeRequest("PATCH", "$REST_BASE/users?id=eq.$encodedId", json.toString())
                response.isSuccess
            } catch (e: Exception) {
                Log.e(TAG, "Error updating password on Supabase: ${e.message}", e)
                false
            }
        }

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
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("name", name)
                put("username", username)
                put("email", email)
                put("bio", bio)
                put("avatar_preset_id", avatarPresetId)
                if (customAvatarUri != null) {
                    put("custom_avatar_uri", customAvatarUri)
                } else {
                    put("custom_avatar_uri", JSONObject.NULL)
                }
                put("focus_status", focusStatus)
                put("daily_task_goal", dailyGoal)
            }
            val encodedId = URLEncoder.encode(userId, "UTF-8")
            val response = executeRequest("PATCH", "$REST_BASE/users?id=eq.$encodedId", json.toString())
            response.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile on Supabase: ${e.message}", e)
            false
        }
    }

    suspend fun updateUserLock(
        userId: String,
        lockType: String,
        lockHash: String?,
        lockSalt: String?,
        isLockEnabled: Boolean,
        isBiometricEnabled: Boolean,
        lockTimeoutMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("lock_type", lockType)
                if (lockHash != null) put("lock_hash", lockHash)
                if (lockSalt != null) put("lock_salt", lockSalt)
                put("is_lock_enabled", isLockEnabled)
                put("is_biometric_enabled", isBiometricEnabled)
                put("lock_timeout_ms", lockTimeoutMs)
            }
            val encodedId = URLEncoder.encode(userId, "UTF-8")
            val response = executeRequest("PATCH", "$REST_BASE/users?id=eq.$encodedId", json.toString())
            response.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error updating lock on Supabase: ${e.message}", e)
            false
        }
    }

    // ==========================================
    // TASK SYNCHRONIZATION METHODS
    // ==========================================

    /**
     * Upserts a single task in Supabase Cloud.
     */
    suspend fun upsertTask(task: TaskEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = formatTaskToJson(task)
            val headers = mapOf("Prefer" to "resolution=merge-duplicates")
            val response = executeRequest("POST", "$REST_BASE/tasks", json.toString(), headers)
            response.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error upserting task ${task.id} to Supabase: ${e.message}", e)
            false
        }
    }

    /**
     * Batch upserts multiple tasks to Supabase.
     */
    suspend fun upsertTasks(tasks: List<TaskEntity>): Boolean = withContext(Dispatchers.IO) {
        if (tasks.isEmpty()) return@withContext true
        try {
            val array = JSONArray()
            tasks.forEach { array.put(formatTaskToJson(it)) }
            val headers = mapOf("Prefer" to "resolution=merge-duplicates")
            val response = executeRequest("POST", "$REST_BASE/tasks", array.toString(), headers)
            response.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error batch upserting tasks to Supabase: ${e.message}", e)
            false
        }
    }

    /**
     * Deletes a task from Supabase by ID.
     */
    suspend fun deleteTask(taskId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val encodedId = URLEncoder.encode(taskId, "UTF-8")
            val response = executeRequest("DELETE", "$REST_BASE/tasks?id=eq.$encodedId", null)
            response.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task $taskId from Supabase: ${e.message}", e)
            false
        }
    }

    /**
     * Fetches all cloud-stored tasks belonging to a specific user ID.
     * This is called on fresh login/reinstall to restore all tasks!
     */
    suspend fun fetchTasksForUser(userId: String): List<TaskEntity> = withContext(Dispatchers.IO) {
        try {
            val encodedId = URLEncoder.encode(userId, "UTF-8")
            val url = "$REST_BASE/tasks?user_id=eq.$encodedId&order=created_at.desc"
            val response = executeRequest("GET", url, null)
            if (response.isSuccess && response.body != null) {
                val array = JSONArray(response.body)
                val list = mutableListOf<TaskEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(parseTaskFromJson(obj))
                }
                return@withContext list
            }
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks for user $userId from Supabase: ${e.message}", e)
            emptyList()
        }
    }

    // ==========================================
    // CATEGORY SYNCHRONIZATION METHODS
    // ==========================================

    suspend fun fetchCategories(): List<CategoryEntity> = withContext(Dispatchers.IO) {
        try {
            val response = executeRequest("GET", "$REST_BASE/categories?order=display_order.asc", null)
            if (response.isSuccess && response.body != null) {
                val array = JSONArray(response.body)
                val list = mutableListOf<CategoryEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        CategoryEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            colorHex = obj.getString("color_hex"),
                            iconName = obj.optString("icon_name", "List"),
                            isSystemDefault = obj.optBoolean("is_system_default", false),
                            displayOrder = obj.optInt("display_order", i)
                        )
                    )
                }
                return@withContext list
            }
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun upsertCategories(categories: List<CategoryEntity>): Boolean = withContext(Dispatchers.IO) {
        if (categories.isEmpty()) return@withContext true
        try {
            val array = JSONArray()
            categories.forEach { cat ->
                array.put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("color_hex", cat.colorHex)
                    put("icon_name", cat.iconName)
                    put("is_system_default", cat.isSystemDefault)
                    put("display_order", cat.displayOrder)
                })
            }
            val headers = mapOf("Prefer" to "resolution=merge-duplicates")
            val response = executeRequest("POST", "$REST_BASE/categories", array.toString(), headers)
            response.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // HELPER JSON SERIALIZATION
    // ==========================================

    private fun formatTaskToJson(task: TaskEntity): JSONObject {
        return JSONObject().apply {
            put("id", task.id)
            put("user_id", task.userId)
            put("title", task.title)
            put("category", task.category)
            put("date", task.date)
            put("time", task.time)
            put("is_completed", task.isCompleted)
            put("epoch_day", task.epochDay)
            put("priority", task.priority)
            put("eisenhower_quadrant", task.eisenhowerQuadrant)
            put("pomodoro_sessions_completed", task.pomodoroSessionsCompleted)
            put("pomodoro_estimated_sessions", task.pomodoroEstimatedSessions)
            put("created_at", task.createdAt)
        }
    }

    private fun parseTaskFromJson(obj: JSONObject): TaskEntity {
        return TaskEntity(
            id = obj.getString("id"),
            title = obj.getString("title"),
            category = obj.getString("category"),
            date = obj.optString("date", "Today"),
            time = obj.getString("time"),
            isCompleted = obj.optBoolean("is_completed", false),
            epochDay = obj.optLong("epoch_day", 0L),
            userId = obj.optString("user_id", ""),
            priority = obj.optString("priority", "NONE"),
            eisenhowerQuadrant = obj.optString("eisenhower_quadrant", "DO_FIRST"),
            pomodoroSessionsCompleted = obj.optInt("pomodoro_sessions_completed", 0),
            pomodoroEstimatedSessions = obj.optInt("pomodoro_estimated_sessions", 1),
            createdAt = obj.optLong("created_at", System.currentTimeMillis())
        )
    }

    private fun parseUserFromJson(obj: JSONObject): UserEntity {
        return UserEntity(
            id = obj.getString("id"),
            name = obj.getString("name"),
            username = obj.getString("username"),
            phone = obj.getString("phone"),
            email = obj.optString("email", ""),
            passwordHash = obj.getString("password_hash"),
            passwordSalt = obj.getString("password_salt"),
            lockType = obj.optString("lock_type", "pin_4"),
            lockHash = if (obj.isNull("lock_hash")) null else obj.getString("lock_hash"),
            lockSalt = if (obj.isNull("lock_salt")) null else obj.getString("lock_salt"),
            isLockEnabled = obj.optBoolean("is_lock_enabled", false),
            isBiometricEnabled = obj.optBoolean("is_biometric_enabled", false),
            isFaceAuthEnabled = obj.optBoolean("is_face_auth_enabled", false),
            isFingerprintAuthEnabled = obj.optBoolean("is_fingerprint_auth_enabled", false),
            lockTimeoutMs = obj.optLong("lock_timeout_ms", 0L),
            avatarPresetId = obj.optInt("avatar_preset_id", 0),
            customAvatarUri = if (obj.isNull("custom_avatar_uri")) null else obj.getString("custom_avatar_uri"),
            bio = obj.optString("bio", "Productivity Architect • Building minimal, powerful tools ⚡"),
            focusStatus = obj.optString("focus_status", "🎯 Deep Work"),
            userTier = obj.optString("user_tier", "TaskFlow Pro"),
            memberSince = obj.optString("member_since", "October 2026"),
            dailyTaskGoal = obj.optInt("daily_task_goal", 5),
            morningDigestTime = obj.optString("morning_digest_time", "08:00 AM"),
            autoCloudSync = obj.optBoolean("auto_cloud_sync", true),
            xpPoints = obj.optInt("xp_points", 0),
            currentStreak = obj.optInt("current_streak", 0),
            bestStreak = obj.optInt("best_streak", 0),
            createdAt = obj.optLong("created_at", System.currentTimeMillis()),
            lastLoginAt = obj.optLong("last_login_at", System.currentTimeMillis())
        )
    }

    // ==========================================
    // HTTP LOW-LEVEL TRANSPORT
    // ==========================================

    private data class NetworkResponse(
        val statusCode: Int,
        val body: String?,
        val isSuccess: Boolean
    )

    private fun executeRequest(
        method: String,
        urlString: String,
        payload: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): NetworkResponse {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doInput = true

                // Standard Supabase Headers
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer $ANON_KEY")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")

                extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }

                if (payload != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
                    doOutput = true
                    OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(payload)
                        writer.flush()
                    }
                }
            }

            val code = conn.responseCode
            val isSuccess = code in 200..299

            val inputStream = if (isSuccess) conn.inputStream else conn.errorStream
            val responseBody = inputStream?.let { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            }

            if (!isSuccess) {
                Log.w(TAG, "Supabase request failed [$code] $method $urlString: $responseBody")
            }

            NetworkResponse(code, responseBody, isSuccess)
        } catch (e: Exception) {
            Log.e(TAG, "Network exception during $method $urlString: ${e.message}")
            NetworkResponse(0, null, false)
        } finally {
            conn?.disconnect()
        }
    }
}
