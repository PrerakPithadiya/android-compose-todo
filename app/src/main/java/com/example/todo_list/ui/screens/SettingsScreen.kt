package com.example.todo_list.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.example.todo_list.ai.AiConfigurationManager
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskListCategory
import com.example.todo_list.security.AppLockManager
import com.example.todo_list.security.BiometricAuthHelper
import com.example.todo_list.security.BiometricAvailability
import com.example.todo_list.security.LockType
import com.example.todo_list.ui.components.CreateCategoryBottomSheet
import com.example.todo_list.ui.components.DeleteCategoryMigrationDialog
import com.example.todo_list.ui.screens.lock.AppLockSetupSheet
import com.example.todo_list.ui.screens.lock.SetupStep
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticIntensity
import com.example.todo_list.utils.HapticManager
import com.example.todo_list.manager.TimePreferencesManager
import com.example.todo_list.manager.AppIcon
import com.example.todo_list.manager.AppIconManager
import com.example.todo_list.model.WorldLocation
import com.example.todo_list.ui.components.TimezonePickerBottomSheet
import com.example.todo_list.utils.TimeFormatHelper
import androidx.fragment.app.FragmentActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    taskList: List<TaskItem>,
    categoriesList: List<TaskListCategory> = TaskListCategory.DEFAULT_CATEGORIES,
    onCreateCategory: (TaskListCategory) -> Unit = {},
    onDeleteCategoryWithMigration: (TaskListCategory, String) -> Unit = { _, _ -> },
    themeMode: Int = 0,
    onThemeModeChange: (Int) -> Unit = {},
    accentColor: AppAccentColor = AppAccentColor.BLUE,
    onAccentColorChange: (AppAccentColor) -> Unit = {},
    onClearCompletedTasks: () -> Unit,
    onResetAllData: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val context = LocalContext.current

    // Reactive Profile state from UserProfileManager
    val userProfile = com.example.todo_list.manager.UserProfileManager.profile
    val userName = userProfile.name
    val userEmail = userProfile.email
    val userTier = userProfile.userTier

    // Categories state
    var showManageCategoriesSheet by remember { mutableStateOf(false) }
    var showCreateCategorySheet by remember { mutableStateOf(false) }
    var categoryPendingDelete by remember { mutableStateOf<TaskListCategory?>(null) }
    var pendingMigrationFromCategory by remember { mutableStateOf<TaskListCategory?>(null) }

    // Preferences & Reminders state
    var notificationsEnabled by remember { mutableStateOf(true) }
    var defaultCategory by remember { mutableStateOf("Personal") }
    var defaultDueTime by remember { mutableStateOf("09:00 AM") }
    var autoArchiveOption by remember { mutableStateOf("After 7 Days") }

    // Customization & Appearance state
    val currentAppIcon = AppIconManager.currentAppIcon
    var compactModeEnabled by remember { mutableStateOf(false) }

    // Security & Data
    var showAppLockSetupSheet by remember { mutableStateOf(false) }
    var appLockSetupStep by remember { mutableStateOf(SetupStep.SELECT_LOCK_TYPE) }
    var showLockTimeoutSheet by remember { mutableStateOf(false) }
    var showEnrollBiometricDialog by remember { mutableStateOf(false) }
    var showChangePasswordSheet by remember { mutableStateOf(false) }

    // Dialog & Modal sheet states
    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showCategoryPickerSheet by remember { mutableStateOf(false) }
    var showTimePickerSheet by remember { mutableStateOf(false) }
    var showAutoArchiveSheet by remember { mutableStateOf(false) }
    var showAppIconSheet by remember { mutableStateOf(false) }
    var showClearCompletedDialog by remember { mutableStateOf(false) }
    var showResetDataDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showStatsSheet by remember { mutableStateOf(false) }
    var showTimezonePickerSheet by remember { mutableStateOf(false) }

    val totalTasks = remember(taskList) { taskList.size }
    val completedCount = remember(taskList) { taskList.count { it.isCompleted } }
    val completionPercentage = remember(totalTasks, completedCount) {
        if (totalTasks > 0) (completedCount.toFloat() / totalTasks * 100).toInt() else 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemGroupedBackground)
    ) {
        // Sticky Glass Header Bar (iOS HIG Large Title)
        SettingsHeaderBar()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Profile / Account Card Header
            item(key = "settings_profile_card") {
                UserProfileCard(
                    userName = userName,
                    userEmail = userEmail,
                    userTier = userTier,
                    accentColor = SystemBlue,
                    onClick = { onOpenProfile() }
                )
            }

            // 2. Lifetime Productivity Statistics Summary Bento Card
            item(key = "settings_productivity_card") {
                ProductivityStatsCard(
                    totalTasks = totalTasks,
                    completedCount = completedCount,
                    completionPercentage = completionPercentage,
                    accentColor = SystemBlue,
                    onViewDetails = { showStatsSheet = true }
                )
            }

            // 3. Preferences & Reminders
            item(key = "settings_preferences_group") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeaderTitle(title = "PREFERENCES & REMINDERS")
                    SettingsGroupCard {
                        SettingsSwitchRow(
                            icon = Icons.Outlined.Notifications,
                            iconTint = SystemBlue,
                            title = "Push Notifications",
                            subtitle = "Task due date reminders",
                            checked = notificationsEnabled,
                            accentColor = SystemBlue,
                            onCheckedChange = {
                                notificationsEnabled = it
                                val msg = if (it) "Notifications enabled" else "Notifications muted"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            showDivider = true
                        )
                        SettingsSwitchRow(
                            icon = Icons.Outlined.Vibration,
                            iconTint = AppleHealth,
                            title = "Haptic Feedback",
                            subtitle = if (HapticManager.isHapticsEnabled) {
                                "Tactile vibration active on all buttons"
                            } else {
                                "Off — all vibrations muted"
                            },
                            checked = HapticManager.isHapticsEnabled,
                            accentColor = SystemBlue,
                            onCheckedChange = { isEnabled ->
                                HapticManager.updateHapticsEnabled(isEnabled, context)
                                if (isEnabled) {
                                    HapticManager.performClick(context)
                                    Toast.makeText(context, "Haptic feedback enabled", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Haptic feedback disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            showDivider = true
                        )

                        if (HapticManager.isHapticsEnabled) {
                            SettingsSegmentedRow(
                                icon = Icons.Outlined.GraphicEq,
                                iconTint = AppleHealth,
                                title = "Vibration Strength",
                                options = listOf("Light", "Medium", "Strong"),
                                selectedIndex = when (HapticManager.hapticIntensity) {
                                    HapticIntensity.LIGHT -> 0
                                    HapticIntensity.MEDIUM -> 1
                                    HapticIntensity.STRONG -> 2
                                },
                                accentColor = SystemBlue,
                                onOptionSelected = { idx ->
                                    val newIntensity = when (idx) {
                                        0 -> HapticIntensity.LIGHT
                                        1 -> HapticIntensity.MEDIUM
                                        else -> HapticIntensity.STRONG
                                    }
                                    HapticManager.updateHapticIntensity(newIntensity, context)
                                },
                                showDivider = true
                            )
                        }

                        SettingsValueRow(
                            icon = Icons.Outlined.Category,
                            iconTint = AppleStudy,
                            title = "Default Category",
                            value = defaultCategory,
                            onClick = { showCategoryPickerSheet = true },
                            showDivider = true
                        )
                        SettingsValueRow(
                            icon = Icons.Outlined.AccessTime,
                            iconTint = SystemBlue,
                            title = "Default Due Time",
                            value = TimeFormatHelper.formatTimeForDisplay(defaultDueTime, TimePreferencesManager.is24HourFormat),
                            onClick = { showTimePickerSheet = true },
                            showDivider = true
                        )
                        SettingsValueRow(
                            icon = Icons.Outlined.Archive,
                            iconTint = SystemGray,
                            title = "Auto-Archive Completed",
                            value = autoArchiveOption,
                            onClick = { showAutoArchiveSheet = true },
                            showDivider = false
                        )
                    }
                }
            }

            // Date, Time & Region Settings Group
            item(key = "settings_time_region_group") {
                val currentLoc = TimePreferencesManager.selectedLocation
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeaderTitle(title = "DATE, TIME & REGION")
                    SettingsGroupCard {
                        // 12-Hour vs 24-Hour Format Switcher
                        SettingsSegmentedRow(
                            icon = Icons.Outlined.Schedule,
                            iconTint = SystemBlue,
                            title = "Time Format",
                            options = listOf("12-Hour", "24-Hour"),
                            selectedIndex = if (TimePreferencesManager.is24HourFormat) 1 else 0,
                            accentColor = SystemBlue,
                            onOptionSelected = { idx ->
                                TimePreferencesManager.setTimeFormat(idx == 1)
                            },
                            showDivider = true
                        )

                        // Set Automatically (Time Zone Detection)
                        SettingsSwitchRow(
                            icon = Icons.Outlined.Public,
                            iconTint = AppleHealth,
                            title = "Set Automatically",
                            subtitle = if (TimePreferencesManager.isAutoDetectEnabled) {
                                "Using device location: ${currentLoc.displayLocation}"
                            } else {
                                "Manual time zone selection active"
                            },
                            checked = TimePreferencesManager.isAutoDetectEnabled,
                            accentColor = SystemBlue,
                            onCheckedChange = { isEnabled ->
                                TimePreferencesManager.setAutoDetectEnabled(isEnabled, context)
                                val msg = if (isEnabled) "Auto-detection enabled" else "Manual time zone mode active"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            showDivider = true
                        )

                        // Region & City Selection Row
                        SettingsValueRow(
                            icon = Icons.Outlined.LocationOn,
                            iconTint = AppleStudy,
                            title = "Region & City",
                            value = "${currentLoc.flagEmoji} ${currentLoc.displayLocation}",
                            onClick = { showTimezonePickerSheet = true },
                            showDivider = true
                        )

                        // Time Zone details
                        val tzDisplay = if (currentLoc.timeZoneAbbr.startsWith("GMT", ignoreCase = true) ||
                            currentLoc.timeZoneAbbr.startsWith("UTC", ignoreCase = true) ||
                            currentLoc.timeZoneAbbr.isBlank()
                        ) {
                            currentLoc.utcOffsetStr
                        } else {
                            "${currentLoc.timeZoneAbbr} (${currentLoc.utcOffsetStr})"
                        }
                        SettingsValueRow(
                            icon = Icons.Outlined.Language,
                            iconTint = SystemBlue,
                            title = "Time Zone",
                            value = tzDisplay,
                            onClick = { showTimezonePickerSheet = true },
                            showDivider = true
                        )

                        // Live World Clock Inset Card
                        TimePreviewWidgetRow(
                            location = currentLoc,
                            is24Hour = TimePreferencesManager.is24HourFormat
                        )
                    }
                }
            }

            // 4. Categories & Lists Management
            item(key = "settings_categories_group") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeaderTitle(title = "CATEGORIES & LISTS")
                    SettingsGroupCard {
                        SettingsValueRow(
                            icon = Icons.Outlined.Category,
                            iconTint = SystemBlue,
                            title = "Manage Categories",
                            value = "${categoriesList.size} categories",
                            onClick = { showManageCategoriesSheet = true },
                            showDivider = true
                        )
                        SettingsActionRow(
                            icon = Icons.Outlined.AddCircleOutline,
                            iconTint = AppleHealth,
                            title = "Create New Category",
                            badgeText = "+ Add",
                            enabled = true,
                            onClick = { showCreateCategorySheet = true },
                            showDivider = false
                        )
                    }
                }
            }

            // 5. Customization & Theme
            item(key = "settings_customization_group") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeaderTitle(title = "APPEARANCE & CUSTOMIZATION")
                    SettingsGroupCard {
                        SettingsSegmentedRow(
                            icon = Icons.Outlined.Contrast,
                            iconTint = SystemBlue,
                            title = "Appearance",
                            options = listOf("System", "Light", "Dark"),
                            selectedIndex = themeMode,
                            accentColor = SystemBlue,
                            onOptionSelected = onThemeModeChange,
                            showDivider = true
                        )

                        // Real-time Accent Color Swatches
                        AccentColorSwatchRow(
                            selectedAccent = accentColor,
                            onAccentSelected = onAccentColorChange,
                            showDivider = true
                        )

                        SettingsValueRow(
                            icon = Icons.Outlined.AppShortcut,
                            iconTint = ApplePersonal,
                            title = "App Icon",
                            value = currentAppIcon.displayName,
                            onClick = { showAppIconSheet = true },
                            showDivider = true
                        )

                        SettingsSwitchRow(
                            icon = Icons.Outlined.DensityMedium,
                            iconTint = AppleStudy,
                            title = "Compact Task Density",
                            subtitle = "Fit more tasks per screen",
                            checked = compactModeEnabled,
                            accentColor = SystemBlue,
                            onCheckedChange = { compactModeEnabled = it },
                            showDivider = false
                        )
                    }
                }
            }

            // TaskFlow Intelligence & Small LLM Settings Group
            item(key = "settings_ai_intelligence_group") {
                var apiKeyText by remember { mutableStateOf(AiConfigurationManager.apiKey) }
                var isApiKeyVisible by remember { mutableStateOf(false) }
                var testStatusMessage by remember { mutableStateOf<String?>(null) }
                var isTestingConnection by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeaderTitle(title = "APPLE INTELLIGENCE & LLM")
                    SettingsGroupCard {
                        // Model Selector Segmented Row
                        SettingsSegmentedRow(
                            icon = Icons.Default.AutoAwesome,
                            iconTint = SystemBlue,
                            title = "AI Engine",
                            options = listOf("Gemini 1.5", "Gemini 2.0", "On-Device"),
                            selectedIndex = when (AiConfigurationManager.selectedModel) {
                                AiConfigurationManager.MODEL_GEMINI_1_5_FLASH -> 0
                                AiConfigurationManager.MODEL_GEMINI_2_0_FLASH -> 1
                                else -> 2
                            },
                            accentColor = SystemBlue,
                            onOptionSelected = { idx ->
                                val newModel = when (idx) {
                                    0 -> AiConfigurationManager.MODEL_GEMINI_1_5_FLASH
                                    1 -> AiConfigurationManager.MODEL_GEMINI_2_0_FLASH
                                    else -> AiConfigurationManager.MODEL_LOCAL_HEURISTIC
                                }
                                AiConfigurationManager.selectedModel = newModel
                                testStatusMessage = null
                            },
                            showDivider = true
                        )

                        // API Key Input Row (if Gemini is selected)
                        if (AiConfigurationManager.selectedModel != AiConfigurationManager.MODEL_LOCAL_HEURISTIC) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Google Gemini API Key",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SystemLabelPrimary
                                    )
                                    Text(
                                        text = if (isApiKeyVisible) "Hide" else "Show",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SystemBlue,
                                        modifier = Modifier.clickable { isApiKeyVisible = !isApiKeyVisible }
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SystemSurfaceSecondary,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = apiKeyText,
                                            onValueChange = {
                                                apiKeyText = it
                                                AiConfigurationManager.apiKey = it
                                                testStatusMessage = null
                                            },
                                            singleLine = true,
                                            visualTransformation = if (isApiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                            textStyle = TextStyle(fontSize = 14.sp, color = SystemLabelPrimary),
                                            modifier = Modifier.weight(1f),
                                            decorationBox = { innerTextField ->
                                                if (apiKeyText.isEmpty()) {
                                                    Text(
                                                        text = "Paste Gemini API key from AI Studio",
                                                        fontSize = 13.sp,
                                                        color = SystemLabelSecondary
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Test Connection Button & Status Banner
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            isTestingConnection = true
                                            testStatusMessage = "Verifying credentials..."
                                            scope.launch {
                                                val res = AiConfigurationManager.testConnection(apiKeyText)
                                                isTestingConnection = false
                                                testStatusMessage = res.getOrElse { it.message ?: "Failed" }
                                            }
                                        },
                                        enabled = apiKeyText.isNotBlank() && !isTestingConnection
                                    ) {
                                        Text(
                                            text = if (isTestingConnection) "Verifying..." else "Test Connection",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SystemBlue
                                        )
                                    }

                                    testStatusMessage?.let { msg ->
                                        val isOk = msg.startsWith("Connection verified")
                                        Text(
                                            text = if (isOk) "✓ Active" else "✗ Error",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOk) SystemGreen else SystemRed
                                        )
                                    }
                                }

                                testStatusMessage?.let { msg ->
                                    Text(
                                        text = msg,
                                        fontSize = 12.sp,
                                        color = if (msg.startsWith("Connection verified")) SystemGreen else SystemRed,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = SystemDivider,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }

                        // Offline Heuristic Fallback Toggle
                        SettingsSwitchRow(
                            icon = Icons.Outlined.CloudOff,
                            iconTint = AppleHealth,
                            title = "Offline Fallback Engine",
                            subtitle = "Use local optimizer when offline or unconfigured",
                            checked = AiConfigurationManager.isHeuristicFallbackEnabled,
                            accentColor = SystemBlue,
                            onCheckedChange = { isChecked ->
                                AiConfigurationManager.isHeuristicFallbackEnabled = isChecked
                            },
                            showDivider = true
                        )

                        // Anti-Hallucination & Privacy Notice
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(
                                text = "🔒 Zero-Hallucination & Privacy Guarantee",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemLabelPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "TaskFlow Intelligence answers strictly based on your SQLite task data with mathematical task ID cross-validation. No phantom tasks or data leakage.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = SystemLabelSecondary
                            )
                        }
                    }
                }
            }

            // 5. Security & App Lock
            item(key = "settings_security_group") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeaderTitle(title = "SECURITY & APP LOCK")
                    SettingsGroupCard {
                        SettingsSwitchRow(
                            icon = Icons.Outlined.Lock,
                            iconTint = AppleHealth,
                            title = "App Lock",
                            subtitle = if (AppLockManager.isLockEnabled) {
                                "${AppLockManager.currentLockType.displayName} active"
                            } else {
                                "Protect app with PIN, Pattern or Password"
                            },
                            checked = AppLockManager.isLockEnabled,
                            accentColor = SystemBlue,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    appLockSetupStep = SetupStep.SELECT_LOCK_TYPE
                                    showAppLockSetupSheet = true
                                } else {
                                    appLockSetupStep = SetupStep.VERIFY_CURRENT_FOR_DISABLE
                                    showAppLockSetupSheet = true
                                }
                            },
                            showDivider = AppLockManager.isLockEnabled
                        )

                        if (AppLockManager.isLockEnabled) {
                            val bioDisplayName = BiometricAuthHelper.getBiometricDisplayName(context)
                            val bioIcon = BiometricAuthHelper.getBiometricIcon(context)
                            SettingsSwitchRow(
                                icon = bioIcon,
                                iconTint = AppleHealth,
                                title = "Unlock with $bioDisplayName",
                                subtitle = BiometricAuthHelper.getBiometricSettingsSubtitle(context, AppLockManager.isBiometricEnabled),
                                checked = AppLockManager.isBiometricEnabled,
                                accentColor = SystemBlue,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        val status = BiometricAuthHelper.checkBiometricAvailability(context)
                                        when (status) {
                                            BiometricAvailability.AVAILABLE -> {
                                                val activity = context as? FragmentActivity
                                                if (activity != null) {
                                                    BiometricAuthHelper.promptBiometric(
                                                        activity = activity,
                                                        title = "Set Up $bioDisplayName",
                                                        subtitle = BiometricAuthHelper.getBiometricSetupSubtitle(context),
                                                        negativeButtonText = "Cancel",
                                                        onSuccess = {
                                                            AppLockManager.setBiometricAuth(true)
                                                            Toast.makeText(context, "$bioDisplayName unlock enabled", Toast.LENGTH_SHORT).show()
                                                        },
                                                        onError = { err ->
                                                            Toast.makeText(context, "Setup canceled: $err", Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                } else {
                                                    AppLockManager.setBiometricAuth(true)
                                                }
                                            }
                                            BiometricAvailability.NOT_ENROLLED -> {
                                                showEnrollBiometricDialog = true
                                            }
                                            BiometricAvailability.NO_HARDWARE -> {
                                                Toast.makeText(context, "No biometric hardware found on this device", Toast.LENGTH_SHORT).show()
                                            }
                                            BiometricAvailability.HW_UNAVAILABLE -> {
                                                Toast.makeText(context, "Biometric sensor is currently unavailable", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        AppLockManager.setBiometricAuth(false)
                                        Toast.makeText(context, "$bioDisplayName unlock disabled", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                showDivider = true
                            )

                            SettingsValueRow(
                                icon = Icons.Outlined.Fingerprint,
                                iconTint = AppleHealth,
                                title = "Device Biometrics",
                                value = "System Settings",
                                onClick = {
                                    BiometricAuthHelper.openEnrollmentSettings(context)
                                },
                                showDivider = true
                            )

                            SettingsValueRow(
                                icon = Icons.Outlined.Password,
                                iconTint = SystemBlue,
                                title = "Change Lock",
                                value = AppLockManager.currentLockType.displayName,
                                onClick = {
                                    appLockSetupStep = SetupStep.VERIFY_CURRENT_FOR_CHANGE
                                    showAppLockSetupSheet = true
                                },
                                showDivider = true
                            )

                            val timeoutLabel = when (AppLockManager.lockTimeoutMs) {
                                0L -> "Immediately"
                                60_000L -> "After 1 minute"
                                300_000L -> "After 5 minutes"
                                900_000L -> "After 15 minutes"
                                else -> "Immediately"
                            }

                            SettingsValueRow(
                                icon = Icons.Outlined.Schedule,
                                iconTint = AppleStudy,
                                title = "Require Lock",
                                value = timeoutLabel,
                                onClick = { showLockTimeoutSheet = true },
                                showDivider = false
                            )
                        }
                    }

                    if (AppLockManager.isLockEnabled && AppLockManager.isBiometricEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SystemSurface.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = SystemBlue,
                                    modifier = Modifier.size(18.dp).padding(top = 1.dp)
                                )
                                Text(
                                    text = "Biometrics verify against fingerprints and Face ID registered in this device's Android Settings. If sharing this device, enroll additional fingers in System Settings, or protect your account with an App PIN.",
                                    fontSize = 13.sp,
                                    color = SystemLabelSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // 6. Data Management
            item(key = "settings_data_group") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeaderTitle(title = "DATA MANAGEMENT")
                    SettingsGroupCard {
                        SettingsActionRow(
                            icon = Icons.Outlined.DeleteSweep,
                            iconTint = SystemBlue,
                            title = "Clear Completed Tasks",
                            badgeText = if (completedCount > 0) "$completedCount completed" else "None",
                            enabled = completedCount > 0,
                            onClick = { showClearCompletedDialog = true },
                            showDivider = false
                        )
                    }
                }
            }

            // 7. Account & Session Management
            item(key = "settings_account_session_group") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeaderTitle(title = "ACCOUNT & SESSION")
                    SettingsGroupCard {
                        SettingsValueRow(
                            icon = Icons.Outlined.PersonOutline,
                            iconTint = SystemBlue,
                            title = "Signed In As",
                            value = com.example.todo_list.security.AuthManager.registeredUsername.ifEmpty { userProfile.username },
                            onClick = { onOpenProfile() },
                            showDivider = true
                        )
                        SettingsActionRow(
                            icon = Icons.Outlined.LockReset,
                            iconTint = SystemBlue,
                            title = "Change Account Password",
                            badgeText = "2FA Gate",
                            onClick = { showChangePasswordSheet = true },
                            showDivider = true
                        )
                        SettingsDestructiveRow(
                            icon = Icons.Outlined.Logout,
                            title = "Log Out of TaskFlow",
                            onClick = { showLogoutDialog = true }
                        )
                    }
                }
            }

            // 8. Destructive Actions
            item(key = "settings_danger_group") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeaderTitle(title = "DANGER ZONE")
                    SettingsGroupCard {
                        SettingsDestructiveRow(
                            icon = Icons.Outlined.DeleteForever,
                            title = "Reset All Task Data",
                            onClick = { showResetDataDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet: Time Zone & Region Picker
    if (showTimezonePickerSheet) {
        TimezonePickerBottomSheet(
            onDismiss = { showTimezonePickerSheet = false },
            onLocationSelected = { loc ->
                Toast.makeText(context, "Location set to ${loc.displayLocation}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal Sheet: Change Password Security Sheet
    if (showChangePasswordSheet) {
        com.example.todo_list.ui.screens.profile.ChangePasswordSecuritySheet(
            onDismiss = { showChangePasswordSheet = false },
            onPasswordChanged = {
                showChangePasswordSheet = false
                Toast.makeText(context, "Account password updated securely! 🛡️", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal Sheet 1: Edit Profile Sheet
    if (showEditProfileSheet) {
        com.example.todo_list.ui.screens.profile.EditProfileBottomSheet(
            profile = userProfile,
            onDismiss = { showEditProfileSheet = false },
            onProfileSaved = {}
        )
    }

    // Modal Sheet 2: Select Default Category
    if (showCategoryPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryPickerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SystemSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select Default Category",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoriesList, key = { it.id }) { cat ->
                        val isSelected = defaultCategory.equals(cat.name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SystemBlueLight else SystemGroupedBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    defaultCategory = cat.name
                                    showCategoryPickerSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(cat.getEmoji(), fontSize = 16.sp)
                                    Text(
                                        text = cat.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) SystemBlue else SystemLabelPrimary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = SystemBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal Sheet: Manage Categories
    if (showManageCategoriesSheet) {
        ManageCategoriesBottomSheet(
            categoriesList = categoriesList,
            taskList = taskList,
            onDismiss = { showManageCategoriesSheet = false },
            onCreateCategoryClick = {
                showCreateCategorySheet = true
            },
            onDeleteCategory = { categoryToDelete ->
                categoryPendingDelete = categoryToDelete
            }
        )
    }

    // Modal Sheet for Creating Custom Category
    if (showCreateCategorySheet) {
        CreateCategoryBottomSheet(
            onDismiss = {
                showCreateCategorySheet = false
                pendingMigrationFromCategory = null
            },
            onCreateCategory = { newCategory ->
                onCreateCategory(newCategory)
                showCreateCategorySheet = false
                // If this was triggered to create a migration destination:
                pendingMigrationFromCategory?.let { fromCat ->
                    onDeleteCategoryWithMigration(fromCat, newCategory.name)
                    pendingMigrationFromCategory = null
                }
            }
        )
    }

    // Modal Dialog for Category Deletion & Task Migration
    categoryPendingDelete?.let { catToDelete ->
        val affectedCount = taskList.count { it.category.equals(catToDelete.name, ignoreCase = true) }
        val remainingCategories = categoriesList.filter { it.id != catToDelete.id }

        DeleteCategoryMigrationDialog(
            categoryToDelete = catToDelete,
            affectedTasksCount = affectedCount,
            availableCategories = remainingCategories,
            onDismiss = { categoryPendingDelete = null },
            onConfirmDeleteAndMigrate = { targetCategoryName ->
                onDeleteCategoryWithMigration(catToDelete, targetCategoryName)
                categoryPendingDelete = null
            },
            onRequestCreateNewCategory = {
                pendingMigrationFromCategory = catToDelete
                categoryPendingDelete = null
                showCreateCategorySheet = true
            }
        )
    }

    // Modal Sheet 3: Select Default Due Time
    if (showTimePickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTimePickerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SystemSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select Default Due Time",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )

                val is24H = TimePreferencesManager.is24HourFormat
                val dueTimeOptions = if (is24H) {
                    listOf("08:00", "09:00", "10:00", "14:00", "18:00")
                } else {
                    listOf("08:00 AM", "09:00 AM", "10:00 AM", "02:00 PM", "06:00 PM")
                }
                val formattedCurrentDueTime = TimeFormatHelper.formatTimeForDisplay(defaultDueTime, is24H)

                dueTimeOptions.forEach { timeOption ->
                    val isSelected = formattedCurrentDueTime == timeOption
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) SystemBlueLight else SystemGroupedBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                defaultDueTime = timeOption
                                showTimePickerSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeOption,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) SystemBlue else SystemLabelPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Selected",
                                    tint = SystemBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal Sheet 4: Auto Archive Option
    if (showAutoArchiveSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAutoArchiveSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SystemSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Auto-Archive Completed Tasks",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )

                listOf("Never", "Instantly", "At Midnight", "After 7 Days").forEach { option ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (autoArchiveOption == option) SystemBlueLight else SystemGroupedBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                autoArchiveOption = option
                                showAutoArchiveSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (autoArchiveOption == option) SystemBlue else SystemLabelPrimary
                            )
                            if (autoArchiveOption == option) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Selected",
                                    tint = SystemBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal Sheet 5: App Icon Picker
    if (showAppIconSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAppIconSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SystemSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "App Icon",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemLabelPrimary
                    )
                    Text(
                        text = "Select an authentic Apple-styled launcher icon for your home screen:",
                        fontSize = 14.sp,
                        color = SystemLabelSecondary
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppIcon.entries.forEach { appIcon ->
                        val isSelected = currentAppIcon == appIcon
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) SystemBlueLight else SystemGroupedBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) SystemBlue else SystemDivider
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val success = AppIconManager.setAppIcon(context, appIcon)
                                    HapticManager.performClick(context)
                                    showAppIconSheet = false
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            "App icon updated to ${appIcon.displayName}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Failed to update app icon",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AppIconPreviewBadge(appIcon = appIcon)
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = appIcon.displayName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) SystemBlue else SystemLabelPrimary
                                        )
                                        Text(
                                            text = appIcon.subtitle,
                                            fontSize = 13.sp,
                                            color = SystemLabelSecondary
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SystemBlue,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val isSamsung = remember {
                    android.os.Build.MANUFACTURER.contains("samsung", ignoreCase = true)
                }
                if (isSamsung) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SystemGroupedBackground,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = SystemBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Samsung One UI Tip",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SystemLabelPrimary
                                )
                            }
                            Text(
                                text = "To ensure notifications display your active custom icon on Samsung Galaxy devices, turn off 'Show app icon in notifications' in device settings.",
                                fontSize = 12.sp,
                                color = SystemLabelSecondary,
                                lineHeight = 16.sp
                            )
                            TextButton(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent("android.settings.NOTIFICATION_SETTINGS").apply {
                                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Open Notification Settings →",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SystemBlue
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Confirmation Dialog: Clear Completed Tasks
    if (showClearCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showClearCompletedDialog = false },
            title = {
                Text(
                    text = "Clear Completed Tasks?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text("Are you sure you want to remove all $completedCount completed tasks from TaskFlow?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearCompletedTasks()
                        showClearCompletedDialog = false
                        Toast.makeText(context, "Cleared $completedCount completed tasks", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear Tasks", color = SystemRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCompletedDialog = false }) {
                    Text("Cancel", color = SystemBlue)
                }
            },
            containerColor = SystemSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Confirmation Dialog: Reset All Data
    if (showResetDataDialog) {
        AlertDialog(
            onDismissRequest = { showResetDataDialog = false },
            title = {
                Text(
                    text = "Reset All Task Data?",
                    fontWeight = FontWeight.Bold,
                    color = SystemRed,
                    fontSize = 18.sp
                )
            },
            text = {
                Text("This action cannot be undone. All tasks, categories, and reminders will be permanently deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetAllData()
                        showResetDataDialog = false
                        Toast.makeText(context, "All task data has been reset", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset All", color = SystemRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDataDialog = false }) {
                    Text("Cancel", color = SystemBlue)
                }
            },
            containerColor = SystemSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Confirmation Dialog: Log Out
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Log Out of TaskFlow?",
                    fontWeight = FontWeight.Bold,
                    color = SystemRed,
                    fontSize = 18.sp
                )
            },
            text = {
                Text("You will need to enter your username and password to log in again.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        com.example.todo_list.security.AuthManager.logout()
                        HapticManager.performWarning(context)
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Log Out", color = SystemRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = SystemBlue)
                }
            },
            containerColor = SystemSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Sheet: Require Lock Timeout Picker
    if (showLockTimeoutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLockTimeoutSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SystemSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Require Lock",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )
                Text(
                    text = "Choose how quickly TaskFlow locks when moved to background or recent apps:",
                    fontSize = 14.sp,
                    color = SystemLabelSecondary
                )

                val timeoutOptions = listOf(
                    0L to "Immediately",
                    60_000L to "After 1 minute",
                    300_000L to "After 5 minutes",
                    900_000L to "After 15 minutes"
                )

                timeoutOptions.forEach { (ms, label) ->
                    val isSelected = AppLockManager.lockTimeoutMs == ms
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) SystemBlueLight else SystemGroupedBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AppLockManager.setTimeout(ms)
                                showLockTimeoutSheet = false
                                Toast.makeText(context, "Lock timeout set to $label", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) SystemBlue else SystemLabelPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Selected",
                                    tint = SystemBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal Sheet: App Lock Setup Flow
    if (showAppLockSetupSheet) {
        AppLockSetupSheet(
            initialStep = appLockSetupStep,
            onDismiss = { showAppLockSetupSheet = false },
            onLockSetupComplete = {
                showAppLockSetupSheet = false
            }
        )
    }

    // Dialog: Prompt to enroll biometric in device settings
    if (showEnrollBiometricDialog) {
        AlertDialog(
            onDismissRequest = { showEnrollBiometricDialog = false },
            title = {
                Text(
                    text = BiometricAuthHelper.getBiometricEnrollmentTitle(context),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SystemLabelPrimary
                )
            },
            text = {
                Text(
                    text = BiometricAuthHelper.getBiometricEnrollmentMessage(context),
                    fontSize = 14.sp,
                    color = SystemLabelSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEnrollBiometricDialog = false
                        BiometricAuthHelper.openEnrollmentSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Open Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnrollBiometricDialog = false }) {
                    Text("Cancel", color = SystemBlue)
                }
            },
            containerColor = SystemSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SettingsHeaderBar() {
    Surface(
        color = SystemSurface.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Settings",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
fun UserProfileCard(
    userName: String,
    userEmail: String,
    userTier: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SystemSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AsyncImage(
                model = com.example.todo_list.manager.UserProfileManager.getAvatarUrl(),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, accentColor, CircleShape)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = userName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemLabelPrimary
                    )
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "PRO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = userEmail,
                    fontSize = 14.sp,
                    color = SystemLabelSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Profile Details",
                tint = SystemGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProductivityStatsCard(
    totalTasks: Int,
    completedCount: Int,
    completionPercentage: Int,
    accentColor: Color,
    onViewDetails: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SystemSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "PRODUCTIVITY OVERVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary
                )
                Text(
                    text = "$completedCount of $totalTasks Tasks Done",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )
                Text(
                    text = "Current streak: 7 Days 🔥",
                    fontSize = 13.sp,
                    color = accentColor
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f))
            ) {
                Text(
                    text = "$completionPercentage%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
fun SectionHeaderTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = SystemLabelSecondary,
        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
    )
}

@Composable
fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SystemSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun SettingsRowIcon(icon: ImageVector, iconTint: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(iconTint.copy(alpha = 0.15f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    accentColor: Color = Color.Unspecified,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    val activeAccent = if (accentColor != Color.Unspecified) accentColor else SystemBlue
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsRowIcon(icon = icon, iconTint = iconTint)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = SystemLabelPrimary
                )
                subtitle?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = SystemLabelSecondary
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = activeAccent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = SystemGray2
                )
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = SystemDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 56.dp)
            )
        }
    }
}

@Composable
fun SettingsValueRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    onClick: () -> Unit,
    showChevron: Boolean = true,
    showDivider: Boolean = true
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    HapticManager.performClick(context)
                    onClick()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsRowIcon(icon = icon, iconTint = iconTint)

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = SystemLabelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = value,
                fontSize = 15.sp,
                color = SystemLabelSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )

            if (showChevron) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SystemGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = SystemDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 56.dp)
            )
        }
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    badgeText: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) {
                    HapticManager.performClick(context)
                    onClick()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsRowIcon(icon = icon, iconTint = if (enabled) iconTint else SystemGray)

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) SystemLabelPrimary else SystemLabelSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            badgeText?.let {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = iconTint.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iconTint,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SystemGray,
                modifier = Modifier.size(18.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = SystemDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 56.dp)
            )
        }
    }
}

@Composable
fun SettingsSegmentedRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    accentColor: Color = Color.Unspecified,
    onOptionSelected: (Int) -> Unit,
    showDivider: Boolean = true,
    isStacked: Boolean = true
) {
    val context = LocalContext.current
    val activeAccent = if (accentColor != Color.Unspecified) accentColor else SystemBlue
    val isDark = LocalTaskFlowColors.current.isDark

    val trackColor = if (isDark) Color(0x3D767680) else SystemGroupedBackground
    val selectedPillColor = if (isDark) Color(0xFF636366) else SystemSurface

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isStacked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row: Icon + Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsRowIcon(icon = icon, iconTint = iconTint)

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = SystemLabelPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Full-width iOS Segmented Control
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = trackColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.5.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        options.forEachIndexed { index, option ->
                            val isSelected = index == selectedIndex
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) selectedPillColor else Color.Transparent,
                                shadowElevation = if (isSelected && !isDark) 1.5.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        onOptionSelected(index)
                                        HapticManager.performClick(context)
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) activeAccent else SystemLabelSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsRowIcon(icon = icon, iconTint = iconTint)

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = SystemLabelPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // iOS Segmented Control
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = trackColor,
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        options.forEachIndexed { index, option ->
                            val isSelected = index == selectedIndex
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) selectedPillColor else Color.Transparent,
                                shadowElevation = if (isSelected && !isDark) 1.dp else 0.dp,
                                modifier = Modifier
                                    .clickable {
                                        onOptionSelected(index)
                                        HapticManager.performClick(context)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeAccent else SystemLabelSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = SystemDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 56.dp)
            )
        }
    }
}

@Composable
fun AccentColorSwatchRow(
    selectedAccent: AppAccentColor,
    onAccentSelected: (AppAccentColor) -> Unit,
    showDivider: Boolean = true
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsRowIcon(icon = Icons.Outlined.Palette, iconTint = SystemBlue)

            Text(
                text = "Accent Color",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = SystemLabelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppAccentColor.entries.forEach { accent ->
                    val isSelected = selectedAccent == accent
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(accent.lightColor)
                            .border(
                                width = if (isSelected) 2.dp else 0.5.dp,
                                color = if (isSelected) SystemLabelPrimary else SystemDivider,
                                shape = CircleShape
                            )
                            .clickable {
                                HapticManager.performClick(context)
                                onAccentSelected(accent)
                            }
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = SystemDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 56.dp)
            )
        }
    }
}

@Composable
fun SettingsDestructiveRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                HapticManager.performWarning(context)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsRowIcon(icon = icon, iconTint = SystemRed)

        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = SystemRed,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = SystemRed,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesBottomSheet(
    categoriesList: List<TaskListCategory>,
    taskList: List<TaskItem>,
    onDismiss: () -> Unit,
    onCreateCategoryClick: () -> Unit,
    onDeleteCategory: (TaskListCategory) -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemGroupedBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Manage Categories",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemLabelPrimary
                    )
                    Text(
                        text = "${categoriesList.size} categories available",
                        fontSize = 13.sp,
                        color = SystemLabelSecondary
                    )
                }

                Button(
                    onClick = {
                        HapticManager.performClick(context)
                        onCreateCategoryClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SystemSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(categoriesList, key = { it.id }) { cat ->
                        val count = taskList.count { it.category.equals(cat.name, ignoreCase = true) }
                        val catColor = cat.getColor()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat.iconName, cat.name),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = cat.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SystemLabelPrimary
                                    )
                                    Text(
                                        text = "$count task${if (count != 1) "s" else ""}",
                                        fontSize = 12.sp,
                                        color = SystemLabelSecondary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (cat.isSystemDefault) {
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = SystemGray5
                                    ) {
                                        Text(
                                            text = "Default",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SystemLabelSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            HapticManager.performClick(context)
                                            onDeleteCategory(cat)
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(SystemRed.copy(alpha = 0.1f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete ${cat.name}",
                                            tint = SystemRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            color = SystemDivider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 56.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimePreviewWidgetRow(
    location: WorldLocation,
    is24Hour: Boolean
) {
    var currentTimeStr by remember(location.timeZoneId, is24Hour) {
        mutableStateOf(TimeFormatHelper.getFormattedCurrentTime(location.timeZoneId, is24Hour))
    }

    LaunchedEffect(location.timeZoneId, is24Hour) {
        while (true) {
            currentTimeStr = TimeFormatHelper.getFormattedCurrentTime(location.timeZoneId, is24Hour)
            kotlinx.coroutines.delay(1000)
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SystemGroupedBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "LIVE WORLD CLOCK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${location.flagEmoji} ${location.displayLocation}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SystemLabelPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SystemBlueLight
            ) {
                Text(
                    text = currentTimeStr,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Authentic Apple iOS squircle preview badge for launcher icon styles.
 */
@Composable
fun AppIconPreviewBadge(
    appIcon: AppIcon,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(shape)
            .then(
                when (appIcon) {
                    AppIcon.CLASSIC -> Modifier.background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFF0088FF), Color(0xFF0055D4))
                        )
                    )
                    AppIcon.DARK -> Modifier.background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFF2C2C2E), Color(0xFF121214))
                        )
                    )
                    AppIcon.NEON -> Modifier.background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFF0D1326), Color(0xFF050811))
                        )
                    )
                    AppIcon.GLASS -> Modifier.background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFF6C5CE7), Color(0xFFFD79A8), Color(0xFF74B9FF))
                        )
                    )
                    AppIcon.SUNSET -> Modifier.background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFFFF5E3A), Color(0xFFFF2A68))
                        )
                    )
                    AppIcon.EMERALD -> Modifier.background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFF34C759), Color(0xFF00A86B))
                        )
                    )
                    AppIcon.PURPLE -> Modifier.background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFFAF52DE), Color(0xFF5856D6))
                        )
                    )
                    AppIcon.GOLD -> Modifier.background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFF2A241B), Color(0xFF12100E))
                        )
                    )
                }
            )
            .border(
                width = 0.5.dp,
                color = when (appIcon) {
                    AppIcon.CLASSIC -> Color(0x33FFFFFF)
                    AppIcon.DARK -> Color(0x33FFFFFF)
                    AppIcon.NEON -> Color(0x4400F0FF)
                    AppIcon.GLASS -> Color(0x66FFFFFF)
                    AppIcon.SUNSET -> Color(0x33FFFFFF)
                    AppIcon.EMERALD -> Color(0x33FFFFFF)
                    AppIcon.PURPLE -> Color(0x33FFFFFF)
                    AppIcon.GOLD -> Color(0x55F6D365)
                },
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        when (appIcon) {
            AppIcon.CLASSIC, AppIcon.SUNSET, AppIcon.EMERALD, AppIcon.PURPLE -> {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            AppIcon.DARK -> {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .border(2.dp, Color(0xFF8E8E93), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFF2F2F7),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            AppIcon.NEON -> {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0x2000F0FF))
                        .border(2.dp, Color(0xFF00F0FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            AppIcon.GLASS -> {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0x55FFFFFF))
                        .border(1.5.dp, Color(0xCCFFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            AppIcon.GOLD -> {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0x22F6D365))
                        .border(2.dp, Color(0xFFF6D365), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFFFF8E7),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}



