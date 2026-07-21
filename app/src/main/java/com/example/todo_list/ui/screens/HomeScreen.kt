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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
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
import com.example.todo_list.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var showViewAllSheet by remember { mutableStateOf(false) }

    var taskList by remember {
        mutableStateOf(
            listOf(
                TaskItem("1", "Review design specs", "Work", "09:30 AM", false),
                TaskItem("2", "Team sync at 2 PM", "Work", "02:00 PM", false),
                TaskItem("3", "Grocery shopping", "Personal", "11:00 AM", true),
                TaskItem("4", "Update project timeline", "Work", "04:30 PM", false),
                TaskItem("5", "Prepare quarterly report", "Work", "06:00 PM", false),
                TaskItem("6", "Evening gym session", "Personal", "07:30 PM", false)
            )
        )
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val totalTasks = taskList.size
    val completedTasks = taskList.count { it.isCompleted }
    val remainingTasks = totalTasks - completedTasks
    val progressRatio = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    Scaffold(
        topBar = {
            HeaderBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskSheet = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        when (selectedTab) {
            0 -> {
                // Home Screen Content
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 88.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        WelcomeSection()
                    }

                    item {
                        StatusBentoCard(
                            remainingTasks = remainingTasks,
                            progressRatio = progressRatio
                        )
                    }

                    item {
                        TodaysTasksHeader(
                            totalCount = taskList.size,
                            onViewAllClick = { showViewAllSheet = true }
                        )
                    }

                    items(taskList, key = { it.id }) { task ->
                        TaskCardItem(
                            task = task,
                            onToggleComplete = { toggledTask ->
                                taskList = taskList.map {
                                    if (it.id == toggledTask.id) it.copy(isCompleted = !it.isCompleted) else it
                                }
                            },
                            onDelete = { deletedTask ->
                                taskList = taskList.filter { it.id != deletedTask.id }
                            }
                        )
                    }
                }
            }
            1 -> PlaceholderScreen(title = "Calendar", icon = Icons.Outlined.DateRange)
            2 -> PlaceholderScreen(title = "Categories", icon = Icons.Outlined.Category)
            3 -> PlaceholderScreen(title = "Settings", icon = Icons.Outlined.Settings)
        }
    }

    // Modern Create Task Bottom Sheet
    if (showAddTaskSheet) {
        CreateTaskBottomSheet(
            onDismiss = { showAddTaskSheet = false },
            onTaskCreated = { newTask ->
                taskList = taskList + newTask
                showAddTaskSheet = false
                coroutineScope.launch {
                    listState.animateScrollToItem(taskList.size + 2)
                }
            }
        )
    }

    // "View All" Modal Bottom Sheet
    if (showViewAllSheet) {
        AllTasksBottomSheet(
            taskList = taskList,
            onDismiss = { showViewAllSheet = false },
            onToggleComplete = { toggledTask ->
                taskList = taskList.map {
                    if (it.id == toggledTask.id) it.copy(isCompleted = !it.isCompleted) else it
                }
            },
            onDelete = { deletedTask ->
                taskList = taskList.filter { it.id != deletedTask.id }
            },
            onAddNewTask = {
                showViewAllSheet = false
                showAddTaskSheet = true
            }
        )
    }
}

@Composable
fun HeaderBar() {
    Surface(
        color = BackgroundLight,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCoBuvdtAGCYMF3-7fgZnTDoXh_snzGoLq1GpOuiTnpGcrs_BUFGcSioVrt4viicVuGC9ZFj9lsxuXEX_szLOWSsdMaaKlMDlKGSJJcfKhyUthyTuHEgIgtHGDUcLm3JMsUS7KQWaovaJxSOD24Pd9PJg9MrXyDZLwQHIBQ2P-6aUTKoBr9ebkyIWiEcuxV_wt95LmVK_nmu9kMlfXd-pio3PZqk-2_pF6npzfw8BXNvJ6lIacTKXE",
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .border(1.dp, OutlineVariant, CircleShape)
                )

                Text(
                    text = "TaskFlow",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }

            IconButton(
                onClick = { },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Calendar",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun WelcomeSection() {
    Column {
        Text(
            text = "Hello, Alex",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackgroundDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Wednesday, October 25",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = OnSurfaceVariant
        )
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

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryFixedBlue),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PrimaryBlue.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnPrimaryFixedBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (remainingTasks == 0) "Great job finishing everything 🎉" else "You're doing great!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnPrimaryFixedVariantBlue
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(64.dp)
            ) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    val strokeWidth = 6.dp.toPx()
                    // Background Ring
                    drawCircle(
                        color = OnPrimaryFixedBlue.copy(alpha = 0.15f),
                        style = Stroke(width = strokeWidth)
                    )
                    // Progress Arc
                    drawArc(
                        color = PrimaryBlue,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnPrimaryFixedBlue
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
        modifier = Modifier.fillMaxWidth(),
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
                color = OnBackgroundDark
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SecondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$totalCount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSecondaryContainer
                )
            }
        }

        TextButton(onClick = onViewAllClick) {
            Text(
                text = "View All",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        }
    }
}

