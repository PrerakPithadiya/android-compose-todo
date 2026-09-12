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
import com.example.todo_list.ui.components.ai.TaskFlowIntelligenceSheet
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
                    HeaderBar(
                        avatarUrl = avatarUrl,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onAddTaskClick = { showAddTaskSheet = true },
                        onProfileClick = { showProfileScreen = true },
                        onIntelligenceClick = { showIntelligenceSheet = true }
                    )
                }
            },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        HapticManager.performClick(context)
                        showAddTaskSheet = true
                    },
                    containerColor = SystemBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = SystemGroupedBackground
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
            0 -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 12.dp,
                        bottom = innerPadding.calculateBottomPadding() + 88.dp,
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
                        // iOS Inset Grouped List Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SystemSurface,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (filteredTaskList.isEmpty()) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp)
                                    ) {
                                        Text(
                                            text = "No tasks found",
                                            fontSize = 15.sp,
                                            color = SystemLabelSecondary
                                        )
                                    }
                                } else {
                                    filteredTaskList.forEachIndexed { index, task ->
                                        key(task.id) {
                                            TaskCardItem(
                                                task = task,
                                                showDivider = index < filteredTaskList.size - 1,
                                                onToggleComplete = onToggleCompleteHelper,
                                                onEditTask = { taskToEdit ->
                                                    editingTask = taskToEdit
                                                },
                                                onDelete = onDeleteHelper
                                            )
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
            onDismiss = { showIntelligenceSheet = false },
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
}

@Composable
fun IntelligenceInsightCard(
    pendingTasksCount: Int,
    onOptimizeClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SystemSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOptimizeClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x1A007AFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "TaskFlow Intelligence",
                        tint = SystemBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TaskFlow Intelligence",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SystemLabelPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x1A007AFF))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "AI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemBlue
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (pendingTasksCount > 0)
                            "Auto-prioritize & optimize your $pendingTasksCount pending tasks"
                        else
                            "Daily schedule planned • Tap to ask questions",
                        fontSize = 13.sp,
                        color = SystemLabelSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "Plan ➔",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SystemBlue
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
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(durationMillis = 800),
        label = "ProgressAnimation"
    )

    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val currentDateStr = remember { dateFormat.format(Date()) }

    Surface(
        shape = RoundedCornerShape(12.dp),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (remainingTasks == 0) "All tasks completed!" else "$remainingTasks tasks left for today",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SystemLabelPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentDateStr,
                    fontSize = 14.sp,
                    color = SystemLabelSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(SystemGray6)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(CircleShape)
                            .background(SystemBlue)
                    )
                }

                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SystemBlue
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

// Analog Clock Time Picker Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalogClockPickerDialog(
    initialTime: String = if (TimePreferencesManager.is24HourFormat) "09:00" else "09:00 AM",
    is24Hour: Boolean = TimePreferencesManager.is24HourFormat,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val (parsedHour, parsedMinute) = remember(initialTime) {
        TimeFormatHelper.parseHourMinute(initialTime)
    }
    val timePickerState = rememberTimePickerState(
        initialHour = parsedHour,
        initialMinute = parsedMinute,
        is24Hour = is24Hour
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    val formattedTime = TimeFormatHelper.formatHourMinute(hour, minute, is24Hour)
                    onConfirm(formattedTime)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Confirm Time", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SystemLabelSecondary)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Outlined.Schedule, contentDescription = null, tint = SystemBlue)
                Text(
                    text = "Select Time",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )
            }
        },
        text = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = SystemGray5,
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = SystemLabelPrimary,
                        selectorColor = SystemBlue,
                        periodSelectorSelectedContainerColor = SystemBlueLight,
                        periodSelectorSelectedContentColor = SystemBlue
                    )
                )
            }
        },
        containerColor = SystemGroupedBackground,
        shape = RoundedCornerShape(28.dp)
    )
}

// Material 3 Date Picker Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val formattedDate = formatter.format(Date(selectedMillis))
                    val epochDay = selectedMillis / (1000 * 60 * 60 * 24)
                    onConfirm(formattedDate, epochDay)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Confirm Date", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SystemLabelSecondary)
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = SystemBlue,
                todayDateBorderColor = SystemBlue
            )
        )
    }
}

