package com.example.todo_list.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskListCategory
import com.example.todo_list.ui.theme.*
import com.example.todo_list.ui.components.CreateCategoryBottomSheet
import com.example.todo_list.ui.components.DeleteCategoryMigrationDialog
import com.example.todo_list.ui.components.primitives.TFCardGroup
import com.example.todo_list.ui.components.primitives.TFGroupDivider
import com.example.todo_list.ui.components.primitives.TFEmptyState
import com.example.todo_list.utils.HapticManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    taskList: List<TaskItem>,
    categoriesList: List<TaskListCategory>,
    onToggleComplete: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onTaskCreated: (TaskItem) -> Unit,
    onCreateCategory: (TaskListCategory) -> Unit,
    onDeleteCategory: (TaskListCategory) -> Unit = {},
    onDeleteCategoryWithMigration: (TaskListCategory, String) -> Unit = { cat, _ -> onDeleteCategory(cat) },
    onOpenEisenhowerMatrix: () -> Unit = {},
    onOpenPomodoro: () -> Unit = {},
    onOpenRetrospective: () -> Unit = {},
    onOpenAddTaskSheet: (prefilledCategory: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryForDetail by remember { mutableStateOf<TaskListCategory?>(null) }
    var selectedSmartFilter by remember { mutableStateOf<String?>(null) } // "Today", "Scheduled", "All", "Completed"
    var showCreateListSheet by remember { mutableStateOf(false) }
    var categoryPendingDelete by remember { mutableStateOf<TaskListCategory?>(null) }
    var pendingMigrationFromCategory by remember { mutableStateOf<TaskListCategory?>(null) }

    // Aggregate metrics for Smart Summary Grid
    val totalTaskCount = remember(taskList) { taskList.size }
    val todayTaskCount = remember(taskList) { taskList.count { it.date.equals("Today", ignoreCase = true) } }
    val scheduledTaskCount = remember(taskList) { taskList.count { !it.date.equals("Someday", ignoreCase = true) } }
    val completedTaskCount = remember(taskList) { taskList.count { it.isCompleted } }

    // Filter categories by search query
    val filteredCategories = remember(categoriesList, searchQuery) {
        if (searchQuery.isBlank()) {
            categoriesList
        } else {
            categoriesList.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Pre-aggregate category statistics to prevent O(N) list filtering during composition & scroll
    val categoryStatsMap = remember(taskList, filteredCategories) {
        filteredCategories.associate { category ->
            val categoryTasks = taskList.filter {
                it.category.equals(category.name, ignoreCase = true)
            }
            val active = categoryTasks.count { !it.isCompleted }
            val completed = categoryTasks.count { it.isCompleted }
            val total = categoryTasks.size
            val ratio = if (total > 0) completed.toFloat() / total else 0f
            category.id to Triple(active, total, ratio)
        }
    }

    val context = LocalContext.current

    // Handle system back button for sheets and dialogs
    BackHandler(enabled = true) {
        when {
            showCreateListSheet -> showCreateListSheet = false
            categoryPendingDelete != null -> categoryPendingDelete = null
            pendingMigrationFromCategory != null -> pendingMigrationFromCategory = null
            selectedSmartFilter != null -> selectedSmartFilter = null
            searchQuery.isNotBlank() -> searchQuery = ""
            else -> {
                // Let the default behavior happen (exit app or go back to previous tab)
                (context as? androidx.activity.ComponentActivity)?.onBackPressed()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemGroupedBackground)
    ) {
        // iOS Large Title Header Bar
        ListsHeaderBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onCreateListClick = { showCreateListSheet = true }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Apple Reminders Bento Grid
            item(key = "smart_summary_grid") {
                Text(
                    text = "PINNED LISTS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                SmartSummaryGrid(
                    todayCount = todayTaskCount,
                    scheduledCount = scheduledTaskCount,
                    allCount = totalTaskCount,
                    completedCount = completedTaskCount,
                    onSelectFilter = { filter ->
                        HapticManager.performClick(context)
                        selectedSmartFilter = filter
                    }
                )
            }

            // Apple Bento Power Tools: Eisenhower Matrix & Pomodoro Timer
            item(key = "productivity_power_tools") {
                Text(
                    text = "PRODUCTIVITY TOOLS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Eisenhower Matrix Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TFTheme.colors.card)
                            .border(hairline(), TFTheme.colors.cardStroke, RoundedCornerShape(16.dp))
                            .clickable {
                                HapticManager.performClick(context)
                                onOpenEisenhowerMatrix()
                            }
                            .padding(14.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SystemBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.GridView,
                                    contentDescription = "Eisenhower Matrix",
                                    tint = SystemBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Eisenhower Matrix",
                                style = TFTheme.typography.subheadline,
                                fontWeight = FontWeight.Bold,
                                color = TFTheme.colors.labelPrimary
                            )
                            Text(
                                text = "4 Quadrants Priority",
                                style = TFTheme.typography.caption,
                                color = TFTheme.colors.labelSecondary
                            )
                        }
                    }

                    // Pomodoro Focus Timer Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TFTheme.colors.card)
                            .border(hairline(), TFTheme.colors.cardStroke, RoundedCornerShape(16.dp))
                            .clickable {
                                HapticManager.performClick(context)
                                onOpenPomodoro()
                            }
                            .padding(14.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = "Pomodoro Timer",
                                    tint = Color(0xFFFF3B30),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Pomodoro Focus",
                                style = TFTheme.typography.subheadline,
                                fontWeight = FontWeight.Bold,
                                color = TFTheme.colors.labelPrimary
                            )
                            Text(
                                text = "Work & Breaks",
                                style = TFTheme.typography.caption,
                                color = TFTheme.colors.labelSecondary
                            )
                        }
                    }

                    // Analytics & Retrospectives Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TFTheme.colors.card)
                            .border(hairline(), TFTheme.colors.cardStroke, RoundedCornerShape(16.dp))
                            .clickable {
                                HapticManager.performClick(context)
                                onOpenRetrospective()
                            }
                            .padding(14.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AppleHealth.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Insights,
                                    contentDescription = "Analytics & Retrospectives",
                                    tint = AppleHealth,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Retrospectives",
                                style = TFTheme.typography.subheadline,
                                fontWeight = FontWeight.Bold,
                                color = TFTheme.colors.labelPrimary
                            )
                            Text(
                                text = "Weekly & MoM",
                                style = TFTheme.typography.caption,
                                color = TFTheme.colors.labelSecondary
                            )
                        }
                    }
                }
            }

            // My Lists Inset Grouped Section
            item(key = "my_lists_group") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MY LISTS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemLabelSecondary,
                        letterSpacing = 0.5.sp
                    )

                    TextButton(
                        onClick = {
                            HapticManager.performClick(context)
                            showCreateListSheet = true
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = SystemBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Add List",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SystemBlue
                        )
                    }
                }

                if (filteredCategories.isEmpty()) {
                    TFCardGroup {
                        TFEmptyState(
                            headline = "Create your first list",
                            body = "Group tasks by project, area or goal.",
                            ctaText = "New List",
                            onCtaClick = { showCreateListSheet = true },
                            starterChips = listOf("Work", "Personal", "Health", "Study"),
                            onChipClick = { name ->
                                val defaultColorHex = "#007AFF"
                                onCreateCategory(
                                    TaskListCategory(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = name,
                                        colorHex = defaultColorHex,
                                        iconName = "List"
                                    )
                                )
                            }
                        )
                    }
                } else {
                    TFCardGroup {
                        filteredCategories.forEachIndexed { index, category ->
                            key(category.id) {
                                val (activeCount, totalCatCount, ratio) = categoryStatsMap[category.id] ?: Triple(0, 0, 0f)

                                CategoryListItemRow(
                                    category = category,
                                    activeCount = activeCount,
                                    progressRatio = ratio,
                                    totalCount = totalCatCount,
                                    showDivider = false,
                                    onClick = {
                                        selectedCategoryForDetail = category
                                    },
                                    onDeleteCategory = {
                                        categoryPendingDelete = category
                                    }
                                )
                                if (index < filteredCategories.size - 1) {
                                    TFGroupDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet for Creating Custom Category
    if (showCreateListSheet) {
        CreateCategoryBottomSheet(
            onDismiss = {
                showCreateListSheet = false
                pendingMigrationFromCategory = null
            },
            onCreateCategory = { newCategory ->
                onCreateCategory(newCategory)
                showCreateListSheet = false
                // If this was opened to create a migration destination:
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
                showCreateListSheet = true
            }
        )
    }

    // Modal Sheet for Category Detail View
    selectedCategoryForDetail?.let { category ->
        val listTasks = taskList
            .filter { it.category.equals(category.name, ignoreCase = true) }
            .sortedWith(compareBy<TaskItem> { it.isCompleted }.thenBy { it.getSortValue() })

        CategoryDetailBottomSheet(
            category = category,
            tasks = listTasks,
            onDismiss = { selectedCategoryForDetail = null },
            onToggleComplete = onToggleComplete,
            onEditTask = onEditTask,
            onDeleteTask = onDeleteTask,
            onAddNewTask = {
                val catName = category.name
                selectedCategoryForDetail = null
                onOpenAddTaskSheet(catName)
            }
        )
    }

    // Modal Sheet for Smart Summary Filter (Today, Scheduled, All, Completed)
    selectedSmartFilter?.let { filterName ->
        val filteredTasks = taskList.filter { task ->
            when (filterName) {
                "Today" -> task.date.equals("Today", ignoreCase = true)
                "Scheduled" -> !task.date.equals("Someday", ignoreCase = true)
                "Completed" -> task.isCompleted
                else -> true // "All"
            }
        }.sortedWith(compareBy<TaskItem> { it.isCompleted }.thenBy { it.getSortValue() })

        SmartSummaryDetailBottomSheet(
            filterTitle = filterName,
            tasks = filteredTasks,
            onDismiss = { selectedSmartFilter = null },
            onToggleComplete = onToggleComplete,
            onEditTask = onEditTask,
            onDeleteTask = onDeleteTask
        )
    }
}

@Composable
fun ListsHeaderBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCreateListClick: () -> Unit
) {
    Surface(
        color = SystemSurface.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lists",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary,
                    letterSpacing = (-0.5).sp
                )

                val context = LocalContext.current
                IconButton(
                    onClick = {
                        HapticManager.performClick(context)
                        onCreateListClick()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add List",
                        tint = SystemBlue,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // iOS Integrated Search Field
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
                                text = "Search lists & tasks",
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
fun SmartSummaryGrid(
    todayCount: Int,
    scheduledCount: Int,
    allCount: Int,
    completedCount: Int,
    onSelectFilter: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SmartSummaryCard(
                title = "Today",
                count = todayCount,
                icon = Icons.Outlined.Today,
                iconTint = SystemBlue,
                bgColor = SystemBlue.copy(alpha = 0.12f),
                modifier = Modifier.weight(1f),
                onClick = { onSelectFilter("Today") }
            )
            SmartSummaryCard(
                title = "Scheduled",
                count = scheduledCount,
                icon = Icons.Outlined.CalendarMonth,
                iconTint = Color(0xFFFF9500),
                bgColor = Color(0xFFFF9500).copy(alpha = 0.12f),
                modifier = Modifier.weight(1f),
                onClick = { onSelectFilter("Scheduled") }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SmartSummaryCard(
                title = "All Tasks",
                count = allCount,
                icon = Icons.Outlined.Inbox,
                iconTint = SystemGray,
                bgColor = SystemGray.copy(alpha = 0.15f),
                modifier = Modifier.weight(1f),
                onClick = { onSelectFilter("All") }
            )
            SmartSummaryCard(
                title = "Completed",
                count = completedCount,
                icon = Icons.Outlined.CheckCircle,
                iconTint = SystemGreen,
                bgColor = SystemGreen.copy(alpha = 0.12f),
                modifier = Modifier.weight(1f),
                onClick = { onSelectFilter("Completed") }
            )
        }
    }
}

@Composable
fun SmartSummaryCard(
    title: String,
    count: Int,
    icon: ImageVector,
    iconTint: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SystemSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        shadowElevation = 1.dp,
        modifier = modifier.clickable {
            HapticManager.performClick(context)
            onClick()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "$count",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SystemLabelSecondary
            )
        }
    }
}

@Composable
fun CategoryListItemRow(
    category: TaskListCategory,
    activeCount: Int,
    totalCount: Int,
    progressRatio: Float,
    showDivider: Boolean,
    onClick: () -> Unit,
    onDeleteCategory: () -> Unit
) {
    val catColor = category.getColor()
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Category Icon Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(catColor)
                ) {
                    val iconVector = getCategoryIcon(category.iconName, category.name)
                    Icon(
                        imageVector = iconVector,
                        contentDescription = category.name,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = SystemLabelPrimary
                    )

                    if (totalCount > 0) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Subtle iOS Progress Bar
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(SystemGray5)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progressRatio)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                            }
                            Text(
                                text = "${(progressRatio * 100).toInt()}% completed",
                                fontSize = 12.sp,
                                color = SystemLabelSecondary
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$activeCount",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SystemLabelSecondary
                )

                if (!category.isSystemDefault) {
                    IconButton(
                        onClick = onDeleteCategory,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete list",
                            tint = SystemGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open List",
                    tint = SystemGray2,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = SystemDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 62.dp)
            )
        }
    }
}

