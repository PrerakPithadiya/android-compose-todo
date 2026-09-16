package com.example.todo_list.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.example.todo_list.utils.TaskScheduleParser
import com.example.todo_list.utils.TimeFormatHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Next-Level Apple (iOS HIG) Create Task Bottom Sheet.
 * Features:
 * - Authentic iOS navigation bar (Cancel / New Task / Add)
 * - Automatic keyboard focus upon presentation
 * - Seamless inset grouped form cards (Title & Notes)
 * - Offline Apple Intelligence natural language schedule detection & suggestion pill
 * - Smooth inline expandable date & time pickers (no disruptive dialogs)
 * - Sleek iOS-style segmented priority control & category carousel
 * - Quick floating keyboard accessory shortcuts
 * - Physical Apple haptic feedback on all interactions
 */
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
    val is24Hour = TimePreferencesManager.is24HourFormat
    val currentEpochDay = remember { System.currentTimeMillis() / (1000 * 60 * 60 * 24) }

    // Input States
    var taskTitle by remember { mutableStateOf("") }
    var taskNotes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedDate by remember { mutableStateOf("Today") }
    var selectedEpochDay by remember { mutableStateOf(currentEpochDay) }
    var selectedTime by remember(is24Hour) {
        mutableStateOf(if (is24Hour) "17:00" else "05:00 PM")
    }
    var selectedPriority by remember { mutableStateOf("NONE") }
    var titleError by remember { mutableStateOf(false) }

    // Inline Pickers Visibility
    var showInlineDatePicker by remember { mutableStateOf(false) }
    var showInlineTimePicker by remember { mutableStateOf(false) }
    var showCreateCategorySheet by remember { mutableStateOf(false) }

    // Focus & Scroll
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    // Real-time Natural Language Schedule Detection
    val parsedSchedule = remember(taskTitle, is24Hour) {
        TaskScheduleParser.parse(
            input = taskTitle,
            is24Hour = is24Hour,
            knownCategories = categoriesList.map { it.name }
        )
    }

    val applySuggestion = {
        HapticManager.perform(context, HapticType.SUCCESS)
        parsedSchedule.suggestedDate?.let { selectedDate = it }
        parsedSchedule.suggestedEpochDay?.let { selectedEpochDay = it }
        parsedSchedule.suggestedTime?.let { selectedTime = it }
        parsedSchedule.suggestedPriority?.let { selectedPriority = it }
        parsedSchedule.suggestedCategory?.let { selectedCategory = it }
        if (parsedSchedule.cleanedTitle.isNotBlank() && parsedSchedule.cleanedTitle != taskTitle) {
            taskTitle = parsedSchedule.cleanedTitle
        }
    }

    // DatePicker state for inline calendar
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    // Sync inline datepicker changes
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

    // TimePicker state for inline clock
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

    // Auto-focus keyboard on launch
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(180)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Submit Action Helper
    val canSubmit = taskTitle.isNotBlank()
    val submitTask = {
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
            val newId = System.currentTimeMillis().toString()
            onTaskCreated(
                TaskItem(
                    id = newId,
                    title = finalTitle,
                    category = selectedCategory,
                    date = selectedDate,
                    time = selectedTime.ifBlank { "12:00 PM" },
                    isCompleted = false,
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
            // 1. Authentic Apple Navigation Bar Header
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
                    text = "New Task",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SystemLabelPrimary
                )

                TextButton(
                    onClick = { submitTask() },
                    enabled = canSubmit,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Add",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canSubmit) SystemBlue else SystemBlue.copy(alpha = 0.35f)
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 2. Apple Inset Grouped Card: Title & Notes
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SystemSurface,
                    border = BorderStroke(0.5.dp, if (titleError) SystemRed else SystemDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Title Input Field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            if (taskTitle.isEmpty()) {
                                Text(
                                    text = "What needs to be done?",
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
                                    onDone = { submitTask() }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }

                        // Inset Hairline Divider
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            thickness = 0.5.dp,
                            color = SystemDivider
                        )

                        // Notes Input Field
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

                // 3. Apple Intelligence Smart Suggestion Pill
                AnimatedVisibility(
                    visible = parsedSchedule.hasSuggestions,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SystemBlueLight.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, SystemBlue.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { applySuggestion() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Smart Schedule",
                                    tint = SystemBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Suggestion: ${parsedSchedule.getSummaryPillText()}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SystemBlueDark
                                    )
                                    Text(
                                        text = "Tap to auto-apply date, time & priority",
                                        fontSize = 11.sp,
                                        color = SystemLabelSecondary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SystemBlue)
                                    .clickable { applySuggestion() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Apply",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 4. Inset Grouped Card: Schedule (Date & Time)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SystemSurface,
                    border = BorderStroke(0.5.dp, SystemDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Date Header Row
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

                        // Quick Date Pills
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

                            // Weekend preset
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

                        // Inset Hairline Divider
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            thickness = 0.5.dp,
                            color = SystemDivider
                        )

                        // Time Header Row
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

                        // Inline Clock Selector Accordion
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

                // 5. Inset Grouped Card: Priority & Category
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

                            // iOS-style 4-Segmented Control
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

                        // Inset Hairline Divider
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

                            // Category Chips Carousel
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

                                // + New Category Chip Button
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

                // 6. Prominent Primary Bottom Button (Thumb Reach)
                Button(
                    onClick = { submitTask() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SystemBlue,
                        disabledContainerColor = SystemBlue.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = canSubmit,
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
                            text = "Create Task",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 7. Floating Apple Keyboard Accessory Toolbar
            Surface(
                color = SystemSurface.copy(alpha = 0.98f),
                tonalElevation = 3.dp,
                border = BorderStroke(0.5.dp, SystemDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Date Toggle
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedDate == "Tomorrow") SystemBlueLight else SystemGroupedBackground,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    HapticManager.perform(context, HapticType.CLICK)
                                    if (selectedDate == "Today") {
                                        selectedDate = "Tomorrow"
                                        selectedEpochDay = currentEpochDay + 1
                                    } else {
                                        selectedDate = "Today"
                                        selectedEpochDay = currentEpochDay
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = "Quick Date",
                                    tint = if (selectedDate == "Tomorrow") SystemBlue else SystemLabelSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = selectedDate,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedDate == "Tomorrow") SystemBlue else SystemLabelPrimary
                                )
                            }
                        }

                        // Quick Priority Cycle
                        val prioEnum = TaskPriority.fromString(selectedPriority)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedPriority != "NONE") Color(prioEnum.badgeBgHex) else SystemGroupedBackground,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    HapticManager.perform(context, HapticType.CLICK)
                                    selectedPriority = when (selectedPriority) {
                                        "NONE" -> "LOW"
                                        "LOW" -> "MEDIUM"
                                        "MEDIUM" -> "HIGH"
                                        else -> "NONE"
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Flag,
                                    contentDescription = "Priority",
                                    tint = if (selectedPriority != "NONE") Color(prioEnum.colorHex) else SystemLabelSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                if (selectedPriority != "NONE") {
                                    Text(
                                        text = prioEnum.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(prioEnum.colorHex)
                                    )
                                }
                            }
                        }

                        // Quick Category Indicator
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SystemGroupedBackground,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    HapticManager.perform(context, HapticType.CLICK)
                                    val currentIdx = categoriesList.indexOfFirst { it.name.equals(selectedCategory, ignoreCase = true) }
                                    if (categoriesList.isNotEmpty()) {
                                        val nextIdx = (currentIdx + 1) % categoriesList.size
                                        selectedCategory = categoriesList[nextIdx].name
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Category,
                                    contentDescription = "Quick List",
                                    tint = SystemBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = selectedCategory,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SystemLabelPrimary
                                )
                            }
                        }
                    }

                    // Quick Right Add Action Button
                    if (canSubmit) {
                        Surface(
                            shape = CircleShape,
                            color = SystemBlue,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { submitTask() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Submit",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
