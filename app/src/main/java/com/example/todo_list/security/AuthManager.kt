package com.example.todo_list.security

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.todo_list.data.local.AppDatabase
import com.example.todo_list.data.local.dao.UserDao
import com.example.todo_list.data.local.entity.UserEntity
import com.example.todo_list.data.repository.TaskRepository
import com.example.todo_list.manager.UserProfileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import kotlin.random.Random

/**
 * Reactive Singleton Manager for persistent Multi-User Authentication,
 * SQLite User Records, Phone OTP Verification, Credentials Hashing, and Session Management.
 */
object AuthManager {
    private const val PREFS_NAME = "taskflow_auth_prefs"
    private const val KEY_ACTIVE_USER_ID = "key_active_user_id"
    private const val KEY_EXPLICIT_LOGOUT = "key_explicit_logout"

    private var prefs: SharedPreferences? = null
    private var userDao: UserDao? = null
    private var appContext: Context? = null

    // Reactive Compose States
    var currentUser by mutableStateOf<UserEntity?>(null)

    var currentUserId by mutableStateOf<String?>(null)
        private set

    var isAccountCreated by mutableStateOf(false)
        private set

    var isLoggedIn by mutableStateOf(false)
        private set

    var isAuthInitialized by mutableStateOf(false)
        private set

    // Convenience getters for UI components
    val registeredPhone: String
        get() = currentUser?.phone ?: ""

    val registeredUsername: String
        get() = currentUser?.username ?: ""

    val registeredName: String
        get() = currentUser?.name ?: ""

    val registeredEmail: String
        get() = currentUser?.email ?: ""

    // Active Pending OTP State (InMemory)
    var pendingOtpCode by mutableStateOf<String?>(null)
        private set

    var pendingPhone by mutableStateOf("")
        private set

    var otpSentTimestamp by mutableStateOf(0L)
        private set

    const val OTP_VALIDITY_SECONDS = 30
    var otpExpiryTimestamp by mutableStateOf(0L)
        private set