fun getCategoryIcon(iconName: String, categoryName: String): ImageVector {
    return when (iconName.lowercase()) {
        "work", "briefcase" -> Icons.Outlined.WorkOutline
        "personal", "person" -> Icons.Outlined.Person
        "health", "fitness" -> Icons.Outlined.FavoriteBorder
        "study", "book" -> Icons.Outlined.School
        "shopping", "cart" -> Icons.Outlined.ShoppingCart
        "star" -> Icons.Outlined.StarBorder
        "flag" -> Icons.Outlined.Flag
        "heart" -> Icons.Outlined.FavoriteBorder
        else -> when (categoryName.lowercase()) {
            "work" -> Icons.Outlined.WorkOutline
            "personal" -> Icons.Outlined.Person
            "health" -> Icons.Outlined.FavoriteBorder
            "study" -> Icons.Outlined.School
            else -> Icons.Outlined.Category
        }
    }
}

// Modal Sheet to Create Custom Category List
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListBottomSheet(
    onDismiss: () -> Unit,
    onCreateList: (TaskListCategory) -> Unit
) {
    var listName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#007AFF") }
    var selectedIconName by remember { mutableStateOf("List") }

    val colors = listOf(
        "#007AFF", // System Blue
        "#AF52DE", // System Purple
        "#34C759", // System Green
        "#FF9500", // System Orange
        "#FF3B30", // System Red
        "#FF2D55", // System Pink
        "#30B0C7", // System Teal
        "#5856D6"  // System Indigo
    )

    val icons = listOf(
        "List" to Icons.Outlined.Category,
        "Work" to Icons.Outlined.WorkOutline,
        "Personal" to Icons.Outlined.Person,
        "Health" to Icons.Outlined.FavoriteBorder,
        "Study" to Icons.Outlined.School,
        "Shopping" to Icons.Outlined.ShoppingCart,
        "Star" to Icons.Outlined.StarBorder,
        "Flag" to Icons.Outlined.Flag
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemGroupedBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New List",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Close", tint = SystemLabelSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview Badge Header
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(selectedColorHex)))
                ) {
                    Icon(
                        imageVector = getCategoryIcon(selectedIconName, listName),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // List Title Input Field
            OutlinedTextField(
                value = listName,
                onValueChange = { listName = it },
                placeholder = { Text("List Name (e.g., Projects, Groceries)") },
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

            Spacer(modifier = Modifier.height(20.dp))

            // Color Selector Row
            Text(
                text = "COLOR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                colors.forEach { hex ->
                    val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(hex)))
                            .then(
                                if (isSelected) Modifier.border(3.dp, SystemLabelPrimary, CircleShape) else Modifier
                            )
                            .clickable { selectedColorHex = hex }
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Icon Selector Chips
            Text(
                text = "ICON",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(icons) { (iconLabel, iconVector) ->
                    val isSelected = iconLabel.equals(selectedIconName, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) SystemBlue else SystemSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            if (isSelected) SystemBlue else SystemDivider
                        ),
                        modifier = Modifier.clickable { selectedIconName = iconLabel }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = iconLabel,
                                tint = if (isSelected) Color.White else SystemLabelSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = iconLabel,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else SystemLabelPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Done Button
            Button(
                onClick = {
                    if (listName.isNotBlank()) {
                        val newCategory = TaskListCategory(
                            id = "cat_${System.currentTimeMillis()}",
                            name = listName.trim(),
                            colorHex = selectedColorHex,
                            iconName = selectedIconName,
                            isSystemDefault = false
                        )
                        onCreateList(newCategory)
                    }
                },
                enabled = listName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Done",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Modal Sheet displaying Detail tasks of a specific Category List
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailBottomSheet(
    category: TaskListCategory,
    tasks: List<TaskItem>,
    onDismiss: () -> Unit,
    onToggleComplete: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onAddNewTask: () -> Unit
) {
    val catColor = category.getColor()
    val completedCount = tasks.count { it.isCompleted }
    val totalCount = tasks.size
    val pendingCount = totalCount - completedCount
    val progressRatio = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

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
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(catColor)
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category.iconName, category.name),
                            contentDescription = category.name,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = category.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = SystemLabelPrimary
                        )
                        Text(
                            text = "$pendingCount pending • $completedCount completed",
                            fontSize = 13.sp,
                            color = SystemLabelSecondary
                        )
                    }
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

            // Progress Banner Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SystemSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "List Progress",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SystemLabelPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(SystemGray5)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressRatio)
                                    .clip(CircleShape)
                                    .background(catColor)
                            )
                        }
                        Text(
                            text = "${(progressRatio * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = catColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tasks List Container
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SystemSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (tasks.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        Text(
                            text = "No tasks in ${category.name}",
                            fontSize = 15.sp,
                            color = SystemLabelSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(tasks.size) { index ->
                            val task = tasks[index]
                            TaskCardItem(
                                task = task,
                                showDivider = index < tasks.size - 1,
                                onToggleComplete = onToggleComplete,
                                onEditTask = onEditTask,
                                onDelete = onDeleteTask
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Modal Sheet for Smart Summary Filter View
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSummaryDetailBottomSheet(
    filterTitle: String,
    tasks: List<TaskItem>,
    onDismiss: () -> Unit,
    onToggleComplete: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit
) {
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
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = filterTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemLabelPrimary
                    )
                    Text(
                        text = "${tasks.size} tasks found",
                        fontSize = 13.sp,
                        color = SystemLabelSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Close", tint = SystemLabelSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SystemSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (tasks.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        Text(
                            text = "No tasks found for $filterTitle",
                            fontSize = 15.sp,
                            color = SystemLabelSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(tasks.size) { index ->
                            val task = tasks[index]
                            TaskCardItem(
                                task = task,
                                showDivider = index < tasks.size - 1,
                                onToggleComplete = onToggleComplete,
                                onEditTask = onEditTask,
                                onDelete = onDeleteTask
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
