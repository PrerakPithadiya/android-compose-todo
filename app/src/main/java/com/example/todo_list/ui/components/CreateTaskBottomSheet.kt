package com.example.todo_list.ui.components

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
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
import com.example.todo_list.model.EisenhowerQuadrant
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
import com.example.todo_list.utils.TaskScheduleParser
import com.example.todo_list.utils.TimeFormatHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Panels that can expand inline in Quick Capture mode (Stage 1).
 */
private enum class QuickPanelType {
    NONE,
    DATE_TIME,
    PRIORITY,
    CATEGORY,
    NOTES,
    AI_SUGGESTION
}

/**
 * Progressive-Disclosure Create Task Bottom Sheet (Apple iOS HIG Specification).
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 11.2
 *
 * Architecture:
 * - Stage 1 (Quick Capture): Compact half-sheet, automatic keyboard focus, live natural language
 *   schedule token extraction with removable chips, inline expanding panels (Date, Time, Priority,
 *   Category, Notes, AI), and quick toolbar docked right above keyboard.
 * - Stage 2 (Details): Expands on drag-up or "Details" action to full inset grouped form
 *   (Title + Notes, Date & Time pickers, Priority segmented control, Category carousel).
 * - Safe Dismissal: Intercepts unsaved changes with an iOS HIG discard confirmation sheet.
 * - Design Tokens: Full compliance with TFTheme colors, typography, shapes, and motion springs.
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
    val colors = TFTheme.colors
    val typography = TFTheme.typography
    val accentRoles = LocalAccentRoles.current
    val is24Hour = TimePreferencesManager.is24HourFormat
    val currentEpochDay = remember { System.currentTimeMillis() / (1000 * 60 * 60 * 24) }

    // Progressive Disclosure Mode: Stage 1 (Quick Capture) vs Stage 2 (Details)
    var isExpandedToDetails by remember { mutableStateOf(false) }
    var activeQuickPanel by remember { mutableStateOf(QuickPanelType.NONE) }

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
    var selectedQuadrant by remember { mutableStateOf(EisenhowerQuadrant.DO_FIRST.key) }
    var estimatedPomodoroSessions by remember { mutableIntStateOf(1) }
    var titleError by remember { mutableStateOf(false) }

    // Inline Pickers Visibility in Stage 2 Details
    var showInlineDatePicker by remember { mutableStateOf(false) }
    var showInlineTimePicker by remember { mutableStateOf(false) }
    var showCreateCategorySheet by remember { mutableStateOf(false) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    // Focus & Scroll
    val focusRequester = remember { FocusRequester() }
    val detailsScrollState = rememberScrollState()

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
        activeQuickPanel = QuickPanelType.NONE
    }

    // DatePicker state for calendar
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
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

    // TimePicker state for clock
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

    // Auto-focus keyboard on quick capture launch
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(180)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

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
                    priority = selectedPriority,
                    eisenhowerQuadrant = selectedQuadrant,
                    pomodoroSessionsCompleted = 0,
                    pomodoroEstimatedSessions = estimatedPomodoroSessions
                )
            )
        }
    }

    val handleDismissAttempt = {
        if (taskTitle.isNotBlank() || taskNotes.isNotBlank()) {
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

    // Discard Changes Confirmation Action Sheet (Section 11.2)
    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = {
                Text(
                    text = "Discard Unsaved Task?",
                    style = typography.headline,
                    color = colors.labelPrimary
                )
            },
            text = {
                Text(
                    text = "If you discard now, any details entered will be permanently lost.",
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
            // Navigation Bar Header (Cancel | New Task / Details | Add)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isExpandedToDetails) "Task Details" else "New Task",
                        style = typography.headline,
                        color = colors.labelPrimary
                    )

                    // Toggle button between Quick Capture & Details
                    Surface(
                        shape = TFShape.pill,
                        color = colors.fillControl,
                        modifier = Modifier
                            .clip(TFShape.pill)
                            .clickable {
                                HapticManager.perform(context, HapticType.CLICK)
                                isExpandedToDetails = !isExpandedToDetails
                                activeQuickPanel = QuickPanelType.NONE
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isExpandedToDetails) "Quick" else "Details",
                                style = typography.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = accentRoles.accentText
                            )
                            Icon(
                                imageVector = if (isExpandedToDetails) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                                contentDescription = "Toggle mode",
                                tint = accentRoles.accentText,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                TextButton(
                    onClick = { submitTask() },
                    enabled = canSubmit,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Add",
                        style = typography.headline,
                        fontWeight = FontWeight.Bold,
                        color = if (canSubmit) accentRoles.accentText else accentRoles.accentText.copy(alpha = 0.35f)
                    )
                }
            }

            HorizontalDivider(thickness = hairline(), color = colors.separator)

            // Content Area: Switch between Stage 1 (Quick Capture) and Stage 2 (Details)
            AnimatedContent(
                targetState = isExpandedToDetails,
                transitionSpec = {
                    fadeIn(animationSpec = TFMotion.standard()) togetherWith
                            fadeOut(animationSpec = TFMotion.standard())
                },
                label = "CreateTaskStageTransition"
            ) { inDetailsMode ->
                if (!inDetailsMode) {
                    // ──────────────────────────────────────────────
                    // STAGE 1: QUICK CAPTURE (Section 11.2)
                    // ──────────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TFSpace.lg, vertical = TFSpace.md),
                        verticalArrangement = Arrangement.spacedBy(TFSpace.md)
                    ) {
                        // Title Input Field (20sp title3, auto-grow to 3 lines)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(TFShape.card)
                                .background(colors.card)
                                .then(
                                    if (colors.isDark) {
                                        Modifier.border(hairline(), if (titleError) colors.red else colors.cardStroke, TFShape.card)
                                    } else {
                                        if (titleError) Modifier.border(hairline(), colors.red, TFShape.card) else Modifier
                                    }
                                )
                                .padding(horizontal = TFSpace.lg, vertical = TFSpace.md)
                        ) {
                            if (taskTitle.isEmpty()) {
                                Text(
                                    text = "What needs doing?",
                                    style = typography.title3,
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
                                    fontSize = 20.sp,
                                    lineHeight = 25.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.labelPrimary
                                ),
                                cursorBrush = SolidColor(accentRoles.accentText),
                                maxLines = 3,
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

                        // NLP Schedule Recognized Chips (Removable)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(TFSpace.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Date token chip
                            QuickRemovableChip(
                                icon = Icons.Outlined.CalendarMonth,
                                text = selectedDate,
                                isHighlighted = selectedDate != "Today",
                                onRemove = {
                                    selectedDate = "Today"
                                    selectedEpochDay = currentEpochDay
                                }
                            )

                            // Time token chip
                            QuickRemovableChip(
                                icon = Icons.Outlined.Schedule,
                                text = selectedTime,
                                isHighlighted = true,
                                onRemove = {
                                    selectedTime = if (is24Hour) "17:00" else "05:00 PM"
                                }
                            )

                            // Priority token chip
                            if (selectedPriority != "NONE") {
                                val prioGlyph = when (selectedPriority) {
                                    "LOW" -> "! Low"
                                    "MEDIUM" -> "!! Med"
                                    "HIGH" -> "!!! High"
                                    else -> ""
                                }
                                QuickRemovableChip(
                                    icon = Icons.Outlined.Flag,
                                    text = prioGlyph,
                                    isHighlighted = true,
                                    onRemove = { selectedPriority = "NONE" }
                                )
                            }

                            // Category token chip
                            QuickRemovableChip(
                                icon = Icons.Outlined.Category,
                                text = selectedCategory,
                                isHighlighted = selectedCategory != initialCategory,
                                onRemove = { selectedCategory = initialCategory }
                            )
                        }

                        // Inline Expandable Panels (animated with TFMotion.standard)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(animationSpec = TFMotion.standard())
                        ) {
                            when (activeQuickPanel) {
                                QuickPanelType.DATE_TIME -> {
                                    Surface(
                                        shape = TFShape.card,
                                        color = colors.card,
                                        border = if (colors.isDark) BorderStroke(hairline(), colors.cardStroke) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(TFSpace.md),
                                            verticalArrangement = Arrangement.spacedBy(TFSpace.md)
                                        ) {
                                            Text(
                                                text = "Schedule Date & Time",
                                                style = typography.headline,
                                                color = colors.labelPrimary
                                            )

                                            // Quick Date Presets
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
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
                                                            }
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            style = typography.caption,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) accentRoles.onAccent else colors.labelPrimary,
                                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                                        )
                                                    }
                                                }

                                                // Weekend Preset
                                                val isWeekendSelected = selectedDate.startsWith("Weekend")
                                                Surface(
                                                    shape = TFShape.pill,
                                                    color = if (isWeekendSelected) accentRoles.accentFill else colors.fillControl,
                                                    modifier = Modifier
                                                        .clip(TFShape.pill)
                                                        .clickable {
                                                            HapticManager.perform(context, HapticType.CLICK)
                                                            val cal = Calendar.getInstance()
                                                            val daysUntilSat = (Calendar.SATURDAY - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
                                                            val targetDays = if (daysUntilSat == 0) 7 else daysUntilSat
                                                            cal.add(Calendar.DAY_OF_YEAR, targetDays)
                                                            val fmt = SimpleDateFormat("MMM dd", Locale.getDefault())
                                                            selectedDate = "Weekend (${fmt.format(cal.time)})"
                                                            selectedEpochDay = cal.timeInMillis / (1000 * 60 * 60 * 24)
                                                        }
                                                ) {
                                                    Text(
                                                        text = "Weekend 🌅",
                                                        style = typography.caption,
                                                        fontWeight = if (isWeekendSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isWeekendSelected) accentRoles.onAccent else colors.labelPrimary,
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                                    )
                                                }
                                            }

                                            // Quick Times Carousel
                                            val quickTimes = remember(is24Hour) { TimeFormatHelper.getQuickTimes(is24Hour) }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(TFSpace.sm)
                                            ) {
                                                quickTimes.forEach { qTime ->
                                                    val isSelected = selectedTime == qTime
                                                    Surface(
                                                        shape = TFShape.pill,
                                                        color = if (isSelected) accentRoles.accentFill else colors.fillControl,
                                                        modifier = Modifier
                                                            .clip(TFShape.pill)
                                                            .clickable {
                                                                HapticManager.perform(context, HapticType.CLICK)
                                                                selectedTime = qTime
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
                                        }
                                    }
                                }

                                QuickPanelType.PRIORITY -> {
                                    Surface(
                                        shape = TFShape.card,
                                        color = colors.card,
                                        border = if (colors.isDark) BorderStroke(hairline(), colors.cardStroke) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(TFSpace.md),
                                            verticalArrangement = Arrangement.spacedBy(TFSpace.sm)
                                        ) {
                                            Text(
                                                text = "Priority Level",
                                                style = typography.headline,
                                                color = colors.labelPrimary
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
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
                                                            .weight(1f)
                                                            .height(32.dp)
                                                            .clip(TFShape.segmentedThumb)
                                                            .background(if (isSelected) colors.card else Color.Transparent)
                                                            .clickable {
                                                                HapticManager.perform(context, HapticType.CLICK)
                                                                selectedPriority = prio.key
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            style = typography.subheadline,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
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
                                    }
                                }

                                QuickPanelType.CATEGORY -> {
                                    Surface(
                                        shape = TFShape.card,
                                        color = colors.card,
                                        border = if (colors.isDark) BorderStroke(hairline(), colors.cardStroke) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(TFSpace.md),
                                            verticalArrangement = Arrangement.spacedBy(TFSpace.sm)
                                        ) {
                                            Text(
                                                text = "Assign to List",
                                                style = typography.headline,
                                                color = colors.labelPrimary
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(TFSpace.sm),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                categoriesList.forEach { cat ->
                                                    val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                                                    Surface(
                                                        shape = TFShape.pill,
                                                        color = if (isSelected) accentRoles.accentFill else colors.fillControl,
                                                        modifier = Modifier
                                                            .clip(TFShape.pill)
                                                            .clickable {
                                                                HapticManager.perform(context, HapticType.CLICK)
                                                                selectedCategory = cat.name
                                                            }
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                                        ) {
                                                            Text(cat.getEmoji(), fontSize = 13.sp)
                                                            Text(
                                                                text = cat.name,
                                                                style = typography.caption,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isSelected) accentRoles.onAccent else colors.labelPrimary
                                                            )
                                                        }
                                                    }
                                                }

                                                // + New List Button
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
                                }

                                QuickPanelType.NOTES -> {
                                    Surface(
                                        shape = TFShape.card,
                                        color = colors.card,
                                        border = if (colors.isDark) BorderStroke(hairline(), colors.cardStroke) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(TFSpace.md)
                                        ) {
                                            if (taskNotes.isEmpty()) {
                                                Text(
                                                    text = "Add notes or checklist details...",
                                                    style = typography.body,
                                                    color = colors.labelSecondary.copy(alpha = 0.5f)
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
                                                minLines = 3,
                                                maxLines = 6,
                                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                QuickPanelType.AI_SUGGESTION -> {
                                    Surface(
                                        shape = TFShape.card,
                                        color = accentRoles.accentContainer,
                                        border = BorderStroke(hairline(), accentRoles.accent),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(TFSpace.md),
                                            verticalArrangement = Arrangement.spacedBy(TFSpace.sm)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = "Smart Schedule",
                                                    tint = accentRoles.accentText,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Smart Suggestion: ${parsedSchedule.getSummaryPillText()}",
                                                    style = typography.headline,
                                                    color = accentRoles.accentText
                                                )
                                            }
                                            Text(
                                                text = "Parsed on-device from task title without sending data outside.",
                                                style = typography.caption,
                                                color = colors.labelSecondary
                                            )
                                            TFButton(
                                                text = "Apply Suggestion",
                                                onClick = { applySuggestion() },
                                                type = TFButtonType.FILLED,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                QuickPanelType.NONE -> { /* No inline panel active */ }
                            }
                        }

                        // Icon Toolbar (docked right above keyboard with 48dp touch targets)
                        Surface(
                            shape = TFShape.card,
                            color = colors.card,
                            border = if (colors.isDark) BorderStroke(hairline(), colors.cardStroke) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Date & Time
                                QuickToolbarIconButton(
                                    icon = Icons.Outlined.CalendarMonth,
                                    label = "Date",
                                    isActive = activeQuickPanel == QuickPanelType.DATE_TIME,
                                    hasValue = selectedDate != "Today",
                                    onClick = {
                                        activeQuickPanel = if (activeQuickPanel == QuickPanelType.DATE_TIME) QuickPanelType.NONE else QuickPanelType.DATE_TIME
                                    }
                                )

                                // Priority
                                QuickToolbarIconButton(
                                    icon = Icons.Outlined.Flag,
                                    label = "Priority",
                                    isActive = activeQuickPanel == QuickPanelType.PRIORITY,
                                    hasValue = selectedPriority != "NONE",
                                    onClick = {
                                        activeQuickPanel = if (activeQuickPanel == QuickPanelType.PRIORITY) QuickPanelType.NONE else QuickPanelType.PRIORITY
                                    }
                                )

                                // Category
                                QuickToolbarIconButton(
                                    icon = Icons.Outlined.Category,
                                    label = "List",
                                    isActive = activeQuickPanel == QuickPanelType.CATEGORY,
                                    hasValue = selectedCategory != initialCategory,
                                    onClick = {
                                        activeQuickPanel = if (activeQuickPanel == QuickPanelType.CATEGORY) QuickPanelType.NONE else QuickPanelType.CATEGORY
                                    }
                                )

                                // Notes
                                QuickToolbarIconButton(
                                    icon = Icons.Outlined.Description,
                                    label = "Notes",
                                    isActive = activeQuickPanel == QuickPanelType.NOTES,
                                    hasValue = taskNotes.isNotBlank(),
                                    onClick = {
                                        activeQuickPanel = if (activeQuickPanel == QuickPanelType.NOTES) QuickPanelType.NONE else QuickPanelType.NOTES
                                    }
                                )

                                // AI Suggestion
                                if (parsedSchedule.hasSuggestions) {
                                    QuickToolbarIconButton(
                                        icon = Icons.Default.AutoAwesome,
                                        label = "AI",
                                        isActive = activeQuickPanel == QuickPanelType.AI_SUGGESTION,
                                        hasValue = true,
                                        onClick = {
                                            activeQuickPanel = if (activeQuickPanel == QuickPanelType.AI_SUGGESTION) QuickPanelType.NONE else QuickPanelType.AI_SUGGESTION
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ──────────────────────────────────────────────
                    // STAGE 2: DETAILS (Inset Grouped Form - Sec 11.2)
                    // ──────────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(detailsScrollState)
                            .padding(vertical = TFSpace.md),
                        verticalArrangement = Arrangement.spacedBy(TFSpace.lg)
                    ) {
                        // Group 1: Title & Notes (min 80dp)
                        TFCardGroup(headerTitle = "TASK INFORMATION") {
                            // Title input
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
                                    keyboardActions = KeyboardActions(onDone = { submitTask() }),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            TFGroupDivider(startIndent = TFSpace.lg)

                            // Notes input (min 80dp)
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

                        // Group 2: Schedule (Date & Time with inline expanders)
                        TFCardGroup(headerTitle = "SCHEDULE") {
                            // Date row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        HapticManager.perform(context, HapticType.CLICK)
                                        val willOpen = !showInlineDatePicker
                                        showInlineDatePicker = willOpen
                                        if (willOpen) {
                                            showInlineTimePicker = false
                                        }
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
                                        val willOpen = !showInlineTimePicker
                                        showInlineTimePicker = willOpen
                                        if (willOpen) {
                                            showInlineDatePicker = false
                                        }
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

                            TFGroupDivider(startIndent = TFSpace.lg)

                            // Eisenhower Matrix Quadrant
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
                                                .background(accentRoles.accent.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.GridView,
                                                contentDescription = "Eisenhower Quadrant",
                                                tint = accentRoles.accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Eisenhower Matrix",
                                                style = typography.body,
                                                color = colors.labelPrimary
                                            )
                                            Text(
                                                text = EisenhowerQuadrant.fromString(selectedQuadrant).subtitle,
                                                style = typography.caption,
                                                color = colors.labelSecondary
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = TFShape.pill,
                                        color = Color(EisenhowerQuadrant.fromString(selectedQuadrant).badgeBgHex)
                                    ) {
                                        Text(
                                            text = EisenhowerQuadrant.fromString(selectedQuadrant).title,
                                            style = typography.caption,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(EisenhowerQuadrant.fromString(selectedQuadrant).colorHex),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(TFShape.segmentedTrack)
                                        .background(colors.fillControl)
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    EisenhowerQuadrant.entries.forEach { quad ->
                                        val isSelected = selectedQuadrant == quad.key
                                        val quadColor = Color(quad.colorHex)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(TFShape.segmentedThumb)
                                                .background(if (isSelected) colors.card else Color.Transparent)
                                                .clickable {
                                                    HapticManager.perform(context, HapticType.CLICK)
                                                    selectedQuadrant = quad.key
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = quad.actionLabel,
                                                style = typography.caption,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) quadColor else colors.labelSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            TFGroupDivider(startIndent = TFSpace.lg)

                            // Pomodoro Estimate Stepper
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
                                            imageVector = Icons.Outlined.Timer,
                                            contentDescription = "Pomodoro",
                                            tint = colors.red,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Pomodoro Focus",
                                            style = typography.body,
                                            color = colors.labelPrimary
                                        )
                                        Text(
                                            text = "Estimated 25m intervals",
                                            style = typography.caption,
                                            color = colors.labelSecondary
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.fillControl),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (estimatedPomodoroSessions > 1) {
                                                HapticManager.perform(context, HapticType.CLICK)
                                                estimatedPomodoroSessions--
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("-", style = typography.headline, color = colors.labelPrimary)
                                    }

                                    Text(
                                        text = "$estimatedPomodoroSessions 🍅",
                                        style = typography.subheadline,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.labelPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            if (estimatedPomodoroSessions < 12) {
                                                HapticManager.perform(context, HapticType.CLICK)
                                                estimatedPomodoroSessions++
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("+", style = typography.headline, color = colors.labelPrimary)
                                    }
                                }
                            }
                        }

                        // Prominent Primary Button at Bottom of Details (Thumb reach)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = TFSpace.lg, vertical = TFSpace.md)
                        ) {
                            TFButton(
                                text = "Create Task",
                                onClick = { submitTask() },
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
    }
}

/**
 * Compact removable chip for live NLP recognized schedule tokens in Stage 1 Quick Capture.
 */
@Composable
private fun QuickRemovableChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isHighlighted: Boolean,
    onRemove: () -> Unit
) {
    val colors = TFTheme.colors
    val typography = TFTheme.typography
    val accentRoles = LocalAccentRoles.current

    Surface(
        shape = TFShape.pill,
        color = if (isHighlighted) accentRoles.accentContainer else colors.fillControl,
        border = if (isHighlighted) BorderStroke(hairline(), accentRoles.accent) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlighted) accentRoles.accentText else colors.labelSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = typography.caption,
                fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isHighlighted) accentRoles.accentText else colors.labelPrimary
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = if (isHighlighted) accentRoles.accentText else colors.labelSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Toolbar icon button for quick accessory actions above keyboard.
 */
@Composable
private fun QuickToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    hasValue: Boolean,
    onClick: () -> Unit
) {
    val colors = TFTheme.colors
    val accentRoles = LocalAccentRoles.current

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isActive) accentRoles.accentContainer else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive || hasValue) accentRoles.accentText else colors.labelSecondary,
            modifier = Modifier.size(22.dp)
        )

        // Accent indicator dot if value is customized
        if (hasValue && !isActive) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .clip(CircleShape)
                    .background(accentRoles.accentFill)
            )
        }
    }
}
