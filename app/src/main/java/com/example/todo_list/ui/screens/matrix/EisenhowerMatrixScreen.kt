package com.example.todo_list.ui.screens.matrix

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.model.EisenhowerQuadrant
import com.example.todo_list.model.TaskItem
import com.example.todo_list.ui.components.primitives.TFCardGroup
import com.example.todo_list.ui.components.primitives.TFEmptyState
import com.example.todo_list.ui.components.primitives.TFGroupDivider
import com.example.todo_list.ui.components.primitives.TFSegmentedControl
import com.example.todo_list.ui.components.primitives.TFTaskRow
import com.example.todo_list.ui.theme.*

/**
 * Eisenhower Matrix Prioritization Screen (Apple iOS HIG Inset Grouped Architecture).
 *
 * View Modes:
 * 1. 2x2 Quadrant Grid: Overview of all 4 actionable quadrants with live counts & preview items.
 * 2. Segmented Focused List: Deep dive into individual quadrants (Do First, Schedule, Delegate, Eliminate)
 *    with instant task completion, quadrant reassignment, and one-tap Pomodoro timer launch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EisenhowerMatrixScreen(
    taskList: List<TaskItem>,
    onToggleComplete: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onQuadrantChange: (TaskItem, EisenhowerQuadrant) -> Unit,
    onStartPomodoro: (TaskItem) -> Unit,
    onNavigateBack: () -> Unit,
    onAddTask: (EisenhowerQuadrant) -> Unit
) {
    val context = LocalContext.current
    val colors = TFTheme.colors
    val typography = TFTheme.typography
    val accentRoles = TFTheme.accentRoles

    // 0: 2x2 Grid, 1: Do First, 2: Schedule, 3: Delegate, 4: Eliminate
    var selectedViewModeIndex by remember { mutableIntStateOf(0) }
    var taskToMoveQuadrant by remember { mutableStateOf<TaskItem?>(null) }

    // Segment tasks into 4 quadrants
    val doFirstTasks = remember(taskList) {
        taskList.filter { it.getEisenhowerQuadrantEnum() == EisenhowerQuadrant.DO_FIRST }
    }
    val scheduleTasks = remember(taskList) {
        taskList.filter { it.getEisenhowerQuadrantEnum() == EisenhowerQuadrant.SCHEDULE }
    }
    val delegateTasks = remember(taskList) {
        taskList.filter { it.getEisenhowerQuadrantEnum() == EisenhowerQuadrant.DELEGATE }
    }
    val eliminateTasks = remember(taskList) {
        taskList.filter { it.getEisenhowerQuadrantEnum() == EisenhowerQuadrant.ELIMINATE }
    }

    // Handle system back button
    BackHandler(enabled = true) {
        when {
            taskToMoveQuadrant != null -> taskToMoveQuadrant = null
            else -> onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Eisenhower Matrix",
                            style = typography.headline,
                            color = colors.labelPrimary
                        )
                        Text(
                            text = "Prioritize by Urgency & Importance",
                            style = typography.caption,
                            color = colors.labelSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        TFHaptics.light(context)
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = accentRoles.accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.canvas
                )
            )
        },
        containerColor = colors.canvas
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Apple HIG Quadrant Filter & Mode Switcher Bar
            val matrixTabs = remember(doFirstTasks.size, scheduleTasks.size, delegateTasks.size, eliminateTasks.size) {
                listOf(
                    MatrixTabItem(
                        title = "Matrix 2x2",
                        icon = Icons.Outlined.GridView
                    ),
                    MatrixTabItem(
                        title = "Do First",
                        indicatorColor = Color(EisenhowerQuadrant.DO_FIRST.colorHex),
                        taskCount = doFirstTasks.count { !it.isCompleted }
                    ),
                    MatrixTabItem(
                        title = "Schedule",
                        indicatorColor = Color(EisenhowerQuadrant.SCHEDULE.colorHex),
                        taskCount = scheduleTasks.count { !it.isCompleted }
                    ),
                    MatrixTabItem(
                        title = "Delegate",
                        indicatorColor = Color(EisenhowerQuadrant.DELEGATE.colorHex),
                        taskCount = delegateTasks.count { !it.isCompleted }
                    ),
                    MatrixTabItem(
                        title = "Eliminate",
                        indicatorColor = Color(EisenhowerQuadrant.ELIMINATE.colorHex),
                        taskCount = eliminateTasks.count { !it.isCompleted }
                    )
                )
            }

            val tabListState = rememberLazyListState()
            LaunchedEffect(selectedViewModeIndex) {
                tabListState.animateScrollToItem(selectedViewModeIndex)
            }

            LazyRow(
                state = tabListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(matrixTabs) { index, tab ->
                    val isSelected = index == selectedViewModeIndex
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) colors.fillSelected else colors.fillControl,
                        border = if (isSelected && colors.isDark) BorderStroke(hairline(), colors.cardStroke) else null,
                        shadowElevation = if (isSelected && !colors.isDark) 2.dp else 0.dp,
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (index != selectedViewModeIndex) {
                                    TFHaptics.selection(context)
                                    selectedViewModeIndex = index
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (tab.icon != null) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) colors.labelPrimary else colors.labelSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            if (tab.indicatorColor != null) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(tab.indicatorColor)
                                )
                            }
                            Text(
                                text = tab.title,
                                style = typography.subheadline,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) colors.labelPrimary else colors.labelSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (tab.taskCount != null && tab.taskCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) colors.labelPrimary.copy(alpha = 0.12f)
                                            else colors.labelSecondary.copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab.taskCount.toString(),
                                        style = typography.caption,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.labelPrimary else colors.labelSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedViewModeIndex) {
                0 -> {
                    // 2x2 Overview Matrix
                    EisenhowerOverviewGrid(
                        doFirstTasks = doFirstTasks,
                        scheduleTasks = scheduleTasks,
                        delegateTasks = delegateTasks,
                        eliminateTasks = eliminateTasks,
                        onSelectQuadrant = { quadrantIndex ->
                            TFHaptics.light(context)
                            selectedViewModeIndex = quadrantIndex
                        },
                        onAddTask = onAddTask
                    )
                }
                1 -> QuadrantDetailedListView(
                    quadrant = EisenhowerQuadrant.DO_FIRST,
                    tasks = doFirstTasks,
                    onToggleComplete = onToggleComplete,
                    onEditTask = onEditTask,
                    onMoveQuadrantClick = { taskToMoveQuadrant = it },
                    onStartPomodoro = onStartPomodoro,
                    onAddTask = { onAddTask(EisenhowerQuadrant.DO_FIRST) }
                )
                2 -> QuadrantDetailedListView(
                    quadrant = EisenhowerQuadrant.SCHEDULE,
                    tasks = scheduleTasks,
                    onToggleComplete = onToggleComplete,
                    onEditTask = onEditTask,
                    onMoveQuadrantClick = { taskToMoveQuadrant = it },
                    onStartPomodoro = onStartPomodoro,
                    onAddTask = { onAddTask(EisenhowerQuadrant.SCHEDULE) }
                )
                3 -> QuadrantDetailedListView(
                    quadrant = EisenhowerQuadrant.DELEGATE,
                    tasks = delegateTasks,
                    onToggleComplete = onToggleComplete,
                    onEditTask = onEditTask,
                    onMoveQuadrantClick = { taskToMoveQuadrant = it },
                    onStartPomodoro = onStartPomodoro,
                    onAddTask = { onAddTask(EisenhowerQuadrant.DELEGATE) }
                )
                4 -> QuadrantDetailedListView(
                    quadrant = EisenhowerQuadrant.ELIMINATE,
                    tasks = eliminateTasks,
                    onToggleComplete = onToggleComplete,
                    onEditTask = onEditTask,
                    onMoveQuadrantClick = { taskToMoveQuadrant = it },
                    onStartPomodoro = onStartPomodoro,
                    onAddTask = { onAddTask(EisenhowerQuadrant.ELIMINATE) }
                )
            }
        }
    }

    // Modal Sheet to move task to another quadrant
    if (taskToMoveQuadrant != null) {
        val task = taskToMoveQuadrant!!
        ModalBottomSheet(
            onDismissRequest = { taskToMoveQuadrant = null },
            containerColor = colors.card
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Move Task Quadrant",
                    style = typography.headline,
                    color = colors.labelPrimary
                )
                Text(
                    text = task.title.lineSequence().firstOrNull() ?: "",
                    style = typography.subheadline,
                    color = colors.labelSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))

                TFCardGroup {
                    EisenhowerQuadrant.entries.forEachIndexed { index, quadrant ->
                        val isCurrent = task.getEisenhowerQuadrantEnum() == quadrant
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    TFHaptics.medium(context)
                                    onQuadrantChange(task, quadrant)
                                    taskToMoveQuadrant = null
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(quadrant.colorHex))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${quadrant.title} (${quadrant.actionLabel})",
                                        style = typography.body,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.labelPrimary
                                    )
                                    Text(
                                        text = quadrant.subtitle,
                                        style = typography.caption,
                                        color = colors.labelSecondary
                                    )
                                }
                            }
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "Current Quadrant",
                                    tint = Color(quadrant.colorHex)
                                )
                            }
                        }
                        if (index < EisenhowerQuadrant.entries.size - 1) {
                            TFGroupDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private data class MatrixTabItem(
    val title: String,
    val indicatorColor: Color? = null,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val taskCount: Int? = null
)

/**
 * 2x2 Overview Matrix Grid
 */
