package com.example.todo_list.ui.components.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.ai.*
import com.example.todo_list.manager.UserProfileManager
import com.example.todo_list.model.TaskItem
import com.example.todo_list.model.TaskListCategory
import com.example.todo_list.model.TaskPriority
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager
import kotlinx.coroutines.launch

/**
 * Apple iOS HIG TaskFlow Intelligence Modal Bottom Sheet.
 * Features 3 tabs:
 * 1. Schedule Plan (conflict detection, time-blocking, buffer suggestions)
 * 2. Priorities (Eisenhower classification with grounded rationale)
 * 3. Ask AI (grounded conversational schedule assistant)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFlowIntelligenceSheet(
    tasks: List<TaskItem>,
    categories: List<TaskListCategory>,
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onApplySchedule: (List<ScheduledTaskSlot>) -> Unit,
    onApplyPriorities: (Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profile = UserProfileManager.profile
    val taskRepository = remember { com.example.todo_list.data.repository.TaskRepository.getInstance(context) }

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0: Plan, 1: Priorities, 2: Ask AI
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var planResult by remember { mutableStateOf<SchedulePlanResult?>(null) }
    var priorityResult by remember { mutableStateOf<PrioritySuggestionResult?>(null) }

    // Chat state
    var chatQuery by remember { mutableStateOf("") }
    var chatMessages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text = "Hello ${profile.name.split(" ").firstOrNull() ?: ""}! I am TaskFlow Intelligence. I have reviewed your ${tasks.filter { !it.isCompleted }.size} pending tasks. What would you like to plan or optimize today?",
                    isUser = false
                )
            )
        )
    }
    var isChatThinking by remember { mutableStateOf(false) }

    // Load initial Plan and Priorities concurrently
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            val plan = TaskFlowIntelligenceService.generateSchedulePlan(tasks, categories, profile)
            val priorities = TaskFlowIntelligenceService.suggestPriorities(tasks, categories, profile)
            planResult = plan
            priorityResult = priorities
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to generate plan"
        } finally {
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemGroupedBackground,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            // Drag Indicator Bar
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 12.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(SystemGray2.copy(alpha = 0.5f))
                    .align(Alignment.CenterHorizontally)
            )

            // Apple Intelligence Header
            IntelligenceHeader(
                selectedModel = AiConfigurationManager.selectedModel,
                isFallback = planResult?.isFallback == true || priorityResult?.isFallback == true,
                onClose = onDismiss
            )

            Spacer(modifier = Modifier.height(14.dp))

            // iOS Segmented Tab Control
            IntelligenceSegmentedTabs(
                selectedTab = selectedTab,
                onTabSelected = {
                    HapticManager.performClick(context)
                    selectedTab = it
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Loading / Error / Content states
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = SystemBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Analyzing your schedule & tasks...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = SystemLabelSecondary
                        )
                    }
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = SystemRed,
                        fontSize = 15.sp
                    )
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> PlanTabContent(
                            plan = planResult,
                            onApplySchedule = {
                                planResult?.slots?.let { slots ->
                                    HapticManager.performSuccess(context)
                                    onApplySchedule(slots)
                                    onDismiss()
                                }
                            }
                        )
                        1 -> PrioritiesTabContent(
                            result = priorityResult,
                            onApplyPriorities = {
                                priorityResult?.suggestions?.let { list ->
                                    HapticManager.performSuccess(context)
                                    val map = list.associate { it.taskId to it.suggestedPriority.key }
                                    onApplyPriorities(map)
                                    onDismiss()
                                }
                            }
                        )
                        2 -> {
                            val handleUserQuery: (String) -> Unit = { queryText ->
                                if (queryText.isNotBlank()) {
                                    val q = queryText.trim()
                                    chatQuery = ""
                                    chatMessages = chatMessages + ChatMessage(text = q, isUser = true)
                                    isChatThinking = true
                                    coroutineScope.launch {
                                        try {
                                            if (com.example.todo_list.utils.TaskScheduleParser.isTaskCreationIntent(q)) {
                                                val cleanPrompt = com.example.todo_list.utils.TaskScheduleParser.stripTaskIntentPrefix(q)
                                                val parsed = com.example.todo_list.utils.TaskScheduleParser.parse(
                                                    cleanPrompt,
                                                    is24Hour = false,
                                                    knownCategories = categories.map { it.name }
                                                )

                                                val title = parsed.cleanedTitle.replaceFirstChar { it.uppercase() }
                                                val category = parsed.suggestedCategory ?: categories.firstOrNull()?.name ?: "Personal"
                                                val date = parsed.suggestedDate ?: "Today"
                                                val time = parsed.suggestedTime ?: "09:00 AM"
                                                val priorityStr = when (parsed.suggestedPriority?.uppercase()) {
                                                    "HIGH" -> "HIGH"
                                                    "LOW" -> "LOW"
                                                    else -> "MEDIUM"
                                                }

                                                val newTask = TaskItem(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    title = title,
                                                    category = category,
                                                    date = date,
                                                    time = time,
                                                    priority = priorityStr,
                                                    epochDay = parsed.suggestedEpochDay ?: (System.currentTimeMillis() / (1000 * 60 * 60 * 24)),
                                                    userId = com.example.todo_list.security.AuthManager.currentUserId ?: ""
                                                )

                                                taskRepository.insertTask(newTask)
                                                com.example.todo_list.notification.TaskNotificationScheduler.schedule(context, newTask)
                                                UserProfileManager.addXp(50)
                                                HapticManager.performSuccess(context)

                                                val confirmationText = "✨ **Task Created Successfully!**\n\n" +
                                                        "📌 **$title**\n" +
                                                        "🗓️ **Date:** $date\n" +
                                                        "⏰ **Time:** $time\n" +
                                                        "📂 **Category:** $category\n" +
                                                        "⚡ **Priority:** $priorityStr\n\n" +
                                                        "Saved directly to your schedule."

                                                chatMessages = chatMessages + ChatMessage(text = confirmationText, isUser = false)
                                            } else {
                                                val res = TaskFlowIntelligenceService.askAssistant(q, tasks, categories, profile)
                                                chatMessages = chatMessages + ChatMessage(text = res.answer, isUser = false)
                                            }
                                        } catch (e: Exception) {
                                            chatMessages = chatMessages + ChatMessage(text = "Could not process request: ${e.message}", isUser = false)
                                        } finally {
                                            isChatThinking = false
                                        }
                                    }
                                }
                            }

                            AskAiTabContent(
                                messages = chatMessages,
                                isThinking = isChatThinking,
                                query = chatQuery,
                                onQueryChange = { chatQuery = it },
                                onSendQuery = { handleUserQuery(it) },
                                onChipClick = { chipText ->
                                    HapticManager.performClick(context)
                                    handleUserQuery(chipText)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
private fun IntelligenceHeader(
    selectedModel: String,
    isFallback: Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val appleGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF007AFF), Color(0xFFAF52DE), Color(0xFFFF2D55))
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Apple Intelligence",
                    tint = SystemBlue,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "TaskFlow Intelligence",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isFallback) SystemGray6 else Color(0x1A007AFF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isFallback) "⚡ Optimized on this device" else "✦ Enhanced with cloud AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFallback) SystemLabelSecondary else SystemBlue
                    )
                }
                Text(
                    text = "• 100% Grounded",
                    fontSize = 11.sp,
                    color = SystemLabelSecondary
                )
            }
        }

        IconButton(
            onClick = {
                HapticManager.performClick(context)
                onClose()
            },
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SystemGray5)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = SystemLabelSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun IntelligenceSegmentedTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SystemSurfaceSecondary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
        ) {
            val tabs = listOf(
                Pair("Schedule Plan", Icons.Outlined.Schedule),
                Pair("Priorities", Icons.Outlined.Flag),
                Pair("Ask AI", Icons.Outlined.ChatBubbleOutline)
            )

            tabs.forEachIndexed { index, (title, icon) ->
                val isSelected = selectedTab == index
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SystemSurface else Color.Transparent)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) SystemBlue else SystemLabelSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) SystemLabelPrimary else SystemLabelSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanTabContent(
    plan: SchedulePlanResult?,
    onApplySchedule: () -> Unit
) {
    if (plan == null) return

    val changedCount = plan.slots.count { it.isTimeChanged }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Focus Status & Summary Card
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SystemSurface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Today's Execution Strategy",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemLabelPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x1A007AFF))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = plan.focusStatusRecommendation,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SystemBlue
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = plan.summary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = SystemLabelSecondary
                        )
                    }
                }
            }

            // Conflict Warning Alert (if any)
            if (plan.conflicts.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x1AFF9500),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x4DFF9500)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = "Conflicts",
                                    tint = Color(0xFFFF9500),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${plan.conflicts.size} Schedule Conflict(s) Detected",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            plan.conflicts.forEach { conflict ->
                                Text(
                                    text = "• ${conflict.timeSlot}: ${conflict.conflictingTaskTitles.joinToString(" & ")}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SystemLabelPrimary
                                )
                                Text(
                                    text = "  ➔ ${conflict.resolutionSuggestion}",
                                    fontSize = 12.sp,
                                    color = SystemLabelSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // Timeline Items Card
            item {
                Text(
                    text = "OPTIMIZED TIMELINE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SystemLabelSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SystemSurface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (plan.slots.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No pending tasks to schedule.",
                                    fontSize = 14.sp,
                                    color = SystemLabelSecondary
                                )
                            }
                        } else {
                            plan.slots.forEachIndexed { index, slot ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = slot.taskTitle,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SystemLabelPrimary,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (slot.isTimeChanged) {
                                                Text(
                                                    text = slot.originalTime,
                                                    fontSize = 12.sp,
                                                    color = SystemLabelTertiary,
                                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                                )
                                                Text(
                                                    text = "➔",
                                                    fontSize = 11.sp,
                                                    color = SystemLabelSecondary
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (slot.isTimeChanged) Color(0x1AFF9500) else SystemGray6)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = slot.suggestedTime,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (slot.isTimeChanged) Color(0xFFFF9500) else SystemLabelPrimary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = slot.rationale,
                                        fontSize = 12.sp,
                                        color = SystemLabelSecondary,
                                        lineHeight = 16.sp
                                    )
                                }

                                if (index < plan.slots.size - 1) {
                                    HorizontalDivider(
                                        color = SystemDivider,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Gaps & Buffers (if any)
            if (plan.gaps.isNotEmpty()) {
                item {
                    Text(
                        text = "REST & PRODUCTIVITY GAPS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SystemLabelSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SystemSurface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            plan.gaps.forEach { gap ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccessTime,
                                        contentDescription = "Gap",
                                        tint = SystemBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${gap.startTime} - ${gap.endTime} (${gap.durationMinutes}m): ${gap.recommendation}",
                                        fontSize = 13.sp,
                                        color = SystemLabelSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 1-Tap Apply Schedule Action
        Surface(
            color = SystemSurface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Button(
                onClick = onApplySchedule,
                enabled = plan.slots.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (changedCount > 0) "Apply Optimized Schedule ($changedCount Adjustments)" else "Confirm Schedule Plan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PrioritiesTabContent(
    result: PrioritySuggestionResult?,
    onApplyPriorities: () -> Unit
) {
    if (result == null) return

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SystemSurface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Auto-Prioritization",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SystemLabelPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.summary,
                            fontSize = 14.sp,
                            color = SystemLabelSecondary
                        )
                    }
                }
            }

            item {
                Text(
                    text = "SUGGESTED TASK PRIORITIES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SystemLabelSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SystemSurface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (result.suggestions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No pending tasks to prioritize.",
                                    fontSize = 14.sp,
                                    color = SystemLabelSecondary
                                )
                            }
                        } else {
                            result.suggestions.forEachIndexed { index, item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = item.taskTitle,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SystemLabelPrimary,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(item.suggestedPriority.badgeBgHex))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = item.suggestedPriority.label,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(item.suggestedPriority.colorHex)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.rationale,
                                        fontSize = 12.sp,
                                        color = SystemLabelSecondary,
                                        lineHeight = 16.sp
                                    )
                                }

                                if (index < result.suggestions.size - 1) {
                                    HorizontalDivider(
                                        color = SystemDivider,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Apply All Priorities Action
        Surface(
            color = SystemSurface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Button(
                onClick = onApplyPriorities,
                enabled = result.suggestions.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Apply Priorities to All Tasks (${result.suggestions.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AskAiTabContent(
    messages: List<ChatMessage>,
    isThinking: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSendQuery: (String) -> Unit,
    onChipClick: (String) -> Unit
) {
    val quickChips = listOf(
        "Add task: Review proposal tomorrow 3pm #work",
        "Remind me to buy groceries tonight 6pm",
        "What should I do next?",
        "Do I have any schedule conflicts?",
        "Top priority tasks"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Action Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            items(quickChips) { chip ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SystemSurfaceSecondary,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                    modifier = Modifier.clickable { onChipClick(chip) }
                ) {
                    Text(
                        text = chip,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SystemLabelPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Chat Message Stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.isUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        color = if (isUser) SystemBlue else SystemSurface,
                        border = if (isUser) null else androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Text(
                            text = msg.text,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = if (isUser) Color.White else SystemLabelPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SystemSurface,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = SystemBlue,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Consulting your TaskFlow data...",
                                    fontSize = 13.sp,
                                    color = SystemLabelSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Input Field Bar
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SystemSurface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = SystemLabelPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendQuery(query) }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Ask anything about your tasks...",
                                fontSize = 15.sp,
                                color = SystemLabelSecondary
                            )
                        }
                        innerTextField()
                    }
                )

                IconButton(
                    onClick = { onSendQuery(query) },
                    enabled = query.isNotBlank() && !isThinking,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (query.isNotBlank() && !isThinking) SystemBlue else SystemGray2,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
