package com.example.todo_list.ui.screens.pomodoro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.manager.PomodoroMode
import com.example.todo_list.manager.PomodoroTimerManager
import com.example.todo_list.model.TaskItem
import com.example.todo_list.ui.components.primitives.TFButton
import com.example.todo_list.ui.components.primitives.TFButtonType
import com.example.todo_list.ui.components.primitives.TFCardGroup
import com.example.todo_list.ui.components.primitives.TFGroupDivider
import com.example.todo_list.ui.components.primitives.TFSegmentedControl
import com.example.todo_list.ui.theme.*

/**
 * Apple iOS HIG Focus Hub Bottom Sheet for Pomodoro Focus Timer.
 * Features:
 * - Minimalist Apple Stopwatch circular progress indicator with smooth spring animation
 * - Tabular monospaced timer typography
 * - Active task connection with direct completion trigger & XP reward
 * - Work / Short Break / Long Break segmented modes
 * - Customization settings accordion (durations, sounds)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroTimerSheet(
    onDismiss: () -> Unit,
    onCompleteActiveTask: ((TaskItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles
    val typography = TFTheme.typography

    val currentMode = PomodoroTimerManager.currentMode
    val isRunning = PomodoroTimerManager.isRunning
    val activeTask = PomodoroTimerManager.activeTask
    val progressRatio = PomodoroTimerManager.getProgressRatio()
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = TFMotion.snappy(),
        label = "pomodoro_progress"
    )

    var showSettings by remember { mutableStateOf(false) }

    // Session duration local editing states for settings
    var editWorkMins by remember(PomodoroTimerManager.workDurationMinutes) { mutableIntStateOf(PomodoroTimerManager.workDurationMinutes) }
    var editShortBreakMins by remember(PomodoroTimerManager.shortBreakDurationMinutes) { mutableIntStateOf(PomodoroTimerManager.shortBreakDurationMinutes) }
    var editLongBreakMins by remember(PomodoroTimerManager.longBreakDurationMinutes) { mutableIntStateOf(PomodoroTimerManager.longBreakDurationMinutes) }
    var editSoundEnabled by remember(PomodoroTimerManager.isSoundEnabled) { mutableStateOf(PomodoroTimerManager.isSoundEnabled) }

    val modeColor = when (currentMode) {
        PomodoroMode.WORK -> accentRoles.accent
        PomodoroMode.SHORT_BREAK -> colors.green
        PomodoroMode.LONG_BREAK -> colors.blue
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 10.dp),
                color = colors.separator,
                shape = CircleShape
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 5.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        TFHaptics.light(context)
                        showSettings = !showSettings
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Timer Settings",
                        tint = if (showSettings) modeColor else colors.labelSecondary
                    )
                }

                Text(
                    text = "Focus Timer",
                    style = typography.headline,
                    color = colors.labelPrimary
                )

                IconButton(
                    onClick = {
                        TFHaptics.light(context)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.labelSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Selector Tabs (Work / Short Break / Long Break)
            val modeItems = listOf("Focus", "Short Break", "Long Break")
            val selectedModeIndex = when (currentMode) {
                PomodoroMode.WORK -> 0
                PomodoroMode.SHORT_BREAK -> 1
                PomodoroMode.LONG_BREAK -> 2
            }

            TFSegmentedControl(
                items = modeItems,
                selectedIndex = selectedModeIndex,
                onItemSelected = { index ->
                    TFHaptics.selection(context)
                    val targetMode = when (index) {
                        0 -> PomodoroMode.WORK
                        1 -> PomodoroMode.SHORT_BREAK
                        2 -> PomodoroMode.LONG_BREAK
                        else -> PomodoroMode.WORK
                    }
                    PomodoroTimerManager.switchToMode(targetMode)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Settings Sheet view if toggled
            AnimatedVisibility(visible = showSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    TFCardGroup {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Focus Duration", style = typography.body, color = colors.labelPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$editWorkMins min", style = typography.subheadline, color = modeColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                DurationStepper(
                                    value = editWorkMins,
                                    onValueChange = {
                                        editWorkMins = it
                                        PomodoroTimerManager.updateDurations(editWorkMins, editShortBreakMins, editLongBreakMins, editSoundEnabled)
                                    },
                                    min = 5,
                                    max = 90,
                                    step = 5
                                )
                            }
                        }
                        TFGroupDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Short Break", style = typography.body, color = colors.labelPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$editShortBreakMins min", style = typography.subheadline, color = colors.green)
                                Spacer(modifier = Modifier.width(8.dp))
                                DurationStepper(
                                    value = editShortBreakMins,
                                    onValueChange = {
                                        editShortBreakMins = it
                                        PomodoroTimerManager.updateDurations(editWorkMins, editShortBreakMins, editLongBreakMins, editSoundEnabled)
                                    },
                                    min = 1,
                                    max = 30,
                                    step = 1
                                )
                            }
                        }
                        TFGroupDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Long Break", style = typography.body, color = colors.labelPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$editLongBreakMins min", style = typography.subheadline, color = colors.blue)
                                Spacer(modifier = Modifier.width(8.dp))
                                DurationStepper(
                                    value = editLongBreakMins,
                                    onValueChange = {
                                        editLongBreakMins = it
                                        PomodoroTimerManager.updateDurations(editWorkMins, editShortBreakMins, editLongBreakMins, editSoundEnabled)
                                    },
                                    min = 5,
                                    max = 45,
                                    step = 5
                                )
                            }
                        }
                    }
                }
            }

            // Circular Timer Dial
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = colors.fillControl,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Progress Arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = modeColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Center Text Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = PomodoroTimerManager.formatRemainingTime(),
                        style = typography.largeTitle.copy(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = colors.labelPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRunning) "IN PROGRESS" else if (PomodoroTimerManager.isPaused) "PAUSED" else "READY",
                        style = typography.caption.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = modeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Active Task Association Badge
            if (activeTask != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.fillControl)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = modeColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = activeTask.title.lineSequence().firstOrNull() ?: "",
                                    style = typography.subheadline,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.labelPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Active Focus Task",
                                    style = typography.caption,
                                    color = colors.labelSecondary
                                )
                            }
                        }

                        if (onCompleteActiveTask != null) {
                            TextButton(
                                onClick = {
                                    TFHaptics.medium(context)
                                    onCompleteActiveTask(activeTask)
                                    PomodoroTimerManager.clearActiveTask()
                                }
                            ) {
                                Text("Complete", color = colors.green, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Cycle Indicator: 4 dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isDone = i < (PomodoroTimerManager.completedPomodorosInCycle % 4)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isDone) modeColor else colors.controlStroke)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${PomodoroTimerManager.completedPomodorosInCycle % 4}/4 Focus Sessions",
                    style = typography.caption,
                    color = colors.labelSecondary
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Controls: Play/Pause, Reset, Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                IconButton(
                    onClick = {
                        TFHaptics.light(context)
                        PomodoroTimerManager.resetTimer()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.fillControl)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = colors.labelPrimary
                    )
                }

                // Play / Pause Circle (Apple Large Action Button)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = modeColor.copy(alpha = 0.35f)
                        )
                        .clip(CircleShape)
                        .background(modeColor)
                        .clickable {
                            TFHaptics.medium(context)
                            if (isRunning) {
                                PomodoroTimerManager.pauseTimer()
                            } else {
                                PomodoroTimerManager.startTimer()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Start",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Skip Button
                IconButton(
                    onClick = {
                        TFHaptics.light(context)
                        PomodoroTimerManager.skipToNext()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.fillControl)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip to Next Interval",
                        tint = colors.labelPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DurationStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int
) {
    val colors = TFTheme.colors
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.fillControl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable {
                    if (value - step >= min) {
                        TFHaptics.selection(context)
                        onValueChange(value - step)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "-",
                style = TFTheme.typography.headline,
                fontWeight = FontWeight.Bold,
                color = if (value - step >= min) colors.labelPrimary else colors.labelTertiary
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable {
                    if (value + step <= max) {
                        TFHaptics.selection(context)
                        onValueChange(value + step)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                style = TFTheme.typography.headline,
                fontWeight = FontWeight.Bold,
                color = if (value + step <= max) colors.labelPrimary else colors.labelTertiary
            )
        }
    }
}
