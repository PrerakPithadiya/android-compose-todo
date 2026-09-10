package com.example.todo_list.security

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.todo_list.data.local.AppDatabase
import com.example.todo_list.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom

object AppLockManager {
    private const val PREFS_NAME = "taskflow_app_lock_prefs"
    private const val KEY_LAST_BACKGROUND_TIME = "key_last_bg_time"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    // Reactive Compose States
    var isLocked by mutableStateOf(false)
        private set

    var isLockEnabled by mutableStateOf(false)
        private set

    var isBiometricEnabled by mutableStateOf(false)
        private set

    var currentLockType by mutableStateOf(LockType.PIN_4)
        private set

    var lockTimeoutMs by mutableLongStateOf(0L) // 0L = Immediately
        private set

    private var lastBackgroundTimestamp: Long = 0L

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            lastBackgroundTimestamp = prefs?.getLong(KEY_LAST_BACKGROUND_TIME, 0L) ?: 0L
        }
    }

    /**
     * Loads lock settings for the active user account from SQLite.
     */
    fun loadUserLock(user: UserEntity?) {
        if (user == null) {
            isLockEnabled = false
            isBiometricEnabled = false
            isLocked = false
            currentLockType = LockType.PIN_4
            lockTimeoutMs = 0L
            return
        }

        isLockEnabled = user.isLockEnabled
        isBiometricEnabled = user.isBiometricEnabled
        currentLockType = LockType.fromId(user.lockType)
        lockTimeoutMs = user.lockTimeoutMs

        if (isLockEnabled) {
            val lastBg = prefs?.getLong(KEY_LAST_BACKGROUND_TIME, 0L) ?: 0L
            if (lockTimeoutMs == 0L || lastBg == 0L) {
                isLocked = true
            } else {
                val elapsed = System.currentTimeMillis() - lastBg
                isLocked = elapsed >= lockTimeoutMs
            }
        } else {
            isLocked = false
        }
    }

    fun resetLockOnLogout() {
        isLocked = false
        isLockEnabled = false
        isBiometricEnabled = false
        lastBackgroundTimestamp = 0L
        prefs?.edit()?.putLong(KEY_LAST_BACKGROUND_TIME, 0L)?.apply()
    }

    fun setLock(type: LockType, secret: String) {
        val salt = generateSalt()
        val hash = hashSecret(secret, salt)

        isLockEnabled = true
        currentLockType = type
        isLocked = false
        lastBackgroundTimestamp = 0L
        prefs?.edit()?.putLong(KEY_LAST_BACKGROUND_TIME, 0L)?.apply()

        val userId = AuthManager.currentUserId
        if (userId != null && appContext != null) {
            val updatedUser = AuthManager.currentUser?.copy(
                lockType = type.id,
                lockHash = hash,
                lockSalt = salt,
                isLockEnabled = true
            )
            AuthManager.currentUser = updatedUser

            CoroutineScope(Dispatchers.IO).launch {
                val dao = AppDatabase.getInstance(appContext!!).userDao()
                dao.updateUserLock(
                    userId = userId,
                    lockType = type.id,
                    lockHash = hash,
                    lockSalt = salt,
                    isLockEnabled = true,
                    isBiometricEnabled = isBiometricEnabled,
                    lockTimeoutMs = lockTimeoutMs
                )
            }
        }
    }

    fun verifySecret(inputSecret: String): Boolean {
        val user = AuthManager.currentUser
        val storedHash = user?.lockHash ?: return false
        val storedSalt = user.lockSalt ?: return false
        val inputHash = hashSecret(inputSecret, storedSalt)
        return inputHash == storedHash
    }

    fun setBiometricAuth(enabled: Boolean) {
        isBiometricEnabled = enabled
        val user = AuthManager.currentUser
        if (user != null && appContext != null) {
            AuthManager.currentUser = user.copy(isBiometricEnabled = enabled)
            CoroutineScope(Dispatchers.IO).launch {
                val dao = AppDatabase.getInstance(appContext!!).userDao()
                dao.updateUserLock(
                    userId = user.id,
                    lockType = currentLockType.id,
                    lockHash = user.lockHash,
                    lockSalt = user.lockSalt,
                    isLockEnabled = isLockEnabled,
                    isBiometricEnabled = enabled,
                    lockTimeoutMs = lockTimeoutMs
                )
            }
        }
    }

    fun disableLock() {
        isLockEnabled = false
        isBiometricEnabled = false
        isLocked = false
        lastBackgroundTimestamp = 0L
        prefs?.edit()?.putLong(KEY_LAST_BACKGROUND_TIME, 0L)?.apply()

        val user = AuthManager.currentUser
        if (user != null && appContext != null) {
            AuthManager.currentUser = user.copy(
                isLockEnabled = false,
                isBiometricEnabled = false,
                lockHash = null,
                lockSalt = null
            )
            CoroutineScope(Dispatchers.IO).launch {
                val dao = AppDatabase.getInstance(appContext!!).userDao()
                dao.updateUserLock(
                    userId = user.id,
                    lockType = currentLockType.id,
                    lockHash = null,
                    lockSalt = null,
                    isLockEnabled = false,
                    isBiometricEnabled = false,
                    lockTimeoutMs = lockTimeoutMs
                )
            }
        }
    }

    fun unlock() {
        isLocked = false
        lastBackgroundTimestamp = 0L
        prefs?.edit()?.putLong(KEY_LAST_BACKGROUND_TIME, 0L)?.apply()
    }

    fun lock() {
        if (isLockEnabled) {
            isLocked = true
        }
    }

    fun setTimeout(timeoutMs: Long) {
        lockTimeoutMs = timeoutMs
        val user = AuthManager.currentUser
        if (user != null && appContext != null) {
            AuthManager.currentUser = user.copy(lockTimeoutMs = timeoutMs)
            CoroutineScope(Dispatchers.IO).launch {
                val dao = AppDatabase.getInstance(appContext!!).userDao()
                dao.updateUserLock(
                    userId = user.id,
                    lockType = currentLockType.id,
                    lockHash = user.lockHash,
                    lockSalt = user.lockSalt,
                    isLockEnabled = isLockEnabled,
                    isBiometricEnabled = isBiometricEnabled,
                    lockTimeoutMs = timeoutMs
                )
            }
        }
    }

    fun onAppBackgrounded() {
        if (!isLockEnabled) return
        val now = System.currentTimeMillis()
        lastBackgroundTimestamp = now
        prefs?.edit()?.putLong(KEY_LAST_BACKGROUND_TIME, now)?.apply()

        if (lockTimeoutMs == 0L) {
            isLocked = true
        }
    }

    fun onAppForegrounded() {
        if (!isLockEnabled) return
        if (isLocked) return

        val storedBgTime = prefs?.getLong(KEY_LAST_BACKGROUND_TIME, 0L) ?: lastBackgroundTimestamp
        val bgTime = if (storedBgTime > 0L) storedBgTime else lastBackgroundTimestamp

        if (bgTime > 0L) {
            val elapsed = System.currentTimeMillis() - bgTime
            if (elapsed >= lockTimeoutMs) {
                isLocked = true
            }
            lastBackgroundTimestamp = 0L
            prefs?.edit()?.putLong(KEY_LAST_BACKGROUND_TIME, 0L)?.apply()
        }
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashSecret(secret: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((salt + secret).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
