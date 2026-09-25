package com.example.todo_list.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import com.example.todo_list.ui.components.CreateTaskBottomSheet
import com.example.todo_list.ui.components.EditTaskBottomSheet
import com.example.todo_list.ui.components.ai.TaskFlowIntelligenceSheet
import com.example.todo_list.ui.components.navigation.TFTabBar
import com.example.todo_list.ui.components.navigation.TFLargeTitleNavBar
import com.example.todo_list.ui.components.primitives.TFCardGroup
import com.example.todo_list.ui.components.primitives.TFGroupDivider
import com.example.todo_list.ui.components.primitives.TFTaskRow
import com.example.todo_list.ui.components.primitives.TFEmptyState
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskListCategory
import com.example.todo_list.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.todo_list.manager.UserProfileManager
import com.example.todo_list.notification.TaskNotificationScheduler
import com.example.todo_list.data.repository.TaskRepository
import com.example.todo_list.ui.components.CreateCategoryBottomSheet
import com.example.todo_list.ui.components.IosTimezoneNotificationBanner
import com.example.todo_list.ui.components.TimezonePickerBottomSheet
import com.example.todo_list.manager.TimePreferencesManager
import com.example.todo_list.utils.TimeFormatHelper
import com.example.todo_list.ui.screens.profile.ProfileScreen
import com.example.todo_list.utils.HapticManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    themeMode: Int = 0,
    onThemeModeChange: (Int) -> Unit = {},
    accentColor: AppAccentColor = AppAccentColor.BLUE,
    onAccentColorChange: (AppAccentColor) -> Unit = {}
) {
    val context = LocalContext.current

    // Request notification permission launcher for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val taskRepository = remember { TaskRepository.getInstance(context) }
    val currentUserId = com.example.todo_list.security.AuthManager.currentUserId ?: ""
    val taskList by remember(currentUserId) {
        if (currentUserId.isNotBlank()) {
            taskRepository.getTasksForUser(currentUserId)
        } else {
            taskRepository.tasks
        }
    }.collectAsState(initial = emptyList())
    val categoriesList by taskRepository.categories.collectAsState(initial = TaskListCategory.DEFAULT_CATEGORIES)

    var selectedTab by remember { mutableStateOf(0) }
    var showProfileScreen by remember { mutableStateOf(false) }
    val userProfile = UserProfileManager.profile
    val avatarUrl = UserProfileManager.getAvatarUrl()
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var showViewAllSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var prefilledTaskCategory by remember { mutableStateOf<String?>(null) }
    var showTimezonePickerSheet by remember { mutableStateOf(false) }
    var showIntelligenceSheet by remember { mutableStateOf(false) }
    var intelligenceInitialTab by remember { mutableIntStateOf(0) }
    val aiShortcutTrigger by com.example.todo_list.MainActivity.openAiChatTrigger

    LaunchedEffect(aiShortcutTrigger) {
        if (aiShortcutTrigger != null) {
            intelligenceInitialTab = 2
            showIntelligenceSheet = true
            com.example.todo_list.MainActivity.openAiChatTrigger.value = null
        }
    }

    val onToggleCompleteHelper: (TaskItem) -> Unit = remember(context, taskRepository, coroutineScope) {
        { toggledTask: TaskItem ->
            coroutineScope.launch {
                val updated = taskRepository.toggleTaskComplete(toggledTask)
                if (updated.isCompleted) {
                    TaskNotificationScheduler.cancel(context, updated)
                    UserProfileManager.addXp(50)
                } else {
                    TaskNotificationScheduler.schedule(context, updated)
                }
            }
        }
    }

    val onDeleteHelper: (TaskItem) -> Unit = remember(context, taskRepository, coroutineScope) {
        { deletedTask: TaskItem ->
            TaskNotificationScheduler.cancel(context, deletedTask)
            coroutineScope.launch {
                taskRepository.deleteTask(deletedTask)
            }
        }
    }

    val onTaskCreatedHelper: (TaskItem) -> Unit = remember(context, taskRepository, coroutineScope) {
        { newTask: TaskItem ->
            TaskNotificationScheduler.schedule(context, newTask)
            coroutineScope.launch {
                taskRepository.insertTask(newTask)
            }
        }
    }

    val onTaskUpdatedHelper: (TaskItem) -> Unit = remember(context, taskRepository, coroutineScope) {
        { updatedTask: TaskItem ->
            if (updatedTask.isCompleted) {
                TaskNotificationScheduler.cancel(context, updatedTask)
            } else {
                TaskNotificationScheduler.schedule(context, updatedTask)
            }
            coroutineScope.launch {
                taskRepository.updateTask(updatedTask)
            }
        }
    }

    // Sort tasks: Pending first (chronologically), Completed tasks at bottom automatically
    val sortedTaskList = remember(taskList) {
        taskList.sortedWith(compareBy<TaskItem> { it.isCompleted }.thenBy { it.getSortValue() })
    }

    val filteredTaskList = remember(sortedTaskList, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedTaskList
        } else {
            sortedTaskList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val listState = rememberLazyListState()

    val totalTasks = remember(sortedTaskList) { sortedTaskList.size }
    val completedTasks = remember(sortedTaskList) { sortedTaskList.count { it.isCompleted } }
    val remainingTasks = remember(totalTasks, completedTasks) { totalTasks - completedTasks }
    val progressRatio = remember(totalTasks, completedTasks) { if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f }

    if (showProfileScreen) {
        ProfileScreen(
            taskList = taskList,
            categoriesList = categoriesList,
            onNavigateBack = { showProfileScreen = false }
        )
    } else {
        Scaffold(
            topBar = {
                if (selectedTab == 0) {
                    TFLargeTitleNavBar(
                        title = "Tasks",
                        avatarUrl = avatarUrl,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onAvatarClick = { showProfileScreen = true },
                        onSparkleClick = {
                            intelligenceInitialTab = 0
                            showIntelligenceSheet = true
                        }
                    )
                }
            },
            bottomBar = {
                TFTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onPlusClick = {
                        prefilledTaskCategory = null
                        showAddTaskSheet = true
                    }
                )
            },
            containerColor = TFTheme.colors.canvas
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                0 -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 24.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(key = "status_bento") {
                            StatusBentoCard(
                                remainingTasks = remainingTasks,
                                progressRatio = progressRatio
                            )
                        }

                        item(key = "intelligence_insight") {
                            IntelligenceInsightCard(
                                pendingTasksCount = remainingTasks,
                                onOptimizeClick = {
                                    HapticManager.performClick(context)
                                    intelligenceInitialTab = 1
                                    showIntelligenceSheet = true
                                }
                            )
                        }

                        item(key = "todays_tasks_header") {
                            TodaysTasksHeader(
                                totalCount = filteredTaskList.size,
                                onViewAllClick = { showViewAllSheet = true }
                            )
                        }

                        item(key = "tasks_grouped_card") {
                            if (filteredTaskList.isEmpty()) {
                                TFCardGroup {
                                    if (searchQuery.isNotBlank()) {
                                        TFEmptyState(
                                            headline = "No results for \"$searchQuery\"",
                                            body = "Try another keyword or clear filters.",
                                            ctaText = "Clear Filters",
                                            onCtaClick = { searchQuery = "" }
                                        )
                                    } else {
                                        TFEmptyState(
                                            headline = "You're all clear",
                                            body = "Nothing due today. Add a task or start with an idea.",
                                            ctaText = "Add Task",
                                            onCtaClick = { showAddTaskSheet = true },
                                            starterChips = listOf("Plan tomorrow", "Weekly review", "Groceries"),
                                            onChipClick = { chipText ->
                                                prefilledTaskCategory = "Personal"
                                                showAddTaskSheet = true
                                            }
                                        )
                                    }
                                }
                            } else {
                                TFCardGroup {
                                    filteredTaskList.forEachIndexed { index, task ->
                                        key(task.id) {
                                            TFTaskRow(
                                                task = task,
                                                onToggleComplete = onToggleCompleteHelper,
                                                onClick = { editingTask = task }
                                            )
                                            if (index < filteredTaskList.size - 1) {
                                                TFGroupDivider()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            1 -> CalendarScreen(
                taskList = taskList,
                categoriesList = categoriesList,
                onToggleComplete = onToggleCompleteHelper,
                onEditTask = { taskToEdit ->
                    editingTask = taskToEdit
                },
                onDeleteTask = onDeleteHelper,
                onAddTaskClick = {
                    prefilledTaskCategory = null
                    showAddTaskSheet = true
                }
            )
            2 -> ListsScreen(
                taskList = taskList,
                categoriesList = categoriesList,
                onToggleComplete = onToggleCompleteHelper,
                onEditTask = { taskToEdit -> editingTask = taskToEdit },
                onDeleteTask = onDeleteHelper,
                onTaskCreated = onTaskCreatedHelper,
                onCreateCategory = { newCategory ->
                    coroutineScope.launch { taskRepository.insertCategory(newCategory) }
                },
                onDeleteCategory = { categoryToDelete ->
                    coroutineScope.launch { taskRepository.deleteCategory(categoryToDelete) }
                },
                onDeleteCategoryWithMigration = { categoryToDelete, targetCategoryName ->
                    coroutineScope.launch { taskRepository.deleteCategoryWithMigration(categoryToDelete, targetCategoryName, currentUserId) }
                },
                onOpenAddTaskSheet = { prefilledCategory ->
                    prefilledTaskCategory = prefilledCategory
                    showAddTaskSheet = true
                }
            )
            3 -> SettingsScreen(
                taskList = taskList,
                categoriesList = categoriesList,
                onCreateCategory = { newCategory ->
                    coroutineScope.launch { taskRepository.insertCategory(newCategory) }
                },
                onDeleteCategoryWithMigration = { categoryToDelete, targetCategoryName ->
                    coroutineScope.launch { taskRepository.deleteCategoryWithMigration(categoryToDelete, targetCategoryName, currentUserId) }
                },
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                accentColor = accentColor,
                onAccentColorChange = onAccentColorChange,
                onClearCompletedTasks = {
                    coroutineScope.launch { taskRepository.clearCompletedTasks(currentUserId) }
                },
                onResetAllData = {
                    coroutineScope.launch { taskRepository.clearAllTasks(currentUserId) }
                },
                onOpenProfile = {
                    showProfileScreen = true
                }
            )
        }

        // Authentic Apple iOS Dynamic Timezone Notification Banner
        IosTimezoneNotificationBanner(
            visible = TimePreferencesManager.showDetectionBanner,
            location = TimePreferencesManager.detectedLocation,
            onCustomizeClick = {
                TimePreferencesManager.dismissBanner()
                showTimezonePickerSheet = true
            },
            onDismiss = {
                TimePreferencesManager.dismissBanner()
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = innerPadding.calculateTopPadding())
        )
    }
}
}


    // Modern Create Task Bottom Sheet
    if (showAddTaskSheet) {
        CreateTaskBottomSheet(
            initialCategory = prefilledTaskCategory ?: "Work",
            categoriesList = categoriesList,
            onCreateCategory = { newCategory ->
                coroutineScope.launch { taskRepository.insertCategory(newCategory) }
            },
            onDismiss = {
                showAddTaskSheet = false
                prefilledTaskCategory = null
            },
            onTaskCreated = { newTask ->
                onTaskCreatedHelper(newTask)
                showAddTaskSheet = false
                prefilledTaskCategory = null
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        )
    }

    // Edit Task Bottom Sheet
    editingTask?.let { taskToEdit ->
        EditTaskBottomSheet(
            task = taskToEdit,
            categoriesList = categoriesList,
            onCreateCategory = { newCategory ->
                coroutineScope.launch { taskRepository.insertCategory(newCategory) }
            },
            onDismiss = { editingTask = null },
            onTaskUpdated = { updatedTask ->
                onTaskUpdatedHelper(updatedTask)
                editingTask = null
            },
            onDeleteTask = { taskToDelete ->
                onDeleteHelper(taskToDelete)
                editingTask = null
            }
        )
    }

    // "View All" Modal Bottom Sheet
    if (showViewAllSheet) {
        AllTasksBottomSheet(
            taskList = sortedTaskList,
            categoriesList = categoriesList,
            onDismiss = { showViewAllSheet = false },
            onToggleComplete = onToggleCompleteHelper,
            onEditTask = { taskToEdit ->
                editingTask = taskToEdit
            },
            onDelete = onDeleteHelper,
            onAddNewTask = {
                showViewAllSheet = false
                prefilledTaskCategory = null
                showAddTaskSheet = true
            }
        )
    }

    // Timezone & World Cities Picker Sheet
    if (showTimezonePickerSheet) {
        TimezonePickerBottomSheet(
            onDismiss = { showTimezonePickerSheet = false }
        )
    }

    // TaskFlow Intelligence Modal Sheet (Small LLM & Schedule Optimizer)
    if (showIntelligenceSheet) {
        TaskFlowIntelligenceSheet(
            tasks = taskList,
            categories = categoriesList,
            initialTab = intelligenceInitialTab,
            onDismiss = {
                showIntelligenceSheet = false
                intelligenceInitialTab = 0
            },
            onApplySchedule = { slots ->
                coroutineScope.launch {
                    val todayEpoch = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
                    slots.forEach { slot ->
                        if (slot.isTimeChanged) {
                            taskRepository.updateTaskSchedule(
                                taskId = slot.taskId,
                                time = slot.suggestedTime,
                                date = "Today",
                                epochDay = todayEpoch
                            )
                        }
                    }
                }
            },
            onApplyPriorities = { prioritiesMap ->
                coroutineScope.launch {
                    taskRepository.updateTasksPriorities(prioritiesMap)
                }
            }
        )
    }

    // In-App Floating AI Assistant Shortcut Button (Active when enabled without system overlay)
    val isFloatingAiActive = com.example.todo_list.ai.FloatingAiButtonManager.isFloatingEnabled
    val hasSystemOverlay = remember(isFloatingAiActive) {
        com.example.todo_list.ai.FloatingAiButtonManager.canDrawOverlays(context)
    }
    if (isFloatingAiActive && !hasSystemOverlay) {
        com.example.todo_list.ui.components.ai.InAppFloatingAiButton {
            intelligenceInitialTab = 0
            showIntelligenceSheet = true
        }
    }
}


@Composable
fun IntelligenceInsightCard(
    pendingTasksCount: Int,
    onOptimizeClick: () -> Unit
) {
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles
    val typography = TFTheme.typography
    val shape = TFShape.card

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(hairline(), accentRoles.accent.copy(alpha = 0.30f), shape)
            .clickable { onOptimizeClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentRoles.accentContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "TaskFlow Intelligence",
                        tint = accentRoles.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TaskFlow Intelligence",
                            style = typography.headline,
                            color = colors.labelPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (pendingTasksCount > 0)
                            "Auto-prioritize & optimize your $pendingTasksCount pending tasks"
                        else
                            "Daily schedule balanced • Tap to optimize",
                        style = typography.footnote,
                        color = colors.labelSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "Plan ›",
                style = typography.subheadline,
                fontWeight = FontWeight.SemiBold,
                color = accentRoles.accentText
            )
        }
    }
}

@Composable
fun HeaderBar(
    avatarUrl: String = UserProfileManager.getAvatarUrl(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddTaskClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onIntelligenceClick: () -> Unit = {}
) {
    Surface(
        color = SystemSurface.copy(alpha = 0.95f),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // iOS Header Row (Large Title + Actions)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tasks",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary,
                    letterSpacing = (-0.5).sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val context = LocalContext.current
                    // Apple Intelligence Sparkle Button
                    IconButton(
                        onClick = {
                            HapticManager.performClick(context)
                            onIntelligenceClick()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "TaskFlow Intelligence",
                            tint = SystemBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            HapticManager.performClick(context)
                            onAddTaskClick()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = SystemBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(0.5.dp, SystemDivider, CircleShape)
                            .clickable {
                                HapticManager.performClick(context)
                                onProfileClick()
                            }
                    )
                }
            }

            // iOS Search Bar
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SearchInputBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                val context = LocalContext.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SystemLabelSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search",
                                fontSize = 16.sp,
                                color = SystemLabelSecondary
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                color = SystemLabelPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                HapticManager.performClick(context)
                                onSearchQueryChange("")
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = SystemLabelSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBentoCard(
    remainingTasks: Int,
    progressRatio: Float
) {
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles
    val typography = TFTheme.typography

    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = TFMotion.standard(),
        label = "ProgressAnimation"
    )

    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val currentDateStr = remember { dateFormat.format(Date()) }

    val shape = TFShape.card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .then(
                if (colors.isDark) {
                    Modifier.border(hairline(), colors.cardStroke, shape)
                } else {
                    Modifier
                }
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today",
                    style = typography.headline,
                    color = colors.labelPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentDateStr,
                    style = typography.subheadline,
                    color = colors.labelSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (remainingTasks == 0 && progressRatio > 0f) "All done 🎉" else "$remainingTasks remaining",
                    style = typography.title1,
                    color = if (remainingTasks == 0 && progressRatio > 0f) colors.green else accentRoles.accent
                )
            }

            // 72dp Animated Circular Progress Ring
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(72.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    // Track
                    drawArc(
                        color = accentRoles.accent.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    // Progress Arc
                    drawArc(
                        color = if (remainingTasks == 0 && progressRatio > 0f) colors.green else accentRoles.accent,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = typography.footnote,
                    fontWeight = FontWeight.Bold,
                    color = colors.labelPrimary
                )
            }
        }
    }
}

@Composable
fun TodaysTasksHeader(
    totalCount: Int,
    onViewAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Today's Tasks",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SystemBlueLight)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$totalCount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemBlue
                )
            }
        }

        TextButton(onClick = onViewAllClick) {
            Text(
                text = "View All",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SystemBlue
            )
        }
    }
}

