package com.example.todo_list.security

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.random.Random

/**
 * Reactive Singleton Manager for persistent User Authentication,
 * Phone OTP Verification, Credentials Hashing, and Session Management.
 */
object AuthManager {
    private const val PREFS_NAME = "taskflow_auth_prefs"

    private const val KEY_IS_ACCOUNT_CREATED = "key_is_account_created"
    private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private const val KEY_USER_PHONE = "key_user_phone"
    private const val KEY_USER_USERNAME = "key_user_username"
    private const val KEY_USER_NAME = "key_user_name"
    private const val KEY_USER_EMAIL = "key_user_email"
    private const val KEY_PASSWORD_HASH = "key_password_hash"
    private const val KEY_PASSWORD_SALT = "key_password_salt"
    private const val KEY_CREATED_AT = "key_created_at"

    private var prefs: SharedPreferences? = null

    // Reactive Compose States
    var isAccountCreated by mutableStateOf(false)
        private set

    var isLoggedIn by mutableStateOf(false)
        private set

    var registeredPhone by mutableStateOf("")
        private set

    var registeredUsername by mutableStateOf("")
        private set

    var registeredName by mutableStateOf("")
        private set

    var registeredEmail by mutableStateOf("")
        private set

    // Active Pending OTP State (InMemory)
    var pendingOtpCode by mutableStateOf<String?>(null)
        private set

    var pendingPhone by mutableStateOf("")
        private set

    var otpSentTimestamp by mutableStateOf(0L)
        private set

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            reloadState()
        }
    }

    private fun reloadState() {
        prefs?.let { p ->
            isAccountCreated = p.getBoolean(KEY_IS_ACCOUNT_CREATED, false)
            isLoggedIn = p.getBoolean(KEY_IS_LOGGED_IN, false)
            registeredPhone = p.getString(KEY_USER_PHONE, "") ?: ""
            registeredUsername = p.getString(KEY_USER_USERNAME, "") ?: ""
            registeredName = p.getString(KEY_USER_NAME, "") ?: ""
            registeredEmail = p.getString(KEY_USER_EMAIL, "") ?: ""
        }
    }

    /**
     * Sends/Generates a random 6-digit OTP and dispatches a REAL SMS via Fast2SMS to the phone number.
     * Returns the generated OTP and the delivery result.
     */
    suspend fun sendRealSmsOtp(phone: String): Pair<String, SmsDeliveryResult> {
        val code = String.format("%06d", Random.nextInt(100000, 999999))
        pendingOtpCode = code
        pendingPhone = phone
        otpSentTimestamp = System.currentTimeMillis()

        val deliveryResult = Fast2SmsService.sendOtpSms(phoneNumber = phone, otpCode = code)
        return Pair(code, deliveryResult)
    }

    /**
     * Sends/Generates a random 6-digit OTP for phone verification.
     */
    fun sendOtp(phone: String): String {
        val code = String.format("%06d", Random.nextInt(100000, 999999))
        pendingOtpCode = code
        pendingPhone = phone
        otpSentTimestamp = System.currentTimeMillis()
        return code
    }

    /**
     * Verifies the input code against the active pending OTP.
     */
    fun verifyOtp(inputCode: String): Boolean {
        val activeCode = pendingOtpCode ?: return false
        val isValid = inputCode.trim() == activeCode.trim()
        return isValid
    }

    /**
     * Clears pending OTP after successful verification.
     */
    fun clearPendingOtp() {
        pendingOtpCode = null
    }

    /**
     * Registers a new account with hashed password and logs the user in.
     */
    fun registerAccount(
        name: String,
        username: String,
        phone: String,
        password: String,
        email: String = ""
    ): Boolean {
        val cleanName = name.trim()
        val cleanUsername = if (username.startsWith("@")) username.trim() else "@${username.trim()}"
        val cleanPhone = phone.trim()
        val cleanEmail = if (email.isBlank()) "${cleanUsername.removePrefix("@").lowercase()}@taskflow.app" else email.trim()

        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        val createdAt = System.currentTimeMillis()

        prefs?.edit()
            ?.putBoolean(KEY_IS_ACCOUNT_CREATED, true)
            ?.putBoolean(KEY_IS_LOGGED_IN, true)
            ?.putString(KEY_USER_PHONE, cleanPhone)
            ?.putString(KEY_USER_USERNAME, cleanUsername)
            ?.putString(KEY_USER_NAME, cleanName)
            ?.putString(KEY_USER_EMAIL, cleanEmail)
            ?.putString(KEY_PASSWORD_HASH, hash)
            ?.putString(KEY_PASSWORD_SALT, salt)
            ?.putLong(KEY_CREATED_AT, createdAt)
            ?.apply()

        isAccountCreated = true
        isLoggedIn = true
        registeredPhone = cleanPhone
        registeredUsername = cleanUsername
        registeredName = cleanName
        registeredEmail = cleanEmail
        pendingOtpCode = null

        return true
    }

    /**
     * Authenticates user against stored credentials.
     * Supports login with username (with or without '@') or phone number.
     */
    fun login(identifier: String, password: String): Boolean {
        if (!isAccountCreated) return false

        val storedUsername = prefs?.getString(KEY_USER_USERNAME, "") ?: ""
        val storedPhone = prefs?.getString(KEY_USER_PHONE, "") ?: ""
        val storedHash = prefs?.getString(KEY_PASSWORD_HASH, null) ?: return false
        val storedSalt = prefs?.getString(KEY_PASSWORD_SALT, null) ?: return false

        val cleanId = identifier.trim()
        val formattedIdWithAt = if (cleanId.startsWith("@")) cleanId else "@$cleanId"

        val matchesUsername = cleanId.equals(storedUsername, ignoreCase = true) ||
                formattedIdWithAt.equals(storedUsername, ignoreCase = true)
        val matchesPhone = cleanId.filter { it.isDigit() } == storedPhone.filter { it.isDigit() }

        if (!matchesUsername && !matchesPhone) {
            return false
        }

        val inputHash = hashPassword(password, storedSalt)
        if (inputHash == storedHash) {
            prefs?.edit()?.putBoolean(KEY_IS_LOGGED_IN, true)?.apply()
            isLoggedIn = true
            return true
        }

        return false
    }

    /**
     * Log out current user session.
     */
    fun logout() {
        prefs?.edit()?.putBoolean(KEY_IS_LOGGED_IN, false)?.apply()
        isLoggedIn = false
    }

    /**
     * Updates password for the registered account after OTP recovery.
     */
    fun resetPassword(newPassword: String): Boolean {
        if (!isAccountCreated) return false

        val newSalt = generateSalt()
        val newHash = hashPassword(newPassword, newSalt)

        prefs?.edit()
            ?.putString(KEY_PASSWORD_HASH, newHash)
            ?.putString(KEY_PASSWORD_SALT, newSalt)
            ?.apply()

        return true
    }

    /**
     * Check if a given phone number belongs to the registered account.
     */
    fun isRegisteredPhone(phone: String): Boolean {
        val stored = registeredPhone.filter { it.isDigit() }
        val input = phone.filter { it.isDigit() }
        return stored.isNotEmpty() && stored == input
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((salt + password).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
