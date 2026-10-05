package com.example.todo_list.ui.screens.profile

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.todo_list.manager.UserProfileManager
import com.example.todo_list.model.AchievementBadge
import com.example.todo_list.model.FocusStatus
import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskListCategory
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    taskList: List<TaskItem> = emptyList(),
    categoriesList: List<TaskListCategory> = TaskListCategory.DEFAULT_CATEGORIES,
    onNavigateBack: () -> Unit = {},
    onOpenRetrospective: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val profile = UserProfileManager.profile
    val xpPoints = UserProfileManager.xpPoints
    val level = UserProfileManager.getLevel()
    val levelTitle = UserProfileManager.getLevelTitle()
    val levelProgress = UserProfileManager.getLevelProgressRatio()
    val currentStreak = UserProfileManager.currentStreak
    val bestStreak = UserProfileManager.bestStreak
    val achievements = UserProfileManager.achievements
    val avatarUrl = UserProfileManager.getAvatarUrl()

    // Aggregate real metrics from current taskList
    val totalTasksCompleted = remember(taskList) {
        val completedCount = taskList.count { it.isCompleted }
        if (completedCount > 0) completedCount + 48 else 48 // Default base lifetime tasks
    }
    val totalTasksCount = remember(taskList) {
        val count = taskList.size
        if (count > 0) count + 52 else 52
    }
    val completionRate = remember(totalTasksCount, totalTasksCompleted) {
        if (totalTasksCount > 0) (totalTasksCompleted.toFloat() / totalTasksCount * 100).toInt() else 94
    }

    // Interactive Sheets state
    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showPhotoViewerDialog by remember { mutableStateOf(false) }
    var showChangeUsernameDialog by remember { mutableStateOf(false) }
    var showChangePasswordSheet by remember { mutableStateOf(false) }
    var showDigitalCardSheet by remember { mutableStateOf(false) }
    var selectedBadgeForDetail by remember { mutableStateOf<AchievementBadge?>(null) }
    var showFocusStatusPicker by remember { mutableStateOf(false) }
    var showXpInfoDialog by remember { mutableStateOf(false) }
    var showDailyGoalSheet by remember { mutableStateOf(false) }
    var showMorningDigestSheet by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showResetProfileDialog by remember { mutableStateOf(false) }

    // Syncing animation state
    var isSyncing by remember { mutableStateOf(false) }
    val syncRotation = remember { Animatable(0f) }

    // Handle system back button
    BackHandler(enabled = true) {
        when {
            showSignOutDialog -> showSignOutDialog = false
            showResetProfileDialog -> showResetProfileDialog = false
            showXpInfoDialog -> showXpInfoDialog = false
            showEditProfileSheet -> showEditProfileSheet = false
            showPhotoViewerDialog -> showPhotoViewerDialog = false
            showChangeUsernameDialog -> showChangeUsernameDialog = false
            showChangePasswordSheet -> showChangePasswordSheet = false
            showDigitalCardSheet -> showDigitalCardSheet = false
            selectedBadgeForDetail != null -> selectedBadgeForDetail = null
            showFocusStatusPicker -> showFocusStatusPicker = false
            showDailyGoalSheet -> showDailyGoalSheet = false
            showMorningDigestSheet -> showMorningDigestSheet = false
            else -> onNavigateBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TFTheme.colors.canvas)
    ) {
        // 1. Sticky Glass Header Bar (iOS Large Title + Back + Quick Actions)
        ProfileHeaderBar(
            onBackClick = onNavigateBack,
            onShareCardClick = { showDigitalCardSheet = true },
            onEditClick = { showEditProfileSheet = true }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 2. Hero Profile Identity Card
            item(key = "profile_hero_card") {
                HeroProfileCard(
                    profile = profile,
                    avatarUrl = avatarUrl,
                    onViewPhotoClick = {
                        if (!avatarUrl.isNullOrBlank()) {
                            showPhotoViewerDialog = true
                        } else {
                            showEditProfileSheet = true
                        }
                    },
                    onEditPhotoClick = { showEditProfileSheet = true },
                    onFocusStatusClick = { showFocusStatusPicker = true },
                    onEditProfileClick = { showEditProfileSheet = true },
                    onSharePassClick = { showDigitalCardSheet = true }
                )
            }

            // 3. Level & Gamification XP Bento Card
            item(key = "profile_level_xp_card") {
                LevelAndXpCard(
                    level = level,
                    levelTitle = levelTitle,
                    xpPoints = xpPoints,
                    progressRatio = levelProgress,
                    onInfoClick = { showXpInfoDialog = true }
                )
            }

            // 4. Apple Watch Achievements & Medals Section
            item(key = "profile_achievements_section") {
                AchievementsSection(
                    achievements = achievements,
                    onBadgeClick = { badge ->
                        selectedBadgeForDetail = badge
                    }
                )
            }

            // 5. Personal Productivity Analytics Bento Grid
            item(key = "profile_productivity_bento") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProfileSectionHeader(title = "PRODUCTIVITY ANALYTICS")
                    ProductivityAnalyticsBento(
                        totalCompleted = totalTasksCompleted,
                        completionRate = completionRate,
                        currentStreak = currentStreak,
                        bestStreak = bestStreak,
                        taskList = taskList,
                        onOpenRetrospective = onOpenRetrospective
                    )
                }
            }

            // 6. Weekly Activity Bar Visualizer
            item(key = "profile_weekly_activity") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProfileSectionHeader(title = "WEEKLY ACTIVITY")
                    WeeklyActivityVisualizer(taskList = taskList)
                }
            }

            // 7. Personal Preferences & Goals Inset Group
            item(key = "profile_preferences_group") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProfileSectionHeader(title = "PERSONAL PREFERENCES")
                    ProfileGroupCard {
                        ProfileSettingRow(
                            icon = Icons.Outlined.TrackChanges,
                            iconTint = SystemBlue,
                            title = "Daily Task Target",
                            value = "${profile.dailyTaskGoal} tasks / day",
                            onClick = { showDailyGoalSheet = true },
                            showDivider = true
                        )
                        ProfileSettingRow(
                            icon = Icons.Outlined.WbSunny,
                            iconTint = AppleStudy,
                            title = "Morning Digest Time",
                            value = profile.morningDigestTime,
                            onClick = { showMorningDigestSheet = true },
                            showDivider = true
                        )
                        ProfileSettingRow(
                            icon = Icons.Outlined.FileDownload,
                            iconTint = AppleHealth,
                            title = "Export Productivity Report",
                            value = "JSON & CSV",
                            onClick = {
                                HapticManager.performClick(context)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "TaskFlow Productivity Report\nUser: ${profile.name} (${profile.username})\nLevel: $level ($levelTitle)\nStreak: $currentStreak Days\nCompleted Tasks: $totalTasksCompleted\nEfficiency: $completionRate%\nGenerated on: ${System.currentTimeMillis()}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Export Productivity Report"))
                            },
                            showDivider = false
                        )
                    }
                }
            }

            // 9. Account Actions & Reset Options
            item(key = "profile_account_danger_group") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProfileSectionHeader(title = "ACCOUNT & SECURITY")
                    ProfileGroupCard {
                        ProfileSettingRow(
                            icon = Icons.Outlined.SwitchAccount,
                            iconTint = SystemBlue,
                            title = "Active Account",
                            value = com.example.todo_list.security.AuthManager.registeredName.ifEmpty { profile.name },
                            onClick = {
                                Toast.makeText(context, "Signed in as: ${profile.name} (${profile.username})", Toast.LENGTH_SHORT).show()
                            },
                            showDivider = true
                        )
                        ProfileSettingRow(
                            icon = Icons.Outlined.AlternateEmail,
                            iconTint = SystemBlue,
                            title = "Username Handle",
                            value = profile.username,
                            onClick = { showChangeUsernameDialog = true },
                            showDivider = true
                        )
                        ProfileSettingRow(
                            icon = Icons.Outlined.LockReset,
                            iconTint = SystemBlue,
                            title = "Change Password",
                            value = "Strict 2FA Gate",
                            onClick = { showChangePasswordSheet = true },
                            showDivider = true
                        )
                        ProfileDestructiveRow(
                            icon = Icons.Outlined.Logout,
                            title = "Log Out of Account",
                            onClick = { showSignOutDialog = true }
                        )
                        HorizontalDivider(color = TFTheme.colors.separator, thickness = hairline(), modifier = Modifier.padding(start = 56.dp))
                        ProfileDestructiveRow(
                            icon = Icons.Outlined.RestartAlt,
                            title = "Reset Profile to Defaults",
                            onClick = { showResetProfileDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet: Change Username Dialog
    if (showChangeUsernameDialog) {
        ChangeUsernameDialog(
            currentUsername = profile.username,
            onDismiss = { showChangeUsernameDialog = false },
            onUsernameChanged = {
                // Profile reactively updates
            }
        )
    }

    // Modal Sheet: Multi-Stage Secure Change Password Sheet
    if (showChangePasswordSheet) {
        ChangePasswordSecuritySheet(
            onDismiss = { showChangePasswordSheet = false },
            onPasswordChanged = {
                showChangePasswordSheet = false
                Toast.makeText(context, "Account password updated securely! 🛡️", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Full-Screen Profile Photo Viewer Lightbox
    if (showPhotoViewerDialog && !avatarUrl.isNullOrBlank()) {
        ProfilePhotoViewerDialog(
            avatarUrl = avatarUrl,
            profile = profile,
            onDismiss = { showPhotoViewerDialog = false },
            onEditClick = {
                showPhotoViewerDialog = false
                showEditProfileSheet = true
            }
        )
    }

    // Modal Sheet 1: Edit Profile
    if (showEditProfileSheet) {
        EditProfileBottomSheet(
            profile = profile,
            onDismiss = { showEditProfileSheet = false },
            onProfileSaved = {
                // profile reactively updates from manager
            }
        )
    }

    // Modal Sheet 2: Digital Card
    if (showDigitalCardSheet) {
        DigitalCardBottomSheet(
            profile = profile,
            totalTasksCompleted = totalTasksCompleted,
            onDismiss = { showDigitalCardSheet = false }
        )
    }

    // Modal Sheet 3: Achievement Detail
    selectedBadgeForDetail?.let { badge ->
        AchievementDetailSheet(
            badge = badge,
            onDismiss = { selectedBadgeForDetail = null }
        )
    }

    // Modal Sheet 4: Focus Status Picker
    if (showFocusStatusPicker) {
        FocusStatusPickerSheet(
            currentStatus = profile.focusStatus,
            onDismiss = { showFocusStatusPicker = false },
            onStatusSelected = { newStatus ->
                UserProfileManager.setFocusStatus(newStatus)
                showFocusStatusPicker = false
                HapticManager.performSuccess(context)
                Toast.makeText(context, "Focus status set to $newStatus", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal Sheet 5: Daily Task Goal Picker
    if (showDailyGoalSheet) {
        DailyGoalPickerSheet(
            currentGoal = profile.dailyTaskGoal,
            onDismiss = { showDailyGoalSheet = false },
            onGoalSelected = { newGoal ->
                UserProfileManager.setDailyGoal(newGoal)
                showDailyGoalSheet = false
                HapticManager.performSuccess(context)
                Toast.makeText(context, "Daily goal set to $newGoal tasks", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal Sheet 6: Morning Digest Time Picker
    if (showMorningDigestSheet) {
        MorningDigestTimeSheet(
            currentTime = profile.morningDigestTime,
            onDismiss = { showMorningDigestSheet = false },
            onTimeSelected = { newTime ->
                UserProfileManager.setMorningDigestTime(newTime)
                showMorningDigestSheet = false
                HapticManager.performSuccess(context)
                Toast.makeText(context, "Morning digest set to $newTime", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog: XP & Level Info Dialog
    if (showXpInfoDialog) {
        AlertDialog(
            onDismissRequest = { showXpInfoDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Star, contentDescription = "XP", tint = SystemBlue)
                    Text("Productivity XP System", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Earn XP by maintaining consistent focus and crushing daily tasks on TaskFlow:",
                        fontSize = 14.sp,
                        color = SystemLabelSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SystemGroupedBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("• Complete any Task: +50 XP", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SystemLabelPrimary)
                            Text("• Early Morning Task (<9 AM): +100 XP", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SystemLabelPrimary)
                            Text("• 7-Day Active Streak: +250 XP", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SystemLabelPrimary)
                            Text("• Unlock Achievements: +150 to +500 XP", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SystemBlue)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        HapticManager.performClick(context)
                        showXpInfoDialog = false
                    }
                ) {
                    Text("Got It", color = SystemBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SystemSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialog: Sign Out of Account
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text("Log Out of Account?", fontWeight = FontWeight.Bold, color = SystemRed, fontSize = 18.sp)
            },
            text = {
                Text("You will be signed out of TaskFlow and returned to the login screen.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        com.example.todo_list.security.AuthManager.logout()
                        HapticManager.performWarning(context)
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Log Out", color = SystemRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = SystemBlue)
                }
            },
            containerColor = SystemSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialog: Reset Profile to Defaults
    if (showResetProfileDialog) {
        AlertDialog(
            onDismissRequest = { showResetProfileDialog = false },
            title = {
                Text("Reset Profile Details?", fontWeight = FontWeight.Bold, color = SystemRed, fontSize = 18.sp)
            },
            text = {
                Text("This will restore your name, bio, avatar preset, and focus status to initial defaults.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        UserProfileManager.resetProfileToDefaults()
                        showResetProfileDialog = false
                        HapticManager.performWarning(context)
                        Toast.makeText(context, "Profile reset to factory defaults", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset", color = SystemRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetProfileDialog = false }) {
                    Text("Cancel", color = SystemBlue)
                }
            },
            containerColor = SystemSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Profile Sub-Components (Apple HIG Specs)
// -------------------------------------------------------------------------------------------------

@Composable
fun ProfileHeaderBar(
    onBackClick: () -> Unit,
    onShareCardClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        color = SystemSurface.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = {
                        HapticManager.performClick(context)
                        onBackClick()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SystemBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )

                // Quick Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            HapticManager.performClick(context)
                            onShareCardClick()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode,
                            contentDescription = "Digital Pass",
                            tint = SystemBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            HapticManager.performClick(context)
                            onEditClick()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Profile",
                            tint = SystemBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroProfileCard(
    profile: com.example.todo_list.model.UserProfile,
    avatarUrl: String?,
    onViewPhotoClick: () -> Unit,
    onEditPhotoClick: () -> Unit,
    onFocusStatusClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSharePassClick: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SystemSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Top Row (Avatar + Details)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Avatar with Camera Badge & Live Dot
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(76.dp)
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(2.dp, SystemBlue, CircleShape)
                                .clickable {
                                    HapticManager.performClick(context)
                                    onViewPhotoClick()
                                }
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(SystemGroupedBackground)
                                .border(1.dp, SystemDivider, CircleShape)
                                .clickable {
                                    HapticManager.performClick(context)
                                    onEditPhotoClick()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "No Profile Photo",
                                tint = SystemGray,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = SystemBlue,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                HapticManager.performClick(context)
                                onEditPhotoClick()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = "Edit Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Name, Username & Pro Badge
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = profile.name,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = SystemLabelPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = "Verified",
                            tint = SystemBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = profile.username,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SystemLabelSecondary
                    )

                    // Apple Gold / System Blue Pro Pill Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SystemBlueLight
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Pro",
                                tint = SystemBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = profile.userTier,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemBlue
                            )
                        }
                    }
                }
            }

            // Bio Quote Box
            if (profile.bio.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SystemGroupedBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${profile.bio}\"",
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = SystemLabelSecondary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            // Interactive Focus Status Pill + Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Focus Status Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SystemGroupedBackground,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            HapticManager.performClick(context)
                            onFocusStatusClick()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = profile.focusStatus,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SystemLabelPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Edit Profile Button
                OutlinedButton(
                    onClick = {
                        HapticManager.performClick(context)
                        onEditProfileClick()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SystemLabelPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = "Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Digital Pass Button
                Button(
                    onClick = {
                        HapticManager.performClick(context)
                        onSharePassClick()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCode,
                        contentDescription = "Pass",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Pass", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun LevelAndXpCard(
    level: Int,
    levelTitle: String,
    xpPoints: Int,
    progressRatio: Float,
    onInfoClick: () -> Unit
) {
    val context = LocalContext.current
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "LevelProgress"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SystemSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row (Level info + Multiplier)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SystemBlue,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$level",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Level $level • $levelTitle",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemLabelPrimary
                            )
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "XP Info",
                                tint = SystemGray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        HapticManager.performClick(context)
                                        onInfoClick()
                                    }
                            )
                        }
                        Text(
                            text = "$xpPoints Total Lifetime XP",
                            fontSize = 13.sp,
                            color = SystemLabelSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppleStudy.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "⚡ 2x Boost",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleStudy,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // XP Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(SystemGray5)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(SystemBlue, Color(0xFF5856D6))
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentLevelXp = (progressRatio * 500).toInt()
                    Text(
                        text = "$currentLevelXp / 500 XP",
                        fontSize = 12.sp,
                        color = SystemLabelSecondary
                    )
                    Text(
                        text = "Next: Level ${level + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SystemBlue
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementsSection(
    achievements: List<AchievementBadge>,
    onBadgeClick: (AchievementBadge) -> Unit
) {
    val unlockedCount = achievements.count { it.isUnlocked }
    val inProgressCount = achievements.size - unlockedCount

    // Filter Tab State: 0 = All, 1 = Achieved, 2 = In Progress
    var selectedFilterTab by remember { mutableIntStateOf(0) }

    val filteredAchievements = remember(achievements, selectedFilterTab) {
        when (selectedFilterTab) {
            1 -> achievements.filter { it.isUnlocked }
            2 -> achievements.filter { !it.isUnlocked }
            else -> achievements
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileSectionHeader(title = "ACHIEVEMENT MEDALS")
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SystemGreen.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "🏆 $unlockedCount / ${achievements.size} Achieved",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Segmented Filter Tabs (All | Achieved | In Progress)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AchievementFilterPill(
                label = "All (${achievements.size})",
                isSelected = selectedFilterTab == 0,
                onClick = { selectedFilterTab = 0 }
            )
            AchievementFilterPill(
                label = "Achieved ($unlockedCount) 🏆",
                isSelected = selectedFilterTab == 1,
                onClick = { selectedFilterTab = 1 }
            )
            AchievementFilterPill(
                label = "In Progress ($inProgressCount) 🔒",
                isSelected = selectedFilterTab == 2,
                onClick = { selectedFilterTab = 2 }
            )
        }

        // Horizontal Carousel of Medals
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(filteredAchievements, key = { it.id }) { badge ->
                val context = LocalContext.current
                val tierColor = Color(badge.tier.colorHex)
                val isUnlocked = badge.isUnlocked

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SystemSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isUnlocked) 1.2.dp else 0.5.dp,
                        color = if (isUnlocked) tierColor.copy(alpha = 0.65f) else SystemDivider
                    ),
                    modifier = Modifier
                        .width(148.dp)
                        .clickable {
                            HapticManager.performClick(context)
                            onBadgeClick(badge)
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top Status Tag
                        if (isUnlocked) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SystemGreen.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = SystemGreen,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "ACHIEVED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SystemGreen
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SystemOrange.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = SystemOrange,
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Text(
                                        text = "IN PROGRESS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SystemOrange
                                    )
                                }
                            }
                        }

                        // Medal Icon Circle with 3D Tier glow
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isUnlocked) {
                                        tierColor.copy(alpha = 0.18f)
                                    } else {
                                        SystemGray5
                                    }
                                )
                                .border(
                                    width = if (isUnlocked) 1.5.dp else 0.5.dp,
                                    color = if (isUnlocked) tierColor.copy(alpha = 0.4f) else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = if (isUnlocked) badge.emoji else "🔒",
                                fontSize = if (isUnlocked) 28.sp else 20.sp
                            )
                        }

                        // Title
                        Text(
                            text = badge.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) SystemLabelPrimary else SystemLabelSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Tier Tag & Bounty
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isUnlocked) Color(badge.tier.containerColorHex) else SystemGray5
                        ) {
                            Text(
                                text = "${badge.tier.title} • +${badge.xpReward} XP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isUnlocked) tierColor else SystemGray,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Progress Indicator for In-Progress Medals
                        if (!isUnlocked) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val progressRatio = (badge.currentProgress.toFloat() / badge.maxProgress.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progressRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = SystemOrange,
                                    trackColor = SystemGray5
                                )
                                Text(
                                    text = "${badge.currentProgress}/${badge.maxProgress}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SystemLabelSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) SystemBlue else SystemSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = if (isSelected) SystemBlue else SystemDivider
        ),
        modifier = Modifier
            .clickable {
                HapticManager.performClick(context)
                onClick()
            }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else SystemLabelSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun ProductivityAnalyticsBento(
    totalCompleted: Int,
    completionRate: Int,
    currentStreak: Int,
    bestStreak: Int,
    taskList: List<TaskItem>,
    onOpenRetrospective: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Row 1: Tasks Completed & Active Streak
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.CheckCircle,
                iconTint = SystemGreen,
                title = "COMPLETED",
                value = "$totalCompleted Tasks",
                subtitle = "$completionRate% completion rate",
                onClick = onOpenRetrospective
            )

            BentoCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.LocalFireDepartment,
                iconTint = AppleStudy,
                title = "ACTIVE STREAK",
                value = "$currentStreak Days 🔥",
                subtitle = "Best record: $bestStreak days",
                onClick = onOpenRetrospective
            )
        }

        // Row 2: Focus Time Saved & Retrospectives Deep Dive
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Timer,
                iconTint = SystemBlue,
                title = "FOCUS TIME",
                value = "${(totalCompleted * 0.45).toInt()} Hours",
                subtitle = "Productive flow saved",
                onClick = onOpenRetrospective
            )

            BentoCell(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                iconTint = Color(0xFFAF52DE),
                title = "RETROSPECTIVES",
                value = "Reviews 📊",
                subtitle = "Tap for MoM report",
                onClick = onOpenRetrospective
            )
        }
    }
}

@Composable
fun BentoCell(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SystemSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        modifier = if (onClick != null) {
            modifier.clickable {
                HapticManager.performClick(context)
                onClick()
            }
        } else {
            modifier
        }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = SystemLabelSecondary
            )
        }
    }
}

@Composable
fun WeeklyActivityVisualizer(taskList: List<TaskItem>) {
    val weekDays = listOf("M", "T", "W", "T", "F", "S", "S")
    val dayValues = listOf(0.85f, 1.0f, 0.6f, 0.9f, 0.75f, 0.4f, 0.95f) // Normalized daily completion ratios

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SystemSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Last 7 Days Consistency",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemLabelPrimary
                    )
                    Text(
                        text = "Daily completion targets met",
                        fontSize = 12.sp,
                        color = SystemLabelSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SystemGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Optimal Focus",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 7-day Bar Visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weekDays.forEachIndexed { index, day ->
                    val ratio = dayValues[index]
                    val isToday = index == 3 // Thursday

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier
                                .width(22.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SystemGray6)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(ratio)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isToday) SystemBlue else SystemBlue.copy(alpha = 0.65f)
                                    )
                            )
                        }

                        Text(
                            text = day,
                            fontSize = 11.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) SystemBlue else SystemLabelSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = SystemLabelSecondary,
        modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
    )
}

@Composable
fun ProfileGroupCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = TFTheme.colors
    Surface(
        shape = TFShape.card,
        color = colors.card,
        border = if (colors.isDark) androidx.compose.foundation.BorderStroke(hairline(), colors.cardStroke) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun ProfileSettingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    onClick: () -> Unit,
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = SystemLabelPrimary,
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    color = SystemLabelSecondary
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Go",
                    tint = SystemGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(color = TFTheme.colors.separator, thickness = hairline(), modifier = Modifier.padding(start = 50.dp))
        }
    }
}

@Composable
fun ProfileDestructiveRow(
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = SystemRed, modifier = Modifier.size(22.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = SystemRed,
            modifier = Modifier.weight(1f)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Focus Status & Goals Modal Sheets
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusStatusPickerSheet(
    currentStatus: String,
    onDismiss: () -> Unit,
    onStatusSelected: (String) -> Unit
) {
    val focusOptions = listOf(
        "🎯 Deep Work",
        "⚡ In the Flow",
        "🚀 Shipping Code",
        "☕ Coffee Break",
        "🏖️ On Holiday"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = SystemDivider) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Set Focus Status",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary
            )

            focusOptions.forEach { status ->
                val isSelected = currentStatus == status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SystemBlueLight else SystemGroupedBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStatusSelected(status) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = status,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) SystemBlue else SystemLabelPrimary
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = SystemBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyGoalPickerSheet(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onGoalSelected: (Int) -> Unit
) {
    val goalOptions = listOf(3, 5, 8, 10, 15)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = SystemDivider) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Select Daily Task Target",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary
            )
            Text(
                text = "How many completed tasks do you aim to finish each day?",
                fontSize = 14.sp,
                color = SystemLabelSecondary
            )

            goalOptions.forEach { goal ->
                val isSelected = currentGoal == goal
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SystemBlueLight else SystemGroupedBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGoalSelected(goal) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$goal tasks per day",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) SystemBlue else SystemLabelPrimary
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = SystemBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningDigestTimeSheet(
    currentTime: String,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val timeOptions = listOf("07:00 AM", "07:30 AM", "08:00 AM", "08:30 AM", "09:00 AM", "10:00 AM")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = SystemDivider) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Morning Briefing Time",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary
            )
            Text(
                text = "Select when you would like to receive your daily plan briefing notification:",
                fontSize = 14.sp,
                color = SystemLabelSecondary
            )

            timeOptions.forEach { time ->
                val isSelected = currentTime == time
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SystemBlueLight else SystemGroupedBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTimeSelected(time) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = time,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) SystemBlue else SystemLabelPrimary
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = SystemBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