@Composable
private fun EisenhowerOverviewGrid(
    doFirstTasks: List<TaskItem>,
    scheduleTasks: List<TaskItem>,
    delegateTasks: List<TaskItem>,
    eliminateTasks: List<TaskItem>,
    onSelectQuadrant: (Int) -> Unit,
    onAddTask: (EisenhowerQuadrant) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row 1: Urgent (Do First & Delegate)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuadrantGridBentoCard(
                    quadrant = EisenhowerQuadrant.DO_FIRST,
                    tasks = doFirstTasks,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectQuadrant(1) },
                    onAddClick = { onAddTask(EisenhowerQuadrant.DO_FIRST) }
                )
                QuadrantGridBentoCard(
                    quadrant = EisenhowerQuadrant.SCHEDULE,
                    tasks = scheduleTasks,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectQuadrant(2) },
                    onAddClick = { onAddTask(EisenhowerQuadrant.SCHEDULE) }
                )
            }
        }

        // Row 2: Not Urgent (Schedule & Eliminate)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuadrantGridBentoCard(
                    quadrant = EisenhowerQuadrant.DELEGATE,
                    tasks = delegateTasks,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectQuadrant(3) },
                    onAddClick = { onAddTask(EisenhowerQuadrant.DELEGATE) }
                )
                QuadrantGridBentoCard(
                    quadrant = EisenhowerQuadrant.ELIMINATE,
                    tasks = eliminateTasks,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectQuadrant(4) },
                    onAddClick = { onAddTask(EisenhowerQuadrant.ELIMINATE) }
                )
            }
        }
    }
}

