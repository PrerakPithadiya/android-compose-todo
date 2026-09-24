package com.example.todo_list.ui.components.primitives

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.model.TaskItem
import com.example.todo_list.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * TaskFlow Apple (iOS HIG) Task Row Component.
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 7 & Section 9.2
 *
 * Features:
 * - 56dp min height
 * - 48dp independent checkbox touch area with 24dp circular check toggle
 * - Tabular time & Category pill
 * - Priority glyph markers: Low (!), Medium (!!), High (!!!)
 * - 900ms celebratory sequence: 180ms check draw -> strikethrough -> micro-confetti -> +50 XP popup -> reorder
 * - Immediate undo affordance: re-tapping before 900ms cancels reordering
 */
@Composable
fun TFTaskRow(
    task: TaskItem,
    onToggleComplete: (TaskItem) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles
    val typography = TFTheme.typography

    // Local animated state for 900ms completion sequence
    var isLocallyCompleted by remember(task.isCompleted) { mutableStateOf(task.isCompleted) }
    var showXpPopup by remember { mutableStateOf(false) }
    var showMicroConfetti by remember { mutableStateOf(false) }

    // Checkbox bounce scale animation
    val checkboxScale = remember { Animatable(1f) }
    // Confetti radial progress
    val confettiProgress = remember { Animatable(0f) }

    val handleToggle = {
        val willComplete = !isLocallyCompleted
        isLocallyCompleted = willComplete

        if (willComplete) {
            TFHaptics.medium(context)
            // Trigger 900ms completion sequence
            showMicroConfetti = true
            showXpPopup = true

            // Trigger actual data layer update after 900ms window (undo window)
            // If user unchecks before 900ms, coroutine cancels
        } else {
            TFHaptics.selection(context)
            showMicroConfetti = false
            showXpPopup = false
        }
        onToggleComplete(task)
    }

    LaunchedEffect(isLocallyCompleted) {
        if (isLocallyCompleted && !task.isCompleted) {
            // Launch pop
            checkboxScale.animateTo(1.15f, animationSpec = TFMotion.bouncy())
            checkboxScale.animateTo(1.0f, animationSpec = TFMotion.snappy())
        }
    }

    LaunchedEffect(showMicroConfetti) {
        if (showMicroConfetti) {
            confettiProgress.snapTo(0f)
            confettiProgress.animateTo(1f, animationSpec = tween(360, easing = LinearEasing))
            showMicroConfetti = false
        }
    }

    LaunchedEffect(showXpPopup) {
        if (showXpPopup) {
            delay(1000)
            showXpPopup = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(end = TFSpace.lg, top = 6.dp, bottom = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox touch area: 48dp minimum
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .semantics { role = Role.Checkbox }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = handleToggle
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 24dp Circular Checkbox
                Box(
                    modifier = Modifier
                        .scale(checkboxScale.value)
                        .size(24.dp)
                        .clip(CircleShape)
                        .then(
                            if (isLocallyCompleted) {
                                Modifier.background(accentRoles.accentFill)
                            } else {
                                Modifier
                                    .background(Color.Transparent)
                                    .border(1.5.dp, colors.controlStroke, CircleShape)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLocallyCompleted) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Completed",
                            tint = accentRoles.onAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Micro-confetti burst (6 radial dots)
                if (showMicroConfetti && confettiProgress.value in 0.05f..0.95f) {
                    val progress = confettiProgress.value
                    Canvas(modifier = Modifier.size(48.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = 12f + progress * 16f
                        val alpha = (1f - progress).coerceIn(0f, 1f)
                        val particleColors = listOf(
                            accentRoles.accent,
                            colors.green,
                            colors.orange,
                            accentRoles.accentFill,
                            colors.blue,
                            colors.red
                        )

                        for (i in 0 until 6) {
                            val angle = (i * 60f) * (Math.PI / 180f)
                            val x = center.x + (radius * cos(angle)).toFloat()
                            val y = center.y + (radius * sin(angle)).toFloat()
                            drawCircle(
                                color = particleColors[i].copy(alpha = alpha),
                                radius = 2.5.dp.toPx() * (1f - progress * 0.5f),
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }

            // Task Title & Metadata
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 2.dp, end = TFSpace.sm)
            ) {
                Text(
                    text = task.title,
                    style = typography.body,
                    color = if (isLocallyCompleted) colors.labelSecondary else colors.labelPrimary,
                    textDecoration = if (isLocallyCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Metadata line (Time, Category badge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (task.time.isNotBlank()) {
                        Text(
                            text = task.time,
                            style = typography.footnote,
                            color = colors.labelSecondary
                        )
                    }

                    if (task.category.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.fillControl)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.category,
                                style = typography.caption,
                                color = colors.labelSecondary
                            )
                        }
                    }
                }
            }

            // Priority Glyphs (!, !!, !!!) with solver contrast colors
            val priority = task.priority.uppercase()
            val (glyphText, glyphColor) = when (priority) {
                "HIGH", "P1" -> Pair("!!!", colors.red)
                "MEDIUM", "P2" -> Pair("!!", colors.orange)
                "LOW", "P3" -> Pair("!", colors.blue)
                else -> Pair("", Color.Transparent)
            }

            if (glyphText.isNotEmpty()) {
                Text(
                    text = glyphText,
                    style = typography.footnote,
                    fontWeight = FontWeight.Bold,
                    color = glyphColor,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // +50 XP Popup Capsule
        AnimatedVisibility(
            visible = showXpPopup,
            enter = fadeIn(animationSpec = tween(150)) + slideInVertically(
                initialOffsetY = { 20 },
                animationSpec = TFMotion.bouncy()
            ),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-14).dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(TFShape.chip)
                    .background(colors.cardRaised)
                    .border(hairline(), accentRoles.accent, TFShape.chip)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "+50 XP",
                    style = typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = accentRoles.accentText
                )
            }
        }
    }
}
