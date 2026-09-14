package com.example.todo_list.ui.components.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager
import kotlinx.coroutines.delay

/**
 * Authentic Apple iOS Dynamic Push Notification Banner.
 * Simulates incoming SMS / Verification alert with 30-Second Countdown and 1-Tap Autofill support.
 */
@Composable
fun IosNotificationBanner(
    visible: Boolean,
    otpCode: String,
    onAutofillClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var countdownSeconds by remember(visible, otpCode) { mutableIntStateOf(30) }

    LaunchedEffect(visible, otpCode) {
        if (visible && otpCode.isNotEmpty()) {
            HapticManager.performSuccess(context)
            countdownSeconds = 30
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds--
            }
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible && otpCode.isNotEmpty(),
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SystemSurface.copy(alpha = 0.96f),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clickable {
                    HapticManager.performClick(context)
                    onAutofillClick(otpCode)
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row (App Icon, App Title, Countdown Timer Pill, Close Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SystemBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = "Security",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Text(
                            text = "TASKFLOW SECURITY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SystemLabelSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Countdown Timer Pill & Dismiss Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (countdownSeconds <= 10) SystemOrange.copy(alpha = 0.14f)
                                    else SystemBlue.copy(alpha = 0.10f)
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = if (countdownSeconds <= 10) Icons.Outlined.HourglassBottom else Icons.Outlined.Timer,
                                contentDescription = "Timer",
                                tint = if (countdownSeconds <= 10) SystemOrange else SystemBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${countdownSeconds}s",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (countdownSeconds <= 10) SystemOrange else SystemBlue
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(SystemDivider.copy(alpha = 0.6f))
                                .clickable {
                                    HapticManager.performClick(context)
                                    onDismiss()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = SystemLabelSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Message Body
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Verification Code: $otpCode",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SystemLabelPrimary
                        )
                        Text(
                            text = "Valid for $countdownSeconds seconds. Tap Autofill to enter.",
                            fontSize = 13.sp,
                            color = if (countdownSeconds <= 10) SystemOrange else SystemLabelSecondary
                        )
                    }

                    // 1-Tap Autofill Chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SystemBlueLight,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                HapticManager.performSuccess(context)
                                onAutofillClick(otpCode)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MarkEmailRead,
                                contentDescription = "Autofill",
                                tint = SystemBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Autofill",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemBlue
                            )
                        }
                    }
                }
            }
        }
    }
}