/**
 * Individual Quadrant Bento Card in 2x2 Grid View
 */
@Composable
private fun QuadrantGridBentoCard(
    quadrant: EisenhowerQuadrant,
    tasks: List<TaskItem>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val colors = TFTheme.colors
    val typography = TFTheme.typography
    val context = LocalContext.current

    val pendingCount = tasks.count { !it.isCompleted }
    val totalCount = tasks.size
    val qColor = Color(quadrant.colorHex)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(hairline(), colors.cardStroke, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(qColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = quadrant.title,
                        style = typography.subheadline,
                        fontWeight = FontWeight.Bold,
                        color = colors.labelPrimary
                    )
                }

                IconButton(
                    onClick = {
                        TFHaptics.light(context)
                        onAddClick()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        tint = qColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = quadrant.subtitle,
                style = typography.caption,
                color = colors.labelSecondary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "$pendingCount",
                style = typography.largeTitle.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                color = qColor
            )
            Text(
                text = if (totalCount == 0) "No tasks" else "$totalCount total",
                style = typography.caption,
                color = colors.labelSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Preview first 2 tasks
            val previewTasks = tasks.filter { !it.isCompleted }.take(2)
            if (previewTasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    previewTasks.forEach { task ->
                        Text(
                            text = "• ${task.title.lineSequence().firstOrNull() ?: ""}",
                            style = typography.caption,
                            color = colors.labelPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Detailed Inset-Grouped Task List for a Single Quadrant
 */
@Composable
private fun QuadrantDetailedListView(
    quadrant: EisenhowerQuadrant,
    tasks: List<TaskItem>,
    onToggleComplete: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onMoveQuadrantClick: (TaskItem) -> Unit,
    onStartPomodoro: (TaskItem) -> Unit,
    onAddTask: () -> Unit
) {
    val colors = TFTheme.colors
    val typography = TFTheme.typography
    val context = LocalContext.current
    val qColor = Color(quadrant.colorHex)

    val pendingTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quadrant Mission Header Card
        item(key = "header_mission_card") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(qColor.copy(alpha = 0.12f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${quadrant.title} • Action: ${quadrant.actionLabel}",
                            style = typography.headline,
                            color = qColor
                        )
                        Text(
                            text = quadrant.subtitle,
                            style = typography.subheadline,
                            color = colors.labelPrimary
                        )
                    }

                    Button(
                        onClick = {
                            TFHaptics.light(context)
                            onAddTask()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = qColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
        }

        // Active Tasks Inset Group
        item(key = "active_tasks_group") {
            if (pendingTasks.isEmpty()) {
                TFCardGroup {
                    TFEmptyState(
                        headline = "No ${quadrant.title} Tasks",
                        body = "Tasks in this quadrant will appear here. Add one to get started.",
                        ctaText = "Add Task",
                        onCtaClick = onAddTask
                    )
                }
            } else {
                TFCardGroup {
                    pendingTasks.forEachIndexed { index, task ->
                        key(task.id) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditTask(task) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    TFTaskRow(
                                        task = task,
                                        onToggleComplete = onToggleComplete,
                                        onClick = { onEditTask(task) }
                                    )
                                }

                                // Quick Pomodoro Launch Button
                                IconButton(
                                    onClick = {
                                        TFHaptics.light(context)
                                        onStartPomodoro(task)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Timer,
                                        contentDescription = "Start Pomodoro",
                                        tint = colors.labelSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Move Quadrant Action
                                IconButton(
                                    onClick = {
                                        TFHaptics.light(context)
                                        onMoveQuadrantClick(task)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = "Change Quadrant",
                                        tint = qColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (index < pendingTasks.size - 1) {
                                TFGroupDivider()
                            }
                        }
                    }
                }
            }
        }

        // Completed Section
        if (completedTasks.isNotEmpty()) {
            item(key = "completed_header") {
                Text(
                    text = "COMPLETED (${completedTasks.size})",
                    style = typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = colors.labelSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item(key = "completed_tasks_group") {
                TFCardGroup {
                    completedTasks.forEachIndexed { index, task ->
                        key(task.id) {
                            TFTaskRow(
                                task = task,
                                onToggleComplete = onToggleComplete,
                                onClick = { onEditTask(task) }
                            )
                            if (index < completedTasks.size - 1) {
                                TFGroupDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