@Composable
fun TaskCardItem(
    task: TaskItem,
    showDivider: Boolean = true,
    onToggleComplete: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // iOS Circular Checkbox Toggle - Toggles completion ONLY when explicitly clicked!
            IosCircularCheckbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete(task) }
            )

            // Content Area - Opens Edit Screen when clicked!
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEditTask(task) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (task.isCompleted) SystemLabelTertiary else SystemLabelPrimary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = TimeFormatHelper.formatTimeForDisplay(task.time, TimePreferencesManager.is24HourFormat),
                        fontSize = 14.sp,
                        color = if (task.isCompleted) SystemLabelTertiary else SystemLabelSecondary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = task.category,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (task.isCompleted) SystemLabelTertiary else SystemBlue
                    )
                    Text(
                        text = "• ${task.date}",
                        fontSize = 13.sp,
                        color = if (task.isCompleted) SystemLabelTertiary else SystemLabelSecondary
                    )

                    if (task.priority != "NONE") {
                        val prio = task.getPriorityEnum()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(prio.badgeBgHex))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = prio.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(prio.colorHex)
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = { onDelete(task) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Task",
                    tint = SystemGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = SystemDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 54.dp)
            )
        }
    }
}

@Composable
fun IosCircularCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (checked) SystemBlue else Color.Transparent)
            .border(
                width = if (checked) 0.dp else 1.5.dp,
                color = if (checked) Color.Transparent else SystemGray2,
                shape = CircleShape
            )
            .clickable {
                if (!checked) {
                    HapticManager.performSuccess(context)
                } else {
                    HapticManager.performClick(context)
                }
                onCheckedChange(!checked)
            }
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Completed",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// "View All" Sheet with Dynamic Categories
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTasksBottomSheet(
    taskList: List<TaskItem>,
    categoriesList: List<TaskListCategory> = TaskListCategory.DEFAULT_CATEGORIES,
    onDismiss: () -> Unit,
    onToggleComplete: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
    onAddNewTask: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filterOptions = remember(categoriesList) {
        listOf("All", "Pending", "Completed") + categoriesList.map { it.name }
    }

    val filteredList = taskList.filter { task ->
        val matchesSearch = task.title.contains(searchQuery, ignoreCase = true) ||
                task.category.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "All" -> true
            "Pending" -> !task.isCompleted
            "Completed" -> task.isCompleted
            else -> task.category.equals(selectedFilter, ignoreCase = true)
        }
        matchesSearch && matchesFilter
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemGroupedBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "All Tasks",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemLabelPrimary
                    )
                    Text(
                        text = "${taskList.size} total tasks • ${taskList.count { !it.isCompleted }} pending",
                        fontSize = 13.sp,
                        color = SystemLabelSecondary
                    )
                }

                Button(
                    onClick = onAddNewTask,
                    colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search tasks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SystemLabelSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = SystemLabelSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = SystemSurface,
                    focusedContainerColor = SystemSurface,
                    unfocusedBorderColor = SystemDivider,
                    focusedBorderColor = SystemBlue
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                filterOptions.forEach { option ->
                    val isSelected = selectedFilter == option
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = option },
                        label = {
                            Text(
                                text = option,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SystemBlue,
                            selectedLabelColor = Color.White,
                            containerColor = SystemSurface,
                            labelColor = SystemLabelSecondary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredList.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = SystemBlue,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No tasks found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = SystemLabelPrimary
                        )
                        Text(
                            text = "Try changing your search or category filter",
                            fontSize = 13.sp,
                            color = SystemLabelSecondary
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SystemSurface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredList, key = { it.id }) { task ->
                            TaskCardItem(
                                task = task,
                                onToggleComplete = onToggleComplete,
                                onEditTask = {
                                    onDismiss()
                                    onEditTask(it)
                                },
                                onDelete = onDelete
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PlaceholderScreen(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = SystemBlue,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary
            )
            Text(
                text = "$title section features are ready",
                fontSize = 14.sp,
                color = SystemLabelSecondary
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = SystemSurface.copy(alpha = 0.95f),
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Filled.Home,
                label = "Home",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            NavItem(
                icon = Icons.Outlined.DateRange,
                label = "Calendar",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
            NavItem(
                icon = Icons.Outlined.Category,
                label = "Lists",
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
            NavItem(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                isSelected = selectedTab == 3,
                onClick = { onTabSelected(3) }
            )
        }
    }
}

@Composable
fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val activeColor = SystemBlue
    val inactiveColor = SystemGray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(CircleShape)
            .clickable {
                HapticManager.performClick(context)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor
        )
    }
}
