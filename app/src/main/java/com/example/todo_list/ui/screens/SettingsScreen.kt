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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.todo_list.model.TaskItem
import com.example.todo_list.security.AppLockManager
import com.example.todo_list.security.BiometricAuthHelper
import com.example.todo_list.security.BiometricAvailability
import com.example.todo_list.security.LockType
import com.example.todo_list.ui.screens.lock.AppLockSetupSheet
import com.example.todo_list.ui.screens.lock.SetupStep
import com.example.todo_list.ui.theme.*
import androidx.fragment.app.FragmentActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    taskList: List<TaskItem>,
    themeMode: Int = 0,
    onThemeModeChange: (Int) -> Unit = {},
    accentColor: AppAccentColor = AppAccentColor.BLUE,
    onAccentColorChange: (AppAccentColor) -> Unit = {},
    onClearCompletedTasks: () -> Unit,
    onResetAllData: () -> Unit
) {
    val context = LocalContext.current

    // Account state
    var userName by remember { mutableStateOf("Prerak Pithadiya") }
    var userEmail by remember { mutableStateOf("prerak@taskflow.app") }
    var userTier by remember { mutableStateOf("TaskFlow Pro") }

    // Preferences & Reminders state
    var notificationsEnabled by remember { mutableStateOf(true) }
    var dailyDigestEnabled by remember { mutableStateOf(true) }
    var dailyDigestTime by remember { mutableStateOf("08:00 AM") }
    var hapticsEnabled by remember { mutableStateOf(true) }
    var defaultCategory by remember { mutableStateOf("Personal") }
    var defaultDueTime by remember { mutableStateOf("09:00 AM") }
    var autoArchiveOption by remember { mutableStateOf("After 7 Days") }

    // Customization & Appearance state
    var selectedAppIcon by remember { mutableStateOf("Classic iOS") }
    var compactModeEnabled by remember { mutableStateOf(false) }

    // Security & Data
    var cloudSyncEnabled by remember { mutableStateOf(true) }
    var showAppLockSetupSheet by remember { mutableStateOf(false) }
    var appLockSetupStep by remember { mutableStateOf(SetupStep.SELECT_LOCK_TYPE) }
    var showLockTimeoutSheet by remember { mutableStateOf(false) }
    var showEnrollBiometricDialog by remember { mutableStateOf(false) }

    // Dialog & Modal sheet states
    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showCategoryPickerSheet by remember { mutableStateOf(false) }
    var showTimePickerSheet by remember { mutableStateOf(false) }
    var showAutoArchiveSheet by remember { mutableStateOf(false) }
    var showAppIconSheet by remember { mutableStateOf(false) }
    var showClearCompletedDialog by remember { mutableStateOf(false) }
    var showResetDataDialog by remember { mutableStateOf(false) }
    var showDesignTokensSheet by remember { mutableStateOf(false) }
    var showStatsSheet by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }

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
            item {
                UserProfileCard(
                    userName = userName,
                    userEmail = userEmail,
                    userTier = userTier,
                    accentColor = SystemBlue,
                    onClick = { showEditProfileSheet = true }
                )
            }

            // 2. Lifetime Productivity Statistics Summary Bento Card
            item {
                ProductivityStatsCard(
                    totalTasks = totalTasks,
                    completedCount = completedCount,
                    completionPercentage = completionPercentage,
                    accentColor = SystemBlue,
                    onViewDetails = { showStatsSheet = true }
                )
            }

            // 3. Preferences & Reminders
            item {
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
                            icon = Icons.Outlined.Schedule,
                            iconTint = ApplePersonal,
                            title = "Daily Summary Digest",
                            subtitle = "Summary at $dailyDigestTime",
                            checked = dailyDigestEnabled,
                            accentColor = SystemBlue,
                            onCheckedChange = { dailyDigestEnabled = it },
                            showDivider = true
                        )
                        SettingsSwitchRow(
                            icon = Icons.Outlined.Vibration,
                            iconTint = AppleHealth,
                            title = "Haptic Feedback",
                            subtitle = "Tactile vibration on task completion",
                            checked = hapticsEnabled,
                            accentColor = SystemBlue,
                            onCheckedChange = { hapticsEnabled = it },
                            showDivider = true
                        )
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
                            value = defaultDueTime,
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

            // 4. Customization & Theme
            item {
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
                            value = selectedAppIcon,
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

            // 5. Cloud Sync & Security / App Lock
            item {
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
                            showDivider = true
                        )

                        if (AppLockManager.isLockEnabled) {
                            SettingsSwitchRow(
                                icon = Icons.Outlined.Fingerprint,
                                iconTint = AppleHealth,
                                title = "Unlock with Fingerprint",
                                subtitle = if (AppLockManager.isBiometricEnabled) "Enabled for instant app unlock" else "Use fingerprint instead of passcode",
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
                                                        title = "Set Up Fingerprint",
                                                        subtitle = "Scan your fingerprint to enable biometric unlock",
                                                        negativeButtonText = "Cancel",
                                                        onSuccess = {
                                                            AppLockManager.setBiometricAuth(true)
                                                            Toast.makeText(context, "Fingerprint unlock enabled", Toast.LENGTH_SHORT).show()
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
                                                Toast.makeText(context, "No fingerprint hardware found on this device", Toast.LENGTH_SHORT).show()
                                            }
                                            BiometricAvailability.HW_UNAVAILABLE -> {
                                                Toast.makeText(context, "Fingerprint sensor is currently unavailable", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        AppLockManager.setBiometricAuth(false)
                                        Toast.makeText(context, "Fingerprint unlock disabled", Toast.LENGTH_SHORT).show()
                                    }
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
                                showDivider = true
                            )
                        }

                        SettingsSwitchRow(
                            icon = Icons.Outlined.CloudSync,
                            iconTint = SystemBlue,
                            title = "iCloud Sync",
                            subtitle = if (cloudSyncEnabled) "Synced 2 minutes ago" else "Offline mode",
                            checked = cloudSyncEnabled,
                            accentColor = SystemBlue,
                            onCheckedChange = {
                                cloudSyncEnabled = it
                                val msg = if (it) "Cloud sync active" else "Cloud sync paused"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            showDivider = false
                        )
                    }
                }
            }

            // 6. Data & Task Management
            item {
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
                            showDivider = true
                        )
                        SettingsActionRow(
                            icon = Icons.Outlined.FileDownload,
                            iconTint = AppleHealth,
                            title = "Export Backup Data",
                            badgeText = "$totalTasks items",
                            onClick = {
                                Toast.makeText(context, "Exported $totalTasks tasks to taskflow_backup.json", Toast.LENGTH_SHORT).show()
                            },
                            showDivider = false
                        )
                    }
                }
            }

            // 7. About & App Specification
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeaderTitle(title = "ABOUT TASKFLOW")
                    SettingsGroupCard {
                        SettingsActionRow(
                            icon = Icons.Outlined.Info,
                            iconTint = SystemBlue,
                            title = "Apple HIG Design System",
                            badgeText = "View Tokens",
                            onClick = { showDesignTokensSheet = true },
                            showDivider = true
                        )
                        SettingsActionRow(
                            icon = Icons.Outlined.StarRate,
                            iconTint = AppleStudy,
                            title = "Rate TaskFlow & Feedback",
                            badgeText = "5 Stars ⭐",
                            onClick = { showFeedbackSheet = true },
                            showDivider = true
                        )
                        SettingsValueRow(
                            icon = Icons.Outlined.CheckCircle,
                            iconTint = AppleHealth,
                            title = "App Version",
                            value = "v1.2.0 (Build 42)",
                            onClick = { },
                            showChevron = false,
                            showDivider = false
                        )
                    }
                }
            }

            // 8. Destructive Actions
            item {
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

    // Modal Sheet 1: Edit Profile Sheet
    if (showEditProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditProfileSheet = false },
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
                    text = "Edit Profile",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )

                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemBlue,
                        unfocusedBorderColor = SystemDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = userEmail,
                    onValueChange = { userEmail = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemBlue,
                        unfocusedBorderColor = SystemDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        showEditProfileSheet = false
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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

                listOf("Work", "Personal", "Health", "Study").forEach { cat ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (defaultCategory == cat) SystemBlueLight else SystemGroupedBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                defaultCategory = cat
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
                            Text(
                                text = cat,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (defaultCategory == cat) SystemBlue else SystemLabelPrimary
                            )
                            if (defaultCategory == cat) {
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

                listOf("08:00 AM", "09:00 AM", "10:00 AM", "02:00 PM", "06:00 PM").forEach { timeOption ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (defaultDueTime == timeOption) SystemBlueLight else SystemGroupedBackground,
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
                                color = if (defaultDueTime == timeOption) SystemBlue else SystemLabelPrimary
                            )
                            if (defaultDueTime == timeOption) {
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select App Icon Style",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )

                listOf("Classic iOS", "Dark Minimal", "Neon Blue Glow", "Glassmorphism").forEach { iconName ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedAppIcon == iconName) SystemBlueLight else SystemGroupedBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAppIcon = iconName
                                showAppIconSheet = false
                                Toast.makeText(context, "App icon updated to $iconName", Toast.LENGTH_SHORT).show()
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
                                text = iconName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selectedAppIcon == iconName) SystemBlue else SystemLabelPrimary
                            )
                            if (selectedAppIcon == iconName) {
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

    // Modal Sheet 6: Apple HIG Tokens Specs
    if (showDesignTokensSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDesignTokensSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SystemGroupedBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Apple HIG Design System",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )
                Text(
                    text = "System Specification Tokens for TaskFlow Android",
                    fontSize = 14.sp,
                    color = SystemLabelSecondary
                )

                SettingsGroupCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("• Primary Accent: ${accentColor.displayName}", fontSize = 14.sp, color = SystemBlue, fontWeight = FontWeight.SemiBold)
                        Text("• Grouped Canvas: Light #F2F2F7 / Dark #000000", fontSize = 14.sp, color = SystemLabelPrimary)
                        Text("• Grouped Surface: Light #FFFFFF / Dark #1C1C1E", fontSize = 14.sp, color = SystemLabelPrimary)
                        Text("• Corner Radius: 12.dp rounded corners", fontSize = 14.sp, color = SystemLabelPrimary)
                        Text("• Thin Dividers: 0.5.dp thickness", fontSize = 14.sp, color = SystemLabelPrimary)
                        Text("• Typography: SF / Inter (-0.5sp letter spacing)", fontSize = 14.sp, color = SystemLabelPrimary)
                    }
                }

                Button(
                    onClick = { showDesignTokensSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal Sheet 7: Rate & Feedback
    if (showFeedbackSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFeedbackSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SystemSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enjoying TaskFlow?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )
                Text(
                    text = "Tap a star to rate your experience",
                    fontSize = 14.sp,
                    color = SystemLabelSecondary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Star ${index + 1}",
                            tint = AppleStudy,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    showFeedbackSheet = false
                                    Toast.makeText(context, "Thank you for rating 5 stars! ⭐", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }

                Button(
                    onClick = { showFeedbackSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Submit Feedback", fontWeight = FontWeight.Bold)
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

    // Dialog: Prompt to enroll fingerprint in device settings
    if (showEnrollBiometricDialog) {
        AlertDialog(
            onDismissRequest = { showEnrollBiometricDialog = false },
            title = {
                Text(
                    text = "Enroll Fingerprint",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SystemLabelPrimary
                )
            },
            text = {
                Text(
                    text = "No fingerprint is currently registered on this device. Would you like to open device settings and add a fingerprint?",
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
                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCoBuvdtAGCYMF3-7fgZnTDoXh_snzGoLq1GpOuiTnpGcrs_BUFGcSioVrt4viicVuGC9ZFj9lsxuXEX_szLOWSsdMaaKlMDlKGSJJcfKhyUthyTuHEgIgtHGDUcLm3JMsUS7KQWaovaJxSOD24Pd9PJg9MrXyDZLwQHIBQ2P-6aUTKoBr9ebkyIWiEcuxV_wt95LmVK_nmu9kMlfXd-pio3PZqk-2_pF6npzfw8BXNvJ6lIacTKXE",
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
                onCheckedChange = onCheckedChange,
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsRowIcon(icon = icon, iconTint = iconTint)

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = SystemLabelPrimary,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = value,
                fontSize = 15.sp,
                color = SystemLabelSecondary
            )

            if (showChevron) {
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsRowIcon(icon = icon, iconTint = if (enabled) iconTint else SystemGray)

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) SystemLabelPrimary else SystemLabelSecondary,
                modifier = Modifier.weight(1f)
            )

            badgeText?.let {
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
    showDivider: Boolean = true
) {
    val activeAccent = if (accentColor != Color.Unspecified) accentColor else SystemBlue
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsRowIcon(icon = icon, iconTint = iconTint)

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = SystemLabelPrimary,
                modifier = Modifier.weight(1f)
            )

            // iOS Segmented Control
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SystemGroupedBackground,
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
                            color = if (isSelected) SystemSurface else Color.Transparent,
                            shadowElevation = if (isSelected) 1.dp else 0.dp,
                            modifier = Modifier
                                .clickable { onOptionSelected(index) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = option,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) activeAccent else SystemLabelSecondary
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
fun AccentColorSwatchRow(
    selectedAccent: AppAccentColor,
    onAccentSelected: (AppAccentColor) -> Unit,
    showDivider: Boolean = true
) {
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
                color = SystemLabelPrimary
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
                            .clickable { onAccentSelected(accent) }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