// Create Task Sheet with Date Selector + Analog Clock Selector
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskBottomSheet(
    initialCategory: String = "Work",
    categoriesList: List<TaskListCategory> = TaskListCategory.DEFAULT_CATEGORIES,
    onCreateCategory: (TaskListCategory) -> Unit = {},
    onDismiss: () -> Unit,
    onTaskCreated: (TaskItem) -> Unit
) {
    val context = LocalContext.current
    val currentEpochDay = remember { System.currentTimeMillis() / (1000 * 60 * 60 * 24) }

    val is24Hour = TimePreferencesManager.is24HourFormat
    var taskTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedDate by remember { mutableStateOf("Today") }
    var selectedEpochDay by remember { mutableStateOf(currentEpochDay) }
    var selectedTime by remember(is24Hour) { mutableStateOf(if (is24Hour) "17:00" else "05:00 PM") }
    var selectedPriority by remember { mutableStateOf("NONE") }
    var titleError by remember { mutableStateOf(false) }

    var showAnalogClock by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCreateCategorySheet by remember { mutableStateOf(false) }

    val quickTimes = remember(is24Hour) { TimeFormatHelper.getQuickTimes(is24Hour) }

    if (showCreateCategorySheet) {
        CreateCategoryBottomSheet(
            onDismiss = { showCreateCategorySheet = false },
            onCreateCategory = { newCategory ->
                onCreateCategory(newCategory)
                selectedCategory = newCategory.name
                showCreateCategorySheet = false
            }
        )
    }

    if (showAnalogClock) {
        AnalogClockPickerDialog(
            initialTime = selectedTime,
            is24Hour = is24Hour,
            onDismiss = { showAnalogClock = false },
            onConfirm = { pickedTime ->
                selectedTime = pickedTime
                showAnalogClock = false
            }
        )
    }

    if (showDatePicker) {
        MaterialDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { formattedDate, epochDay ->
                selectedDate = formattedDate
                selectedEpochDay = epochDay
                showDatePicker = false
            }
        )
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
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Create New Task",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemLabelPrimary
                    )
                    Text(
                        text = "Set date, time & category to organize",
                        fontSize = 13.sp,
                        color = SystemLabelSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SystemGray5)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Close",
                        tint = SystemLabelSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TASK DESCRIPTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = {
                        taskTitle = it
                        if (it.isNotBlank()) titleError = false
                    },
                    placeholder = { Text("What needs to be done?", color = SystemLabelSecondary.copy(alpha = 0.6f)) },
                    isError = titleError,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = SystemSurface,
                        focusedContainerColor = SystemSurface,
                        unfocusedBorderColor = SystemDivider,
                        focusedBorderColor = SystemBlue,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (titleError) {
                    Text(
                        text = "Please enter a task description",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CATEGORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    categoriesList.forEach { catItem ->
                        val isSelected = selectedCategory.equals(catItem.name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) SystemBlue else SystemSurface,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, SystemDivider),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    HapticManager.performClick(context)
                                    selectedCategory = catItem.name
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(catItem.getEmoji(), fontSize = 14.sp)
                                Text(
                                    text = catItem.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else SystemLabelPrimary
                                )
                            }
                        }
                    }

                    // + New Category Chip Button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SystemBlue.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SystemBlue.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                HapticManager.performClick(context)
                                showCreateCategorySheet = true
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Category",
                                tint = SystemBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "New Category",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SystemBlue
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SCHEDULE DATE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedDate == "Today",
                        onClick = {
                            selectedDate = "Today"
                            selectedEpochDay = currentEpochDay
                        },
                        label = { Text("Today", fontSize = 12.sp) },
                        shape = RoundedCornerShape(16.dp)
                    )

                    FilterChip(
                        selected = selectedDate == "Tomorrow",
                        onClick = {
                            selectedDate = "Tomorrow"
                            selectedEpochDay = currentEpochDay + 1
                        },
                        label = { Text("Tomorrow", fontSize = 12.sp) },
                        shape = RoundedCornerShape(16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SystemSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SystemBlue.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showDatePicker = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = SystemBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (selectedDate != "Today" && selectedDate != "Tomorrow") selectedDate else "Calendar 📅",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemBlue
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SCHEDULE TIME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    quickTimes.forEach { qTime ->
                        val isSelected = selectedTime == qTime
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTime = qTime },
                            label = { Text(qTime, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemBlueLight,
                                selectedLabelColor = SystemBlue,
                                selectedLeadingIconColor = SystemBlue
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SystemSurface,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SystemBlue.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showAnalogClock = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = SystemBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Selected: $selectedTime",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SystemLabelPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SystemBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Analog Clock 🕒",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Priority Selection Row
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PRIORITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        com.example.todo_list.model.TaskPriority.NONE to "None",
                        com.example.todo_list.model.TaskPriority.LOW to "Low",
                        com.example.todo_list.model.TaskPriority.MEDIUM to "Medium",
                        com.example.todo_list.model.TaskPriority.HIGH to "High"
                    ).forEach { (prioEnum, label) ->
                        val isSelected = selectedPriority == prioEnum.key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPriority = prioEnum.key },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (prioEnum == com.example.todo_list.model.TaskPriority.NONE) SystemGray5 else Color(prioEnum.badgeBgHex),
                                selectedLabelColor = if (prioEnum == com.example.todo_list.model.TaskPriority.NONE) SystemLabelPrimary else Color(prioEnum.colorHex)
                            )
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (taskTitle.isBlank()) {
                        titleError = true
                    } else {
                        val newId = System.currentTimeMillis().toString()
                        onTaskCreated(
                            TaskItem(
                                id = newId,
                                title = taskTitle.trim(),
                                category = selectedCategory,
                                date = selectedDate,
                                time = selectedTime.ifBlank { "12:00 PM" },
                                isCompleted = false,
                                epochDay = selectedEpochDay,
                                priority = selectedPriority
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create Task",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Edit Task Bottom Sheet - Opens when tapping task text/details!
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskBottomSheet(
    task: TaskItem,
    categoriesList: List<TaskListCategory> = TaskListCategory.DEFAULT_CATEGORIES,
    onCreateCategory: (TaskListCategory) -> Unit = {},
    onDismiss: () -> Unit,
    onTaskUpdated: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit
) {
    val context = LocalContext.current
    val currentEpochDay = remember { System.currentTimeMillis() / (1000 * 60 * 60 * 24) }

    val is24Hour = TimePreferencesManager.is24HourFormat
    var taskTitle by remember { mutableStateOf(task.title) }
    var selectedCategory by remember { mutableStateOf(task.category) }
    var selectedDate by remember { mutableStateOf(task.date) }
    var selectedEpochDay by remember { mutableStateOf(task.epochDay.takeIf { it > 0 } ?: currentEpochDay) }
    var selectedTime by remember(task.time, is24Hour) {
        mutableStateOf(TimeFormatHelper.formatTimeForDisplay(task.time, is24Hour))
    }
    var selectedPriority by remember(task.priority) { mutableStateOf(task.priority) }
    var titleError by remember { mutableStateOf(false) }

    var showAnalogClock by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCreateCategorySheet by remember { mutableStateOf(false) }

    val quickTimes = remember(is24Hour) { TimeFormatHelper.getQuickTimes(is24Hour) }

    if (showCreateCategorySheet) {
        CreateCategoryBottomSheet(
            onDismiss = { showCreateCategorySheet = false },
            onCreateCategory = { newCategory ->
                onCreateCategory(newCategory)
                selectedCategory = newCategory.name
                showCreateCategorySheet = false
            }
        )
    }

    if (showAnalogClock) {
        AnalogClockPickerDialog(
            initialTime = selectedTime,
            is24Hour = is24Hour,
            onDismiss = { showAnalogClock = false },
            onConfirm = { pickedTime ->
                selectedTime = pickedTime
                showAnalogClock = false
            }
        )
    }

    if (showDatePicker) {
        MaterialDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { formattedDate, epochDay ->
                selectedDate = formattedDate
                selectedEpochDay = epochDay
                showDatePicker = false
            }
        )
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
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Sheet Header with Delete Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit Task",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemLabelPrimary
                    )
                    Text(
                        text = "Modify task name, category, date or time",
                        fontSize = 13.sp,
                        color = SystemLabelSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { onDeleteTask(task) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SystemRed.copy(alpha = 0.1f))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Task",
                            tint = SystemRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SystemGray5)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close",
                            tint = SystemLabelSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Task Description Input Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TASK DESCRIPTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = {
                        taskTitle = it
                        if (it.isNotBlank()) titleError = false
                    },
                    placeholder = { Text("Task description...", color = SystemLabelSecondary.copy(alpha = 0.6f)) },
                    isError = titleError,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = SystemSurface,
                        focusedContainerColor = SystemSurface,
                        unfocusedBorderColor = SystemDivider,
                        focusedBorderColor = SystemBlue,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (titleError) {
                    Text(
                        text = "Task description cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }

            // Category Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CATEGORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    categoriesList.forEach { catItem ->
                        val isSelected = selectedCategory.equals(catItem.name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) SystemBlue else SystemSurface,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, SystemDivider),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    HapticManager.performClick(context)
                                    selectedCategory = catItem.name
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(catItem.getEmoji(), fontSize = 14.sp)
                                Text(
                                    text = catItem.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else SystemLabelPrimary
                                )
                            }
                        }
                    }

                    // + New Category Chip Button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SystemBlue.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SystemBlue.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                HapticManager.performClick(context)
                                showCreateCategorySheet = true
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Category",
                                tint = SystemBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "New Category",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SystemBlue
                            )
                        }
                    }
                }
            }

            // Schedule Date Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SCHEDULE DATE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedDate == "Today",
                        onClick = {
                            selectedDate = "Today"
                            selectedEpochDay = currentEpochDay
                        },
                        label = { Text("Today", fontSize = 12.sp) },
                        shape = RoundedCornerShape(16.dp)
                    )

                    FilterChip(
                        selected = selectedDate == "Tomorrow",
                        onClick = {
                            selectedDate = "Tomorrow"
                            selectedEpochDay = currentEpochDay + 1
                        },
                        label = { Text("Tomorrow", fontSize = 12.sp) },
                        shape = RoundedCornerShape(16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SystemSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SystemBlue.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showDatePicker = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = SystemBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (selectedDate != "Today" && selectedDate != "Tomorrow") selectedDate else "Calendar 📅",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemBlue
                            )
                        }
                    }
                }
            }

            // Schedule Time Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SCHEDULE TIME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    quickTimes.forEach { qTime ->
                        val isSelected = selectedTime == qTime
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTime = qTime },
                            label = { Text(qTime, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemBlueLight,
                                selectedLabelColor = SystemBlue,
                                selectedLeadingIconColor = SystemBlue
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SystemSurface,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SystemBlue.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showAnalogClock = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = SystemBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Selected: $selectedTime",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SystemLabelPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SystemBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Analog Clock 🕒",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Priority Selection Row
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PRIORITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        com.example.todo_list.model.TaskPriority.NONE to "None",
                        com.example.todo_list.model.TaskPriority.LOW to "Low",
                        com.example.todo_list.model.TaskPriority.MEDIUM to "Medium",
                        com.example.todo_list.model.TaskPriority.HIGH to "High"
                    ).forEach { (prioEnum, label) ->
                        val isSelected = selectedPriority == prioEnum.key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPriority = prioEnum.key },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (prioEnum == com.example.todo_list.model.TaskPriority.NONE) SystemGray5 else Color(prioEnum.badgeBgHex),
                                selectedLabelColor = if (prioEnum == com.example.todo_list.model.TaskPriority.NONE) SystemLabelPrimary else Color(prioEnum.colorHex)
                            )
                        )
                    }
                }
            }

            // Save Changes Button
            Button(
                onClick = {
                    if (taskTitle.isBlank()) {
                        titleError = true
                    } else {
                        onTaskUpdated(
                            task.copy(
                                title = taskTitle.trim(),
                                category = selectedCategory,
                                date = selectedDate,
                                time = selectedTime.ifBlank { "12:00 PM" },
                                epochDay = selectedEpochDay,
                                priority = selectedPriority
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Changes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
