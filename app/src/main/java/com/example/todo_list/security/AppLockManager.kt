package com.example.todo_list.security

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest
import java.security.SecureRandom

object AppLockManager {
    private const val PREFS_NAME = "taskflow_app_lock_prefs"
    private const val KEY_LOCK_ENABLED = "key_lock_enabled"
    private const val KEY_LOCK_TYPE = "key_lock_type"
    private const val KEY_LOCK_HASH = "key_lock_hash"
    private const val KEY_LOCK_SALT = "key_lock_salt"
    private const val KEY_LOCK_TIMEOUT = "key_lock_timeout"
    private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
    private const val KEY_LAST_BACKGROUND_TIME = "key_last_bg_time"

    private var prefs: SharedPreferences? = null

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
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            reloadState()

            if (isLockEnabled) {
                val lastBg = prefs?.getLong(KEY_LAST_BACKGROUND_TIME, 0L) ?: 0L
                if (lockTimeoutMs == 0L || lastBg == 0L) {
                    isLocked = true
                } else {
                    val elapsed = System.currentTimeMillis() - lastBg
                    if (elapsed >= lockTimeoutMs) {
                        isLocked = true
                    } else {
                        isLocked = false
                    }
                }
            }
        }
    }

    private fun reloadState() {
        prefs?.let { p ->
            isLockEnabled = p.getBoolean(KEY_LOCK_ENABLED, false)
            isBiometricEnabled = p.getBoolean(KEY_BIOMETRIC_ENABLED, false)
            val typeStr = p.getString(KEY_LOCK_TYPE, LockType.PIN_4.id)
            currentLockType = LockType.fromId(typeStr)
            lockTimeoutMs = p.getLong(KEY_LOCK_TIMEOUT, 0L)
            lastBackgroundTimestamp = p.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        }
    }

    fun setLock(type: LockType, secret: String) {
        val salt = generateSalt()
        val hash = hashSecret(secret, salt)

        prefs?.edit()
            ?.putBoolean(KEY_LOCK_ENABLED, true)
            ?.putString(KEY_LOCK_TYPE, type.id)
            ?.putString(KEY_LOCK_HASH, hash)
            ?.putString(KEY_LOCK_SALT, salt)
            ?.apply()

        isLockEnabled = true
        currentLockType = type
        isLocked = false
        lastBackgroundTimestamp = 0L
        prefs?.edit()?.putLong(KEY_LAST_BACKGROUND_TIME, 0L)?.apply()
    }

    fun verifySecret(inputSecret: String): Boolean {
        val storedHash = prefs?.getString(KEY_LOCK_HASH, null) ?: return false
        val storedSalt = prefs?.getString(KEY_LOCK_SALT, null) ?: return false
        val inputHash = hashSecret(inputSecret, storedSalt)
        return inputHash == storedHash
    }

    fun setBiometricAuth(enabled: Boolean) {
        isBiometricEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_BIOMETRIC_ENABLED, enabled)?.apply()
    }

    fun disableLock() {
        prefs?.edit()
            ?.putBoolean(KEY_LOCK_ENABLED, false)
            ?.putBoolean(KEY_BIOMETRIC_ENABLED, false)
            ?.remove(KEY_LOCK_HASH)
            ?.remove(KEY_LOCK_SALT)
            ?.putLong(KEY_LAST_BACKGROUND_TIME, 0L)
            ?.apply()

        isLockEnabled = false
        isBiometricEnabled = false
        isLocked = false
        lastBackgroundTimestamp = 0L
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
        prefs?.edit()?.putLong(KEY_LOCK_TIMEOUT, timeoutMs)?.apply()
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
            // Reset background timestamp since app is actively in foreground now
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