@Composable
fun TaskCardItem(
    task: TaskItem,
    onToggleComplete: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit
) {
    val alphaModifier = if (task.isCompleted) 0.6f else 1.0f

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alphaModifier)
            .clickable { onToggleComplete(task) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete(task) },
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryBlue,
                    uncheckedColor = Outline
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnBackgroundDark,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tag Badge
                    val (tagBg, tagText) = if (task.category.equals("Personal", ignoreCase = true)) {
                        TertiaryContainerSoft to OnTertiaryFixedVariant
                    } else {
                        SecondaryContainer to OnSecondaryContainer
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(tagBg)
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = task.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = tagText
                        )
                    }

                    // Schedule Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = "Schedule",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = task.time,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = { onDelete(task) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Task",
                    tint = OnSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Fixed & Scrollable Filter Chips + Premium Sheet Layout
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTasksBottomSheet(
    taskList: List<TaskItem>,
    onDismiss: () -> Unit,
    onToggleComplete: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
    onAddNewTask: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Pending", "Completed", "Work", "Personal"

    val filteredList = taskList.filter { task ->
        val matchesSearch = task.title.contains(searchQuery, ignoreCase = true) ||
                task.category.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "Pending" -> !task.isCompleted
            "Completed" -> task.isCompleted
            "Work" -> task.category.equals("Work", ignoreCase = true)
            "Personal" -> task.category.equals("Personal", ignoreCase = true)
            else -> true
        }
        matchesSearch && matchesFilter
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BackgroundLight,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
        ) {
            // Sheet Top Bar
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
                        color = OnBackgroundDark
                    )
                    Text(
                        text = "${taskList.size} total tasks • ${taskList.count { !it.isCompleted }} pending",
                        fontSize = 13.sp,
                        color = OnSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddNewTask,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar with Clear Button
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search tasks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OnSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = OnSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = SurfaceContainerLow,
                    focusedContainerColor = SurfaceContainerLow,
                    unfocusedBorderColor = OutlineVariant,
                    focusedBorderColor = PrimaryBlue
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Filter Chips (Fixed UI bug: wraps in horizontalScroll to prevent vertical letter squeezing)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                val filterOptions = listOf("All", "Pending", "Completed", "Work", "Personal")
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
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceContainerLow,
                            labelColor = OnSurfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task List
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
                            tint = PrimaryBlue,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No tasks found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnBackgroundDark
                        )
                        Text(
                            text = "Try changing your search or filter",
                            fontSize = 13.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredList, key = { it.id }) { task ->
                        TaskCardItem(
                            task = task,
                            onToggleComplete = onToggleComplete,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }
}

// Stunning Modern Create Task Bottom Sheet (Replaces old small unappealing Dialog)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskBottomSheet(
    onDismiss: () -> Unit,
    onTaskCreated: (TaskItem) -> Unit
) {
    var taskTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Work") }
    var selectedTime by remember { mutableStateOf("05:00 PM") }
    var titleError by remember { mutableStateOf(false) }

    val quickTimes = listOf("09:00 AM", "11:30 AM", "02:00 PM", "05:00 PM", "08:00 PM")
    val categories = listOf("Work" to "💼", "Personal" to "👤", "Health" to "🏋️", "Study" to "📚")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BackgroundLight,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Sheet Header
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
                        color = OnBackgroundDark
                    )
                    Text(
                        text = "Add details to stay organized",
                        fontSize = 13.sp,
                        color = OnSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Close",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Task Description Input Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TASK DESCRIPTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = {
                        taskTitle = it
                        if (it.isNotBlank()) titleError = false
                    },
                    placeholder = { Text("What needs to be done?", color = OnSurfaceVariant.copy(alpha = 0.6f)) },
                    isError = titleError,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = SurfaceContainerLow,
                        focusedContainerColor = SurfaceContainerLow,
                        unfocusedBorderColor = OutlineVariant,
                        focusedBorderColor = PrimaryBlue,
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

            // Category Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CATEGORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    categories.forEach { (cat, emoji) ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) PrimaryBlue else SurfaceContainerLow,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedCategory = cat }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(emoji, fontSize = 14.sp)
                                Text(
                                    text = cat,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else OnBackgroundDark
                                )
                            }
                        }
                    }
                }
            }

            // Time Selection & Quick Times
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SCHEDULE TIME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                // Quick time chips
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
                                selectedContainerColor = SecondaryContainer,
                                selectedLabelColor = OnSecondaryContainer,
                                selectedLeadingIconColor = OnSecondaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // Custom Time Input
                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = { selectedTime = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = SurfaceContainerLow,
                        focusedContainerColor = SurfaceContainerLow,
                        unfocusedBorderColor = OutlineVariant,
                        focusedBorderColor = PrimaryBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Submit Button
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
                                time = selectedTime.ifBlank { "12:00 PM" },
                                isCompleted = false
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
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
                tint = PrimaryBlue,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackgroundDark
            )
            Text(
                text = "$title section features are ready",
                fontSize = 14.sp,
                color = OnSurfaceVariant
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
        color = SurfaceContainer,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Tab
            NavItem(
                icon = Icons.Filled.Home,
                label = "Home",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            // Calendar Tab
            NavItem(
                icon = Icons.Outlined.DateRange,
                label = "Calendar",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
            // Categories Tab
            NavItem(
                icon = Icons.Outlined.Category,
                label = "Categories",
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
            // Settings Tab
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (isSelected) Modifier.background(SecondaryContainer) else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) OnSecondaryContainer else OnSecondaryFixedVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) OnSecondaryContainer else OnSecondaryFixedVariant
        )
    }
}
