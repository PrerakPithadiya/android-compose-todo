package com.example.todo_list.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.manager.TimePreferencesManager
import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskListCategory
import com.example.todo_list.model.TaskPriority
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager
import com.example.todo_list.utils.HapticType
import com.example.todo_list.utils.TimeFormatHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Next-Level Apple (iOS HIG) Edit Task Bottom Sheet.
 * Provides consistent styling, inline date/time pickers, priority segmented controls,
 * and seamless updates across the application.
 */
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

    // Parse potential notes stored inside title string
    val initialTitleParts = remember(task.title) {
        val split = task.title.split("\n", limit = 2)
        Pair(split[0], split.getOrNull(1).orEmpty())
    }

    var taskTitle by remember { mutableStateOf(initialTitleParts.first) }
    var taskNotes by remember { mutableStateOf(initialTitleParts.second) }
    var selectedCategory by remember { mutableStateOf(task.category) }
    var selectedDate by remember { mutableStateOf(task.date) }
    var selectedEpochDay by remember { mutableStateOf(task.epochDay.takeIf { it > 0 } ?: currentEpochDay) }
    var selectedTime by remember(task.time, is24Hour) {
        mutableStateOf(TimeFormatHelper.formatTimeForDisplay(task.time, is24Hour))
    }
    var selectedPriority by remember(task.priority) { mutableStateOf(task.priority) }
    var titleError by remember { mutableStateOf(false) }

    // Inline Pickers Visibility
    var showInlineDatePicker by remember { mutableStateOf(false) }
    var showInlineTimePicker by remember { mutableStateOf(false) }
    var showCreateCategorySheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // DatePicker State
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (selectedEpochDay > 0) selectedEpochDay * (1000 * 60 * 60 * 24) else System.currentTimeMillis()
    )

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { millis ->
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val formatted = formatter.format(Date(millis))
            val epochDay = millis / (1000 * 60 * 60 * 24)
            if (epochDay == currentEpochDay) {
                selectedDate = "Today"
            } else if (epochDay == currentEpochDay + 1) {
                selectedDate = "Tomorrow"
            } else {
                selectedDate = formatted
            }
            selectedEpochDay = epochDay
        }
    }

    // TimePicker State
    val (parsedHour, parsedMinute) = remember(selectedTime) {
        TimeFormatHelper.parseHourMinute(selectedTime)
    }
    val timePickerState = rememberTimePickerState(
        initialHour = parsedHour,
        initialMinute = parsedMinute,
        is24Hour = is24Hour
    )

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        val formattedTime = TimeFormatHelper.formatHourMinute(
            timePickerState.hour,
            timePickerState.minute,
            is24Hour
        )
        selectedTime = formattedTime
    }

    val canSave = taskTitle.isNotBlank()
    val saveChanges = {
        if (taskTitle.isBlank()) {
            titleError = true
            HapticManager.perform(context, HapticType.WARNING)
        } else {
            HapticManager.perform(context, HapticType.SUCCESS)
            val finalTitle = if (taskNotes.isNotBlank()) {
                "${taskTitle.trim()}\n${taskNotes.trim()}"
            } else {
                taskTitle.trim()
            }
            onTaskUpdated(
                task.copy(
                    title = finalTitle,
                    category = selectedCategory,
                    date = selectedDate,
                    time = selectedTime.ifBlank { "12:00 PM" },
                    epochDay = selectedEpochDay,
                    priority = selectedPriority
                )
            )
        }
    }

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemGroupedBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(SystemGray2.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // iOS Form Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        HapticManager.perform(context, HapticType.CLICK)
                        onDismiss()
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 17.sp,
                        color = SystemBlue,
                        fontWeight = FontWeight.Normal
                    )
                }

                Text(
                    text = "Details",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SystemLabelPrimary
                )

                TextButton(
                    onClick = { saveChanges() },
                    enabled = canSave,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canSave) SystemBlue else SystemBlue.copy(alpha = 0.35f)
                    )
                }
            }

            // Scrollable Inset Grouped Sections
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Title & Notes
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SystemSurface,
                    border = BorderStroke(0.5.dp, if (titleError) SystemRed else SystemDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            if (taskTitle.isEmpty()) {
                                Text(
                                    text = "Task Title",
                                    fontSize = 17.sp,
                                    color = SystemLabelSecondary.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            BasicTextField(
                                value = taskTitle,
                                onValueChange = {
                                    taskTitle = it
                                    if (it.isNotBlank()) titleError = false
                                },
                                textStyle = TextStyle(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SystemLabelPrimary
                                ),
                                cursorBrush = SolidColor(SystemBlue),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { saveChanges() }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            thickness = 0.5.dp,
                            color = SystemDivider
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            if (taskNotes.isEmpty()) {
                                Text(
                                    text = "Notes or details...",
                                    fontSize = 15.sp,
                                    color = SystemLabelSecondary.copy(alpha = 0.45f)
                                )
                            }
                            BasicTextField(
                                value = taskNotes,
                                onValueChange = { taskNotes = it },
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    color = SystemLabelPrimary
                                ),
                                cursorBrush = SolidColor(SystemBlue),
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Card 2: Schedule (Date & Time)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SystemSurface,
                    border = BorderStroke(0.5.dp, SystemDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Date Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticManager.perform(context, HapticType.CLICK)
                                    showInlineDatePicker = !showInlineDatePicker
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SystemBlue.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = "Date",
                                        tint = SystemBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Date",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SystemLabelPrimary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SystemBlueLight
                                ) {
                                    Text(
                                        text = selectedDate,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SystemBlue,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Icon(
                                    imageVector = if (showInlineDatePicker) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    tint = SystemGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Quick Date Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Today" to currentEpochDay,
                                "Tomorrow" to currentEpochDay + 1
                            ).forEach { (label, epoch) ->
                                val isSelected = selectedDate == label
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) SystemBlue else SystemGroupedBackground,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            HapticManager.perform(context, HapticType.CLICK)
                                            selectedDate = label
                                            selectedEpochDay = epoch
                                            showInlineDatePicker = false
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else SystemLabelPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            val isWeekendSelected = selectedDate != "Today" && selectedDate != "Tomorrow" && !showInlineDatePicker
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isWeekendSelected) SystemBlue else SystemGroupedBackground,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        HapticManager.perform(context, HapticType.CLICK)
                                        val cal = Calendar.getInstance()
                                        val daysUntilSat = (Calendar.SATURDAY - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
                                        val targetDays = if (daysUntilSat == 0) 7 else daysUntilSat
                                        cal.add(Calendar.DAY_OF_YEAR, targetDays)
                                        val fmt = SimpleDateFormat("MMM dd", Locale.getDefault())
                                        selectedDate = "Weekend (${fmt.format(cal.time)})"
                                        selectedEpochDay = cal.timeInMillis / (1000 * 60 * 60 * 24)
                                        showInlineDatePicker = false
                                    }
                            ) {
                                Text(
                                    text = "Weekend 🌅",
                                    fontSize = 12.sp,
                                    fontWeight = if (isWeekendSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isWeekendSelected) Color.White else SystemLabelPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Inline Calendar Accordion
                        AnimatedVisibility(
                            visible = showInlineDatePicker,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                DatePicker(
                                    state = datePickerState,
                                    showModeToggle = false,
                                    colors = DatePickerDefaults.colors(
                                        selectedDayContainerColor = SystemBlue,
                                        todayDateBorderColor = SystemBlue
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            thickness = 0.5.dp,
                            color = SystemDivider
                        )

                        // Time Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticManager.perform(context, HapticType.CLICK)
                                    showInlineTimePicker = !showInlineTimePicker
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFF9500).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = "Time",
                                        tint = Color(0xFFFF9500),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Time",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SystemLabelPrimary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SystemBlueLight
                                ) {
                                    Text(
                                        text = selectedTime,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SystemBlue,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Icon(
                                    imageVector = if (showInlineTimePicker) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    tint = SystemGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Quick Time Chips
                        val quickTimes = remember(is24Hour) { TimeFormatHelper.getQuickTimes(is24Hour) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickTimes.forEach { qTime ->
                                val isSelected = selectedTime == qTime && !showInlineTimePicker
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) SystemBlue else SystemGroupedBackground,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            HapticManager.perform(context, HapticType.CLICK)
                                            selectedTime = qTime
                                            showInlineTimePicker = false
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccessTime,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else SystemLabelSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = qTime,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else SystemLabelPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Inline Clock Accordion
                        AnimatedVisibility(
                            visible = showInlineTimePicker,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                TimePicker(
                                    state = timePickerState,
                                    colors = TimePickerDefaults.colors(
                                        clockDialColor = SystemGroupedBackground,
                                        clockDialSelectedContentColor = Color.White,
                                        clockDialUnselectedContentColor = SystemLabelPrimary,
                                        selectorColor = SystemBlue,
                                        periodSelectorSelectedContainerColor = SystemBlueLight,
                                        periodSelectorSelectedContentColor = SystemBlue
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Card 3: Priority & Category
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SystemSurface,
                    border = BorderStroke(0.5.dp, SystemDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Priority Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SystemRed.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Flag,
                                        contentDescription = "Priority",
                                        tint = SystemRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Priority",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SystemLabelPrimary
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SystemGroupedBackground)
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf(
                                    TaskPriority.NONE to "None",
                                    TaskPriority.LOW to "! Low",
                                    TaskPriority.MEDIUM to "!! Med",
                                    TaskPriority.HIGH to "!!! High"
                                ).forEach { (prio, label) ->
                                    val isSelected = selectedPriority == prio.key
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) {
                                                    if (prio == TaskPriority.NONE) SystemSurface else Color(prio.badgeBgHex)
                                                } else Color.Transparent
                                            )
                                            .clickable {
                                                HapticManager.perform(context, HapticType.CLICK)
                                                selectedPriority = prio.key
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                if (prio == TaskPriority.NONE) SystemLabelPrimary else Color(prio.colorHex)
                                            } else SystemLabelSecondary
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            thickness = 0.5.dp,
                            color = SystemDivider
                        )

                        // Category Row
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SystemBlue.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Category,
                                            contentDescription = "List",
                                            tint = SystemBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "List",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SystemLabelPrimary
                                    )
                                }

                                Text(
                                    text = selectedCategory,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SystemBlue
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                categoriesList.forEach { catItem ->
                                    val isSelected = selectedCategory.equals(catItem.name, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) SystemBlue else SystemGroupedBackground,
                                        border = if (isSelected) null else BorderStroke(0.5.dp, SystemDivider),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                HapticManager.perform(context, HapticType.CLICK)
                                                selectedCategory = catItem.name
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(catItem.getEmoji(), fontSize = 13.sp)
                                            Text(
                                                text = catItem.name,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else SystemLabelPrimary
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SystemBlue.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, SystemBlue.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            HapticManager.perform(context, HapticType.CLICK)
                                            showCreateCategorySheet = true
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New List",
                                            tint = SystemBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "New List",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SystemBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Card 4: Destructive Action (Delete Task)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SystemSurface,
                    border = BorderStroke(0.5.dp, SystemDivider),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            HapticManager.perform(context, HapticType.WARNING)
                            onDeleteTask(task)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Task",
                            tint = SystemRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Task",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SystemRed
                        )
                    }
                }

                // Bottom Save Action
                Button(
                    onClick = { saveChanges() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SystemBlue,
                        disabledContainerColor = SystemBlue.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
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

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
