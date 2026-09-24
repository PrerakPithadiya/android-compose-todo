package com.example.todo_list.ui.components

import androidx.compose.animation.*
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
import androidx.compose.material.icons.outlined.*
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
import com.example.todo_list.ui.components.primitives.TFButton
import com.example.todo_list.ui.components.primitives.TFButtonType
import com.example.todo_list.ui.components.primitives.TFCardGroup
import com.example.todo_list.ui.components.primitives.TFGroupDivider
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
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 11.2 (Stage 2 Form)
 *
 * Features:
 * - Authentic iOS Navigation bar (Cancel / Task Details / Save)
 * - Inset grouped form cards (Title & Notes, Schedule, Priority & Category, Destructive Delete)
 * - Inline calendar accordion and interactive clock selector
 * - Apple haptic feedback and real spring animations
 * - Intercepts unsaved changes and prompts for confirmation on cancel/dismiss
 * - Destructive action confirmation dialog for task deletion
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
    val colors = TFTheme.colors
    val typography = TFTheme.typography
    val accentRoles = LocalAccentRoles.current
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

    // Pickers & Dialogs
    var showInlineDatePicker by remember { mutableStateOf(false) }
    var showInlineTimePicker by remember { mutableStateOf(false) }
    var showCreateCategorySheet by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // DatePicker state
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

    // TimePicker state
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

    val canSubmit = taskTitle.isNotBlank()

    val hasChanges = remember(taskTitle, taskNotes, selectedCategory, selectedDate, selectedTime, selectedPriority) {
        taskTitle != initialTitleParts.first ||
                taskNotes != initialTitleParts.second ||
                selectedCategory != task.category ||
                selectedDate != task.date ||
                selectedTime != TimeFormatHelper.formatTimeForDisplay(task.time, is24Hour) ||
                selectedPriority != task.priority
    }

    val saveTask = {
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

    val handleDismissAttempt = {
        if (hasChanges) {
            showDiscardConfirmation = true
        } else {
            onDismiss()
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

    // Discard Confirmation Dialog
    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = {
                Text(
                    text = "Discard Changes?",
                    style = typography.headline,
                    color = colors.labelPrimary
                )
            },
            text = {
                Text(
                    text = "You have unsaved changes that will be lost if you leave now.",
                    style = typography.subheadline,
                    color = colors.labelSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "Discard",
                        style = typography.headline,
                        color = colors.red,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text(
                        text = "Keep Editing",
                        style = typography.body,
                        color = accentRoles.accentText
                    )
                }
            },
            containerColor = colors.cardRaised,
            shape = TFShape.button
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "Delete Task?",
                    style = typography.headline,
                    color = colors.labelPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this task? This action cannot be undone.",
                    style = typography.subheadline,
                    color = colors.labelSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        HapticManager.perform(context, HapticType.ERROR)
                        showDeleteConfirmation = false
                        onDeleteTask(task)
                    }
                ) {
                    Text(
                        text = "Delete",
                        style = typography.headline,
                        color = colors.red,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(
                        text = "Cancel",
                        style = typography.body,
                        color = colors.labelSecondary
                    )
                }
            },
            containerColor = colors.cardRaised,
            shape = TFShape.button
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { handleDismissAttempt() },
        sheetState = sheetState,
        containerColor = colors.canvas,
        shape = TFShape.sheet,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(colors.labelTertiary)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // Header Bar (Cancel | Task Details | Save)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = TFSpace.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        HapticManager.perform(context, HapticType.CLICK)
                        handleDismissAttempt()
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = typography.body,
                        color = accentRoles.accentText
                    )
                }

                Text(
                    text = "Task Details",
                    style = typography.headline,
                    color = colors.labelPrimary
                )

                TextButton(
                    onClick = { saveTask() },
                    enabled = canSubmit,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Save",
                        style = typography.headline,
                        fontWeight = FontWeight.Bold,
                        color = if (canSubmit) accentRoles.accentText else accentRoles.accentText.copy(alpha = 0.35f)
                    )
                }
            }

            HorizontalDivider(thickness = hairline(), color = colors.separator)

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = TFSpace.md),
                verticalArrangement = Arrangement.spacedBy(TFSpace.lg)
            ) {
                // Group 1: Title & Notes
                TFCardGroup(headerTitle = "TASK INFORMATION") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TFSpace.lg, vertical = 14.dp)
                    ) {
                        if (taskTitle.isEmpty()) {
                            Text(
                                text = "What needs to be done?",
                                style = typography.body,
                                color = colors.labelSecondary.copy(alpha = 0.5f)
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
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.labelPrimary
                            ),
                            cursorBrush = SolidColor(accentRoles.accentText),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { saveTask() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    TFGroupDivider(startIndent = TFSpace.lg)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 80.dp)
                            .padding(horizontal = TFSpace.lg, vertical = 12.dp)
                    ) {
                        if (taskNotes.isEmpty()) {
                            Text(
                                text = "Notes or details...",
                                style = typography.subheadline,
                                color = colors.labelSecondary.copy(alpha = 0.45f)
                            )
                        }
                        BasicTextField(
                            value = taskNotes,
                            onValueChange = { taskNotes = it },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                color = colors.labelPrimary
                            ),
                            cursorBrush = SolidColor(accentRoles.accentText),
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Group 2: Schedule (Date & Time)
                TFCardGroup(headerTitle = "SCHEDULE") {
                    // Date row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticManager.perform(context, HapticType.CLICK)
                                showInlineDatePicker = !showInlineDatePicker
                            }
                            .padding(horizontal = TFSpace.lg, vertical = 12.dp),
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
                                    .background(accentRoles.accentContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = "Date",
                                    tint = accentRoles.accentText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Date",
                                style = typography.body,
                                color = colors.labelPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = TFShape.pill,
                                color = accentRoles.accentContainer
                            ) {
                                Text(
                                    text = selectedDate,
                                    style = typography.subheadline,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentRoles.accentText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Icon(
                                imageVector = if (showInlineDatePicker) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = colors.labelSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Quick Date Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TFSpace.lg, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(TFSpace.sm)
                    ) {
                        listOf(
                            "Today" to currentEpochDay,
                            "Tomorrow" to currentEpochDay + 1
                        ).forEach { (label, epoch) ->
                            val isSelected = selectedDate == label
                            Surface(
                                shape = TFShape.pill,
                                color = if (isSelected) accentRoles.accentFill else colors.fillControl,
                                modifier = Modifier
                                    .clip(TFShape.pill)
                                    .clickable {
                                        HapticManager.perform(context, HapticType.CLICK)
                                        selectedDate = label
                                        selectedEpochDay = epoch
                                        showInlineDatePicker = false
                                    }
                            ) {
                                Text(
                                    text = label,
                                    style = typography.caption,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentRoles.onAccent else colors.labelPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Inline Calendar Accordion
                    AnimatedVisibility(
                        visible = showInlineDatePicker,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        DatePicker(
                            state = datePickerState,
                            showModeToggle = false,
                            colors = DatePickerDefaults.colors(
                                selectedDayContainerColor = accentRoles.accentFill,
                                todayDateBorderColor = accentRoles.accentFill
                            )
                        )
                    }

                    TFGroupDivider(startIndent = TFSpace.lg)

                    // Time row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticManager.perform(context, HapticType.CLICK)
                                showInlineTimePicker = !showInlineTimePicker
                            }
                            .padding(horizontal = TFSpace.lg, vertical = 12.dp),
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
                                    .background(colors.orange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = "Time",
                                    tint = colors.orange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Time",
                                style = typography.body,
                                color = colors.labelPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = TFShape.pill,
                                color = accentRoles.accentContainer
                            ) {
                                Text(
                                    text = selectedTime,
                                    style = typography.subheadline,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentRoles.accentText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Icon(
                                imageVector = if (showInlineTimePicker) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = colors.labelSecondary,
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
                            .padding(horizontal = TFSpace.lg, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(TFSpace.sm)
                    ) {
                        quickTimes.forEach { qTime ->
                            val isSelected = selectedTime == qTime && !showInlineTimePicker
                            Surface(
                                shape = TFShape.pill,
                                color = if (isSelected) accentRoles.accentFill else colors.fillControl,
                                modifier = Modifier
                                    .clip(TFShape.pill)
                                    .clickable {
                                        HapticManager.perform(context, HapticType.CLICK)
                                        selectedTime = qTime
                                        showInlineTimePicker = false
                                    }
                            ) {
                                Text(
                                    text = qTime,
                                    style = typography.caption,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentRoles.onAccent else colors.labelPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Inline TimePicker Accordion
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
                                    clockDialColor = colors.canvas,
                                    clockDialSelectedContentColor = Color.White,
                                    clockDialUnselectedContentColor = colors.labelPrimary,
                                    selectorColor = accentRoles.accentFill,
                                    periodSelectorSelectedContainerColor = accentRoles.accentContainer,
                                    periodSelectorSelectedContentColor = accentRoles.accentText
                                )
                            )
                        }
                    }
                }

                // Group 3: Priority & Category
                TFCardGroup(headerTitle = "ORGANIZATION") {
                    // Priority
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TFSpace.lg, vertical = 12.dp),
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
                                    .background(colors.red.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Flag,
                                    contentDescription = "Priority",
                                    tint = colors.red,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Priority",
                                style = typography.body,
                                color = colors.labelPrimary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(TFShape.segmentedTrack)
                                .background(colors.fillControl)
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
                                        .clip(TFShape.segmentedThumb)
                                        .background(if (isSelected) colors.card else Color.Transparent)
                                        .clickable {
                                            HapticManager.perform(context, HapticType.CLICK)
                                            selectedPriority = prio.key
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = typography.caption,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            when (prio) {
                                                TaskPriority.LOW -> colors.blue
                                                TaskPriority.MEDIUM -> colors.orange
                                                TaskPriority.HIGH -> colors.red
                                                else -> colors.labelPrimary
                                            }
                                        } else colors.labelSecondary
                                    )
                                }
                            }
                        }
                    }

                    TFGroupDivider(startIndent = TFSpace.lg)

                    // Category
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TFSpace.lg, vertical = 12.dp),
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
                                        .background(accentRoles.accentContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Category,
                                        contentDescription = "List",
                                        tint = accentRoles.accentText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "List",
                                    style = typography.body,
                                    color = colors.labelPrimary
                                )
                            }

                            Text(
                                text = selectedCategory,
                                style = typography.subheadline,
                                fontWeight = FontWeight.SemiBold,
                                color = accentRoles.accentText
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(TFSpace.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            categoriesList.forEach { catItem ->
                                val isSelected = selectedCategory.equals(catItem.name, ignoreCase = true)
                                Surface(
                                    shape = TFShape.pill,
                                    color = if (isSelected) accentRoles.accentFill else colors.fillControl,
                                    modifier = Modifier
                                        .clip(TFShape.pill)
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
                                            style = typography.caption,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) accentRoles.onAccent else colors.labelPrimary
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = TFShape.pill,
                                color = accentRoles.accentContainer,
                                modifier = Modifier
                                    .clip(TFShape.pill)
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
                                        tint = accentRoles.accentText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "New List",
                                        style = typography.caption,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentRoles.accentText
                                    )
                                }
                            }
                        }
                    }
                }

                // Group 4: Destructive Delete Task Button (Centered, Section 11.2)
                TFCardGroup {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticManager.perform(context, HapticType.WARNING)
                                showDeleteConfirmation = true
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Task",
                                tint = colors.red,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Delete Task",
                                style = typography.headline,
                                color = colors.red,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Save Changes Primary Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = TFSpace.lg, vertical = TFSpace.sm)
                ) {
                    TFButton(
                        text = "Save Changes",
                        onClick = { saveTask() },
                        type = TFButtonType.FILLED,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = accentRoles.onAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        enabled = canSubmit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
