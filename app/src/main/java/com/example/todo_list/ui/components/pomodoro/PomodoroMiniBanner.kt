package com.example.todo_list.ui.components.pomodoro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.manager.PomodoroMode
import com.example.todo_list.manager.PomodoroTimerManager
import com.example.todo_list.ui.theme.TFHaptics
import com.example.todo_list.ui.theme.TFTheme

/**
 * Apple Dynamic Island / Now Playing style mini-banner for active Pomodoro sessions.
 * Appears docked subtly above the bottom navigation bar when a focus timer is running or paused.
 */
@Composable
fun PomodoroMiniBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = PomodoroTimerManager.isRunning || PomodoroTimerManager.isPaused
    val context = LocalContext.current
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        val mode = PomodoroTimerManager.currentMode
        val isWork = mode == PomodoroMode.WORK
        val modeColor = if (isWork) accentRoles.accent else colors.green
        val taskTitle = PomodoroTimerManager.activeTask?.title?.lineSequence()?.firstOrNull() ?: mode.label

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = modeColor.copy(alpha = 0.25f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(colors.card)
                .clickable {
                    TFHaptics.light(context)
                    onClick()
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Icon + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(modeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Pomodoro",
                            tint = modeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = taskTitle,
                            style = TFTheme.typography.subheadline,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.labelPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isWork) "Focus Session" else mode.label,
                            style = TFTheme.typography.caption,
                            color = modeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Time remaining & Play/Pause mini control
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = PomodoroTimerManager.formatRemainingTime(),
                        style = TFTheme.typography.body.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = colors.labelPrimary
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.fillControl)
                            .clickable {
                                TFHaptics.medium(context)
                                if (PomodoroTimerManager.isRunning) {
                                    PomodoroTimerManager.pauseTimer()
                                } else {
                                    PomodoroTimerManager.startTimer()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (PomodoroTimerManager.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (PomodoroTimerManager.isRunning) "Pause" else "Resume",
                            tint = colors.labelPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