    private const val LEGACY_KEY_USERNAME = "key_user_username"
    private const val LEGACY_KEY_PHONE = "key_user_phone"
    private const val LEGACY_KEY_NAME = "key_user_name"
    private const val LEGACY_KEY_EMAIL = "key_user_email"
    private const val LEGACY_KEY_PASSWORD_HASH = "key_password_hash"
    private const val LEGACY_KEY_PASSWORD_SALT = "key_password_salt"

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val db = AppDatabase.getInstance(context.applicationContext)
        userDao = db.userDao()

        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        CoroutineScope(Dispatchers.IO).launch {
            var userCount = userDao?.getUserCount() ?: 0
            val explicitLogout = prefs?.getBoolean(KEY_EXPLICIT_LOGOUT, false) ?: false
            val savedUserId = prefs?.getString(KEY_ACTIVE_USER_ID, null)

            var resolvedUser: UserEntity? = null
            if (savedUserId != null) {
                resolvedUser = userDao?.getUserById(savedUserId)
            }

            // Automatic legacy recovery: If no SQLite accounts exist, check if legacy
            // SharedPreferences user credentials exist from prior versions and restore them.
            if (userCount == 0) {
                val legacyUser = prefs?.getString(LEGACY_KEY_USERNAME, null)
                val legacyPhone = prefs?.getString(LEGACY_KEY_PHONE, null)
                val legacyHash = prefs?.getString(LEGACY_KEY_PASSWORD_HASH, null)
                val legacySalt = prefs?.getString(LEGACY_KEY_PASSWORD_SALT, null)
                val legacyName = prefs?.getString(LEGACY_KEY_NAME, "User") ?: "User"
                val legacyEmail = prefs?.getString(LEGACY_KEY_EMAIL, "") ?: ""

                if (!legacyUser.isNullOrBlank() && !legacyHash.isNullOrBlank() && !legacySalt.isNullOrBlank()) {
                    val recoveredId = UUID.randomUUID().toString()
                    val recoveredUser = UserEntity(
                        id = recoveredId,
                        name = legacyName,
                        username = if (legacyUser.startsWith("@")) legacyUser else "@$legacyUser",
                        phone = legacyPhone ?: "",
                        email = legacyEmail,
                        passwordHash = legacyHash,
                        passwordSalt = legacySalt,
                        createdAt = System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis()
                    )
                    userDao?.insertUser(recoveredUser)
                    userCount = 1
                    resolvedUser = recoveredUser
                    prefs?.edit()
                        ?.putString(KEY_ACTIVE_USER_ID, recoveredId)
                        ?.putBoolean(KEY_EXPLICIT_LOGOUT, false)
                        ?.commit()
                }
            }

            // Automatic fallback: If no active user is saved (e.g. crash before prefs flush
            // or cache clearance), but accounts exist and the user didn't explicitly log out,
            // recover the most recent user record.
            if (resolvedUser == null && !explicitLogout && userCount > 0) {
                resolvedUser = userDao?.getMostRecentUser()
                resolvedUser?.let {
                    prefs?.edit()
                        ?.putString(KEY_ACTIVE_USER_ID, it.id)
                        ?.putBoolean(KEY_EXPLICIT_LOGOUT, false)
                        ?.commit()
                }
            }

            withContext(Dispatchers.Main) {
                isAccountCreated = userCount > 0
                if (resolvedUser != null) {
                    currentUser = resolvedUser
                    currentUserId = resolvedUser.id
                    isLoggedIn = true
                    UserProfileManager.loadUserProfile(resolvedUser)
                    AppLockManager.loadUserLock(resolvedUser)
                } else {
                    isLoggedIn = false
                    currentUserId = null
                    currentUser = null
                }
                isAuthInitialized = true
            }
        }
    }

    /**
     * Checks whether any users exist in the SQLite database.
     */
    suspend fun hasAnyUsers(): Boolean = withContext(Dispatchers.IO) {
        val count = userDao?.getUserCount() ?: 0
        withContext(Dispatchers.Main) {
            isAccountCreated = count > 0
        }
        count > 0
    }

    /**
     * Sends/Generates a random 6-digit OTP and dispatches a REAL SMS via Fast2SMS to the phone number.
     */
    suspend fun sendRealSmsOtp(phone: String): Pair<String, SmsDeliveryResult> {
        val code = String.format("%06d", Random.nextInt(100000, 999999))
        pendingOtpCode = code
        pendingPhone = phone
        otpSentTimestamp = System.currentTimeMillis()
        otpExpiryTimestamp = otpSentTimestamp + (OTP_VALIDITY_SECONDS * 1000L)

        val deliveryResult = Fast2SmsService.sendOtpSms(phoneNumber = phone, otpCode = code)
        return Pair(code, deliveryResult)
    }

    /**
     * Sends/Generates a random 6-digit OTP for phone verification using the built-in system.
     */
    fun sendOtp(phone: String): String {
        val code = String.format("%06d", Random.nextInt(100000, 999999))
        pendingOtpCode = code
        pendingPhone = phone
        otpSentTimestamp = System.currentTimeMillis()
        otpExpiryTimestamp = otpSentTimestamp + (OTP_VALIDITY_SECONDS * 1000L)
        return code
    }

    fun isOtpExpired(): Boolean {
        if (pendingOtpCode == null || otpExpiryTimestamp == 0L) return true
        return System.currentTimeMillis() > otpExpiryTimestamp
    }

    fun expireCurrentOtp() {
        pendingOtpCode = null
        otpExpiryTimestamp = 0L
    }

    fun verifyOtp(inputCode: String): Boolean {
        val activeCode = pendingOtpCode ?: return false
        if (isOtpExpired()) {
            return false
        }
        return inputCode.trim() == activeCode.trim()
    }

    fun clearPendingOtp() {
        pendingOtpCode = null
        otpExpiryTimestamp = 0L
    }

    /**
     * Registers a new account and immediately inserts a new record into the SQLite 'users' table.
     */
    suspend fun registerAccount(
        name: String,
        username: String,
        phone: String,
        password: String,
        email: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val dao = userDao ?: return@withContext false
        val cleanName = name.trim()
        val cleanUsername = if (username.startsWith("@")) username.trim() else "@${username.trim()}"
        val cleanPhone = phone.trim()
        val cleanEmail = if (email.isBlank()) "${cleanUsername.removePrefix("@").lowercase()}@taskflow.app" else email.trim()

        // Check if username or phone already exists
        val existingByUsername = dao.getUserByUsername(cleanUsername)
        if (existingByUsername != null) return@withContext false

        val existingByPhone = dao.getUserByPhone(cleanPhone)
        if (existingByPhone != null) return@withContext false

        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        val newUserId = UUID.randomUUID().toString()

        val newUser = UserEntity(
            id = newUserId,
            name = cleanName,
            username = cleanUsername,
            phone = cleanPhone,
            email = cleanEmail,
            passwordHash = hash,
            passwordSalt = salt,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )

        dao.insertUser(newUser)

        // Seed initial starter tasks for this specific user
        appContext?.let { ctx ->
            TaskRepository.getInstance(ctx).seedStarterTasksForUser(newUserId)
        }

        prefs?.edit()
            ?.putString(KEY_ACTIVE_USER_ID, newUserId)
            ?.putBoolean(KEY_EXPLICIT_LOGOUT, false)
            ?.commit()

        withContext(Dispatchers.Main) {
            currentUser = newUser
            currentUserId = newUserId
            isAccountCreated = true
            isLoggedIn = true
            pendingOtpCode = null

            UserProfileManager.loadUserProfile(newUser)
            AppLockManager.loadUserLock(newUser)
        }

        true
    }

    /**
     * Authenticates user against stored credentials in SQLite 'users' table.
     * Supports login with username (with or without '@') or phone number.
     */
    suspend fun login(identifier: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val dao = userDao ?: return@withContext false
        val cleanId = identifier.trim()
        val user = dao.getUserByIdentifier(cleanId) ?: return@withContext false

        val inputHash = hashPassword(password, user.passwordSalt)
        if (inputHash == user.passwordHash) {
            dao.updateLastLogin(user.id)
            prefs?.edit()
                ?.putString(KEY_ACTIVE_USER_ID, user.id)
                ?.putBoolean(KEY_EXPLICIT_LOGOUT, false)
                ?.commit()

            withContext(Dispatchers.Main) {
                currentUser = user
                currentUserId = user.id
                isLoggedIn = true
                isAccountCreated = true

                UserProfileManager.loadUserProfile(user)
                AppLockManager.loadUserLock(user)
            }
            return@withContext true
        }

        false
    }

    /**
     * Retrieves all saved user accounts stored in the SQLite database.
     */
    suspend fun getSavedAccounts(): List<UserEntity> = withContext(Dispatchers.IO) {
        userDao?.getAllUsers() ?: emptyList()
    }

    /**
     * Signs in directly as a specific user after biometric authentication has succeeded.
     */
    suspend fun loginWithBiometrics(targetUser: UserEntity): Boolean = withContext(Dispatchers.IO) {
        val dao = userDao ?: return@withContext false
        val user = dao.getUserById(targetUser.id) ?: return@withContext false

        dao.updateLastLogin(user.id)
        prefs?.edit()
            ?.putString(KEY_ACTIVE_USER_ID, user.id)
            ?.putBoolean(KEY_EXPLICIT_LOGOUT, false)
            ?.commit()

        val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
        withContext(Dispatchers.Main) {
            currentUser = updatedUser
            currentUserId = updatedUser.id
            isLoggedIn = true
            isAccountCreated = true

            UserProfileManager.loadUserProfile(updatedUser)
            AppLockManager.loadUserLock(updatedUser)
        }
        true
    }

    /**
     * Logs in as a specified user using their password.
     */
    suspend fun loginUserWithPassword(user: UserEntity, password: String): Boolean = withContext(Dispatchers.IO) {
        val dao = userDao ?: return@withContext false
        val inputHash = hashPassword(password, user.passwordSalt)
        if (inputHash == user.passwordHash) {
            dao.updateLastLogin(user.id)
            prefs?.edit()
                ?.putString(KEY_ACTIVE_USER_ID, user.id)
                ?.putBoolean(KEY_EXPLICIT_LOGOUT, false)
                ?.commit()

            val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
            withContext(Dispatchers.Main) {
                currentUser = updatedUser
                currentUserId = updatedUser.id
                isLoggedIn = true
                isAccountCreated = true

                UserProfileManager.loadUserProfile(updatedUser)
                AppLockManager.loadUserLock(updatedUser)
            }
            return@withContext true
        }
        false
    }

    /**
     * Removes an account from the local SQLite database.
     */
    suspend fun removeAccountLocally(userId: String): Boolean = withContext(Dispatchers.IO) {
        val dao = userDao ?: return@withContext false
        dao.deleteUserById(userId)
        val remainingCount = dao.getUserCount()
        withContext(Dispatchers.Main) {
            isAccountCreated = remainingCount > 0
            if (currentUserId == userId) {
                logout()
            }
        }
        true
    }

    /**
     * Log out current user session.
     */
    fun logout() {
        prefs?.edit()
            ?.remove(KEY_ACTIVE_USER_ID)
            ?.putBoolean(KEY_EXPLICIT_LOGOUT, true)
            ?.commit()
        currentUserId = null
        currentUser = null
        isLoggedIn = false
        AppLockManager.resetLockOnLogout()
    }

    /**
     * Validates if the input password matches the stored hashed credentials of active user.
     */
    fun validateCurrentPassword(password: String): Boolean {
        val user = currentUser ?: return true
        val inputHash = hashPassword(password, user.passwordSalt)
        return inputHash == user.passwordHash
    }

    fun isCurrentPasswordSet(): Boolean {
        return currentUser?.passwordHash != null
    }

    suspend fun isRegisteredPhone(phone: String): Boolean = withContext(Dispatchers.IO) {
        val dao = userDao ?: return@withContext false
        val cleanPhone = phone.trim()
        val user = dao.getUserByPhone(cleanPhone) ?: dao.getUserByIdentifier(cleanPhone)
        user != null
    }

    /**
     * Checks if a user exists by username or phone.
     */
    suspend fun userExists(identifier: String): Boolean = withContext(Dispatchers.IO) {
        val dao = userDao ?: return@withContext false
        val cleanId = identifier.trim()
        if (cleanId.isEmpty()) return@withContext false
        val user = dao.getUserByIdentifier(cleanId)
        user != null
    }

    /**
     * Updates password for the registered account securely in SQLite 'users' table.
     */
    suspend fun updatePassword(phoneOrUser: String, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        val dao = userDao ?: return@withContext false
        val user = dao.getUserByIdentifier(phoneOrUser.trim()) ?: return@withContext false

        val newSalt = generateSalt()
        val newHash = hashPassword(newPassword, newSalt)
        dao.updateUserPassword(user.id, newHash, newSalt)

        if (currentUser?.id == user.id) {
            val updated = user.copy(passwordHash = newHash, passwordSalt = newSalt)
            withContext(Dispatchers.Main) {
                currentUser = updated
            }
        }
        true
    }

    suspend fun resetPassword(newPassword: String): Boolean {
        val identifier = pendingPhone.ifEmpty { registeredPhone.ifEmpty { registeredUsername } }
        return updatePassword(identifier, newPassword)
    }

    fun updateUsername(newUsername: String): Boolean {
        val cleanUsername = if (newUsername.startsWith("@")) newUsername.trim() else "@${newUsername.trim()}"
        val user = currentUser ?: return false
        val updated = user.copy(username = cleanUsername)
        currentUser = updated
        val ctx = appContext ?: return true
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getInstance(ctx).userDao()
            dao.updateUser(updated)
        }
        return true
    }

    fun updatePasswordSecure(newPassword: String): Boolean {
        val user = currentUser ?: return false
        val newSalt = generateSalt()
        val newHash = hashPassword(newPassword, newSalt)
        val updated = user.copy(passwordHash = newHash, passwordSalt = newSalt)
        currentUser = updated
        val ctx = appContext ?: return true
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getInstance(ctx).userDao()
            dao.updateUserPassword(user.id, newHash, newSalt)
        }
        return true
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val saltedPassword = "$password$salt"
        val digest = md.digest(saltedPassword.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
