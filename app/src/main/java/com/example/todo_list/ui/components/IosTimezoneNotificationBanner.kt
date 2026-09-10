package com.example.todo_list.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.model.WorldLocation
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager
import kotlinx.coroutines.delay

/**
 * Authentic Apple iOS Dynamic Notification Banner for Time Zone & Location Detection.
 * Slides down smoothly when a new time zone or country is detected, allowing 1-tap confirmation or customization.
 */
@Composable
fun IosTimezoneNotificationBanner(
    visible: Boolean,
    location: WorldLocation?,
    onCustomizeClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(visible, location) {
        if (visible && location != null) {
            HapticManager.performSuccess(context)
            // Auto dismiss after 10 seconds if untouched
            delay(10000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible && location != null,
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
        if (location != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SystemSurface.copy(alpha = 0.96f),
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Row (App Icon, Section Label, "now")
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
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = "Globe",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Text(
                                text = "TASKFLOW TIME & REGION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemLabelSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = "now",
                            fontSize = 12.sp,
                            color = SystemLabelSecondary
                        )
                    }

                    // Content Information Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = location.flagEmoji,
                            fontSize = 28.sp
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Detected ${location.cityName}, ${location.countryName}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemLabelPrimary
                            )
                            Text(
                                text = "Standard Time: ${location.timeZoneAbbr} (${location.utcOffsetStr})",
                                fontSize = 13.sp,
                                color = SystemLabelSecondary
                            )
                        }
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "Keep" / "Confirm" Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SystemGray5,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    HapticManager.performClick(context)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Confirm",
                                    tint = SystemLabelPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Keep",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SystemLabelPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // "Customize" Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SystemBlueLight,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    HapticManager.performClick(context)
                                    onCustomizeClick()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = "Customize",
                                    tint = SystemBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Change Region",
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
}
