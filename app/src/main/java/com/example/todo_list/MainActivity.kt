package com.example.todo_list

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.todo_list.notification.TaskNotificationScheduler
import com.example.todo_list.security.AppLockManager
import com.example.todo_list.ui.screens.HomeScreen
import com.example.todo_list.ui.screens.lock.AppLockAuthScreen
import com.example.todo_list.ui.theme.AppAccentColor
import com.example.todo_list.ui.theme.TaskFlowTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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

        // Enforce 120Hz high refresh rate display mode & prevent OS throttling
        com.example.todo_list.utils.HighRefreshRateManager.enableHighRefreshRate(this)

        // Monitor activity lifecycle to lock app when backgrounded or reopened from recents
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    AppLockManager.onAppBackgrounded()
                }
                Lifecycle.Event.ON_START -> {
                    AppLockManager.onAppForegrounded()
                }
                Lifecycle.Event.ON_RESUME -> {
                    com.example.todo_list.utils.HighRefreshRateManager.onResume(this)
                    com.example.todo_list.manager.TimePreferencesManager.detectUserTimezone(this, forceNotify = false)
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

            val isLoggedIn = com.example.todo_list.security.AuthManager.isLoggedIn
            val isAccountCreated = com.example.todo_list.security.AuthManager.isAccountCreated

            TaskFlowTheme(darkTheme = isDark, accentColor = selectedAccent) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!isAccountCreated || !isLoggedIn) {
                        // Registration & Login Gatekeeper
                        com.example.todo_list.ui.screens.auth.AuthScreen(
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
}
