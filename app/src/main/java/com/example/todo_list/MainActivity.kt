package com.example.todo_list

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.todo_list.notification.TaskNotificationScheduler
import com.example.todo_list.security.AppLockManager
import com.example.todo_list.security.AuthManager
import com.example.todo_list.ui.screens.HomeScreen
import com.example.todo_list.ui.screens.auth.AuthScreen
import com.example.todo_list.ui.screens.lock.AppLockAuthScreen
import com.example.todo_list.ui.theme.AppAccentColor
import com.example.todo_list.ui.theme.SystemBlue
import com.example.todo_list.ui.theme.SystemGroupedBackground
import com.example.todo_list.ui.theme.TaskFlowTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Clear saved instance state to ensure app always starts on home tab
        savedInstanceState?.clear()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TaskNotificationScheduler.createNotificationChannel(this)

        // Initialize persistent Auth & Session state
        com.example.todo_list.security.AuthManager.initialize(this)

        // Initialize persistent App Lock state
        AppLockManager.initialize(this)

        // Initialize persistent User Profile state
        com.example.todo_list.manager.UserProfileManager.initialize(this)

        // Initialize persistent Time Zone & Regional formatting state
        com.example.todo_list.manager.TimePreferencesManager.initialize(this)

        // Initialize persistent Haptics state
        com.example.todo_list.utils.HapticManager.initialize(this)

        // Initialize persistent App Icon Customization state
        com.example.todo_list.manager.AppIconManager.initialize(this)

        // Initialize persistent TaskFlow Intelligence AI state
        com.example.todo_list.ai.AiConfigurationManager.initialize(this)

        // Initialize persistent Floating AI Shortcut Button state & service
        com.example.todo_list.ai.FloatingAiButtonManager.initialize(this)

        // Initialize persistent Pomodoro Focus Timer state
        com.example.todo_list.manager.PomodoroTimerManager.initialize(this)

        // Handle direct AI shortcut intent from floating assistant
        handleAiChatIntent(intent)

        // Enforce 120Hz high refresh rate display mode & prevent OS throttling
        com.example.todo_list.utils.HighRefreshRateManager.enableHighRefreshRate(this)

        // Monitor activity lifecycle to lock app when backgrounded or reopened from recents
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    AppLockManager.onAppBackgrounded()
                    com.example.todo_list.manager.AppIconManager.onAppBackgrounded(this@MainActivity)
                }
                Lifecycle.Event.ON_START -> {
                    AppLockManager.onAppForegrounded()
                }
                Lifecycle.Event.ON_RESUME -> {
                    com.example.todo_list.utils.HighRefreshRateManager.onResume(this)
                    com.example.todo_list.manager.TimePreferencesManager.detectUserTimezone(this, forceNotify = false)
                    com.example.todo_list.ai.FloatingAiButtonManager.syncService(this)
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(lifecycleObserver)

        setContent {
            var themeMode by remember { mutableIntStateOf(0) } // 0: System, 1: Light, 2: Dark
            var selectedAccent by remember { mutableStateOf(AppAccentColor.BLUE) }
            val isDark = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            val isAuthInitialized = com.example.todo_list.security.AuthManager.isAuthInitialized
            val isLoggedIn = com.example.todo_list.security.AuthManager.isLoggedIn
            val isAccountCreated = com.example.todo_list.security.AuthManager.isAccountCreated

            TaskFlowTheme(darkTheme = isDark, accentColor = selectedAccent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SystemGroupedBackground)
                ) {
                    if (!isAuthInitialized) {
                        // Authentic Apple launch splash placeholder while SQLite accounts are loaded
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SystemBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "TaskFlow",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    } else if (!isAccountCreated || !isLoggedIn) {
                        // Registration & Login Gatekeeper
                        AuthScreen(
                            onAuthComplete = {
                                // User logged in or registered successfully
                            }
                        )
                    } else {
                        // Main application content
                        HomeScreen(
                            themeMode = themeMode,
                            onThemeModeChange = { themeMode = it },
                            accentColor = selectedAccent,
                            onAccentColorChange = { selectedAccent = it }
                        )

                        // Gatekeeper App Lock Overlay (Requires passcode or fingerprint to continue)
                        if (AppLockManager.isLocked) {
                            AppLockAuthScreen()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAiChatIntent(intent)
    }

    private fun handleAiChatIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_AI_CHAT, false) == true) {
            openAiChatTrigger.value = System.currentTimeMillis()
        }
    }

    companion object {
        const val EXTRA_OPEN_AI_CHAT = "extra_open_ai_chat"
        val openAiChatTrigger = androidx.compose.runtime.mutableStateOf<Long?>(null)
    }
}

// this is the main file and alse have to optional and for that we have to use some other package....???