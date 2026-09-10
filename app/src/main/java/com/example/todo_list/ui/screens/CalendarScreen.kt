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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskListCategory
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    taskList: List<TaskItem>,
    categoriesList: List<TaskListCategory> = TaskListCategory.DEFAULT_CATEGORIES,
    onToggleComplete: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onAddTaskClick: () -> Unit
) {
    // Current date references
    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    // Calendar state
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var viewMode by remember { mutableStateOf(0) } // 0 = Month, 1 = Week, 2 = Agenda
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Derived dates
    val selectedYear = selectedCalendar.get(Calendar.YEAR)
    val selectedMonth = selectedCalendar.get(Calendar.MONTH)
    val selectedDay = selectedCalendar.get(Calendar.DAY_OF_MONTH)

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val shortMonthFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    val currentMonthTitle = remember(selectedCalendar) { shortMonthFormat.format(selectedCalendar.time) }
    val fullMonthTitle = remember(selectedCalendar) { monthFormat.format(selectedCalendar.time).uppercase() }
    val selectedDayTitle = remember(selectedCalendar) { dayFormat.format(selectedCalendar.time) }

    // Filter AND SORT tasks for selected date & category
    // Completed tasks move automatically to the bottom (isCompleted = true comes after isCompleted = false)
    val tasksForSelectedDate = remember(taskList, selectedYear, selectedMonth, selectedDay, selectedCategoryFilter) {
        taskList
            .filter { task ->
                val matchesCategory = (selectedCategoryFilter == "All" || task.category.equals(selectedCategoryFilter, ignoreCase = true))
                val isTodaySelected = (selectedYear == todayYear && selectedMonth == todayMonth && selectedDay == todayDay)
                val matchesDate = if (task.date.equals("Today", ignoreCase = true)) {
                    isTodaySelected
                } else if (task.date.equals("Tomorrow", ignoreCase = true)) {
                    val tomCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                    selectedYear == tomCal.get(Calendar.YEAR) &&
                    selectedMonth == tomCal.get(Calendar.MONTH) &&
                    selectedDay == tomCal.get(Calendar.DAY_OF_MONTH)
                } else {
                    true
                }
                matchesCategory && matchesDate
            }
            .sortedWith(compareBy<TaskItem> { it.isCompleted }.thenBy { it.getSortValue() })
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemGroupedBackground)
    ) {
        // Sticky Apple Glass Header (with statusBarsPadding to avoid camera cutout overlap on S24 FE & all phones)
        CalendarHeaderBar(
            currentMonthTitle = currentMonthTitle,
            viewMode = viewMode,
            onViewModeChange = { viewMode = it },
            onTodayClick = {
                selectedCalendar = Calendar.getInstance()
            },
            onAddTaskClick = onAddTaskClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header Month / Week Title with Next/Prev Arrows
            item(key = "calendar_month_nav_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fullMonthTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SystemLabelSecondary,
                        letterSpacing = 1.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(
                            onClick = {
                                HapticManager.performClick(context)
                                selectedCalendar = (selectedCalendar.clone() as Calendar).apply {
                                    if (viewMode == 1) {
                                        // Week view: Slide to previous 7 days
                                        add(Calendar.DAY_OF_YEAR, -7)
                                    } else {
                                        // Month view: Previous month
                                        add(Calendar.MONTH, -1)
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous",
                                tint = SystemBlue
                            )
                        }
                        IconButton(
                            onClick = {
                                HapticManager.performClick(context)
                                selectedCalendar = (selectedCalendar.clone() as Calendar).apply {
                                    if (viewMode == 1) {
                                        // Week view: Slide to next 7 days
                                        add(Calendar.DAY_OF_YEAR, 7)
                                    } else {
                                        // Month view: Next month
                                        add(Calendar.MONTH, 1)
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next",
                                tint = SystemBlue
                            )
                        }
                    }
                }
            }

            // Interactive Calendar View Switcher
            item(key = "calendar_view_switcher") {
                AnimatedContent(
                    targetState = viewMode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "CalendarViewMode"
                ) { targetMode ->
                    when (targetMode) {
                        0 -> MonthCalendarCard(
                            selectedCalendar = selectedCalendar,
                            todayYear = todayYear,
                            todayMonth = todayMonth,
                            todayDay = todayDay,
                            taskList = taskList,
                            onDateSelected = { year, month, day ->
                                selectedCalendar = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, day)
                                }
                            }
                        )
                        1 -> WeekCalendarStrip(
                            selectedCalendar = selectedCalendar,
                            todayYear = todayYear,
                            todayMonth = todayMonth,
                            todayDay = todayDay,
                            onDateSelected = { year, month, day ->
                                selectedCalendar = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, day)
                                }
                            }
                        )
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Category Filter Pills
            item(key = "calendar_category_filter") {
                CategoryFilterBar(
                    selectedCategory = selectedCategoryFilter,
                    categoriesList = categoriesList,
                    onCategorySelected = { selectedCategoryFilter = it }
                )
            }

            // Schedule Agenda Header
            item(key = "calendar_agenda_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCHEDULE FOR ${selectedDayTitle.uppercase()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemLabelSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = SystemGray5
                    ) {
                        Text(
                            text = "${tasksForSelectedDate.size} TASKS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SystemLabelSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Agenda List Cards (Completed tasks sorted to bottom automatically)
            if (tasksForSelectedDate.isEmpty()) {
                item(key = "calendar_empty_schedule") {
                    EmptyScheduleState(
                        dateString = selectedDayTitle,
                        onAddTaskClick = onAddTaskClick
                    )
                }
            } else {
                items(tasksForSelectedDate, key = { it.id }) { task ->
                    CalendarTaskCardItem(
                        task = task,
                        onToggleComplete = onToggleComplete,
                        onEditTask = onEditTask,
                        onDeleteTask = onDeleteTask
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarHeaderBar(
    currentMonthTitle: String,
    viewMode: Int,
    onViewModeChange: (Int) -> Unit,
    onTodayClick: () -> Unit,
    onAddTaskClick: () -> Unit
) {
    Surface(
        color = SystemSurface.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding() // FIX ISSUE 1: Adds safe top padding so camera cutout & status bar do not block controls on S24 FE
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val context = LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Today Button
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = SystemBlueLight,
                    modifier = Modifier.clickable {
                        HapticManager.performClick(context)
                        onTodayClick()
                    }
                ) {
                    Text(
                        text = "Today",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SystemBlue,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // Apple iOS Segmented Control
                IosSegmentedControl(
                    items = listOf("Month", "Week", "Agenda"),
                    selectedIndex = viewMode,
                    onOptionSelected = onViewModeChange
                )

                // Add Task Icon Button
                IconButton(onClick = {
                    HapticManager.performClick(context)
                    onAddTaskClick()
                }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        tint = SystemBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Title Row
            Text(
                text = currentMonthTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
fun IosSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(8.9.dp),
        color = SearchInputBackground,
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.9.dp))
                        .background(if (isSelected) SystemSurface else Color.Transparent)
                        .then(
                            if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(6.9.dp)) else Modifier
                        )
                        .clickable {
                            HapticManager.performClick(context)
                            onOptionSelected(index)
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) SystemLabelPrimary else SystemLabelSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun MonthCalendarCard(
    selectedCalendar: Calendar,
    todayYear: Int,
    todayMonth: Int,
    todayDay: Int,
    taskList: List<TaskItem>,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit
) {
    val curYear = selectedCalendar.get(Calendar.YEAR)
    val curMonth = selectedCalendar.get(Calendar.MONTH)
    val selDay = selectedCalendar.get(Calendar.DAY_OF_MONTH)

    // Compute month days grid
    val gridDays = remember(curYear, curMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, curYear)
            set(Calendar.MONTH, curMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon ...
        // Convert to Monday = 0 index offset
        val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Previous month days for padding
        val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val list = mutableListOf<CalendarDayInfo>()
        for (i in (daysInPrevMonth - offset + 1)..daysInPrevMonth) {
            list.add(CalendarDayInfo(i, isCurrentMonth = false, isNextMonth = false))
        }
        for (i in 1..daysInMonth) {
            list.add(CalendarDayInfo(i, isCurrentMonth = true, isNextMonth = false))
        }
        val remaining = 42 - list.size // 6 rows * 7 columns
        for (i in 1..remaining) {
            list.add(CalendarDayInfo(i, isCurrentMonth = false, isNextMonth = true))
        }
        list
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SystemSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Weekday Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SystemLabelSecondary,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6 Rows x 7 Columns Calendar Grid
            gridDays.chunked(7).forEach { weekRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    weekRow.forEach { dayInfo ->
                        val isToday = dayInfo.isCurrentMonth && curYear == todayYear && curMonth == todayMonth && dayInfo.dayNumber == todayDay
                        val isSelected = dayInfo.isCurrentMonth && dayInfo.dayNumber == selDay
                        val context = LocalContext.current

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isToday -> SystemBlue
                                        isSelected -> SystemBlueLight
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (isSelected && !isToday) {
                                        Modifier.border(1.5.dp, SystemBlue, CircleShape)
                                    } else Modifier
                                )
                                .clickable(enabled = dayInfo.isCurrentMonth) {
                                    HapticManager.performClick(context)
                                    onDateSelected(curYear, curMonth, dayInfo.dayNumber)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayInfo.dayNumber.toString(),
                                    fontSize = 15.sp,
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isToday -> Color.White
                                        isSelected -> SystemBlue
                                        !dayInfo.isCurrentMonth -> SystemLabelTertiary
                                        else -> SystemLabelPrimary
                                    }
                                )

                                // Category density dots
                                if (dayInfo.isCurrentMonth) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        if (dayInfo.dayNumber % 3 == 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isToday) Color.White else AppleWork)
                                            )
                                        }
                                        if (dayInfo.dayNumber % 4 == 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isToday) Color.White else ApplePersonal)
                                            )
                                        }
                                        if (dayInfo.dayNumber % 5 == 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isToday) Color.White else AppleHealth)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CalendarDayInfo(
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val isNextMonth: Boolean
)

@Composable
fun WeekCalendarStrip(
    selectedCalendar: Calendar,
    todayYear: Int,
    todayMonth: Int,
    todayDay: Int,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit
) {
    val curYear = selectedCalendar.get(Calendar.YEAR)
    val curMonth = selectedCalendar.get(Calendar.MONTH)
    val selDay = selectedCalendar.get(Calendar.DAY_OF_MONTH)

    // FIX ISSUE 2: Compute the 7 days of the selected week starting from Monday -> Sunday
    val weekDays = remember(curYear, curMonth, selDay) {
        val cal = (selectedCalendar.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val list = mutableListOf<Calendar>()
        for (i in 0..6) {
            list.add(cal.clone() as Calendar)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    val dayNameFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SystemSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekDays.forEach { cal ->
                val yr = cal.get(Calendar.YEAR)
                val mo = cal.get(Calendar.MONTH)
                val dy = cal.get(Calendar.DAY_OF_MONTH)

                val isToday = yr == todayYear && mo == todayMonth && dy == todayDay
                val isSelected = yr == curYear && mo == curMonth && dy == selDay

                val context = LocalContext.current
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isToday -> SystemBlue
                        isSelected -> SystemBlueLight
                        else -> SystemSurface
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected && !isToday) 1.5.dp else 0.5.dp,
                        color = if (isSelected && !isToday) SystemBlue else SystemDivider
                    ),
                    modifier = Modifier
                        .weight(1f) // FIX ISSUE 2: Weight = 1f ensures all 7 days fit 100% across the screen width without clipping!
                        .height(68.dp)
                        .clickable {
                            HapticManager.performClick(context)
                            onDateSelected(yr, mo, dy)
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                    ) {
                        Text(
                            text = dayNameFormat.format(cal.time).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                isToday -> Color.White.copy(alpha = 0.9f)
                                isSelected -> SystemBlue
                                else -> SystemLabelSecondary
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dy.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isToday -> Color.White
                                isSelected -> SystemBlue
                                else -> SystemLabelPrimary
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterBar(
    selectedCategory: String,
    categoriesList: List<TaskListCategory> = TaskListCategory.DEFAULT_CATEGORIES,
    onCategorySelected: (String) -> Unit
) {
    val categories = remember(categoriesList) {
        listOf("All") + categoriesList.map { it.name }
    }
    val context = LocalContext.current

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories, key = { it }) { category ->
            val isSelected = category.equals(selectedCategory, ignoreCase = true)

            Surface(
                shape = RoundedCornerShape(100.dp),
                color = if (isSelected) SystemBlue else SystemSurface,
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    if (isSelected) SystemBlue else SystemDivider
                ),
                shadowElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier.clickable {
                    HapticManager.performClick(context)
                    onCategorySelected(category)
                }
            ) {
                Text(
                    text = category,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else SystemLabelSecondary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun CalendarTaskCardItem(
    task: TaskItem,
    onToggleComplete: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit
) {
    val categoryColor = getCategoryColor(task.category)
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SystemSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable {
                HapticManager.performClick(context)
                onEditTask(task)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Vertical Category Color Strip
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(categoryColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Circular Checkbox Toggle
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (task.isCompleted) SystemBlue else Color.Transparent)
                            .border(
                                1.5.dp,
                                if (task.isCompleted) SystemBlue else SystemGray2,
                                CircleShape
                            )
                            .clickable {
                                if (!task.isCompleted) {
                                    HapticManager.performSuccess(context)
                                } else {
                                    HapticManager.performClick(context)
                                }
                                onToggleComplete(task)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.isCompleted) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = task.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (task.isCompleted) SystemLabelTertiary else SystemLabelPrimary,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = "Time",
                                tint = SystemLabelSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = com.example.todo_list.utils.TimeFormatHelper.formatTimeForDisplay(
                                    task.time,
                                    com.example.todo_list.manager.TimePreferencesManager.is24HourFormat
                                ),
                                fontSize = 12.sp,
                                color = SystemLabelSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Category Badge Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = categoryColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = task.category.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyScheduleState(
    dateString: String,
    onAddTaskClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SystemSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = SystemGray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Tasks Scheduled for $dateString",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = SystemLabelSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddTaskClick,
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                shape = RoundedCornerShape(100.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Add Task for $dateString", fontSize = 14.sp)
            }
        }
    }
}
