package com.example.todo_list.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.model.AchievementBadge
import com.example.todo_list.model.AchievementTier
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementDetailSheet(
    badge: AchievementBadge,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tierColor = Color(badge.tier.colorHex)
    val tierContainerColor = Color(badge.tier.containerColorHex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = SystemDivider)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 3D Apple-style Glowing Medal Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                tierColor.copy(alpha = 0.35f),
                                tierContainerColor,
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Surface(
                    shape = CircleShape,
                    color = SystemSurfaceSecondary,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            listOf(tierColor, tierColor.copy(alpha = 0.4f), tierColor)
                        )
                    ),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(86.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = badge.emoji,
                            fontSize = 42.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Title & Tier Row
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = badge.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = tierContainerColor,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, tierColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = "Tier",
                            tint = tierColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${badge.tier.title} Medal • +${badge.xpReward} XP Bounty",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tierColor
                        )
                    }
                }
            }

            // Distinct Status Card (Achieved vs In Progress)
            if (badge.isUnlocked) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SystemGreen.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Achieved",
                            tint = SystemGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "🏆 Medal Achieved & Claimed",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemGreen
                            )
                            Text(
                                text = if (badge.unlockedDate != null) "Earned on ${badge.unlockedDate}" else "Completed in profile",
                                fontSize = 13.sp,
                                color = SystemLabelSecondary
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SystemOrange.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemOrange.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Locked",
                                    tint = SystemOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "🔒 In Progress • Locked",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SystemOrange
                                )
                            }
                            Text(
                                text = "${badge.currentProgress} / ${badge.maxProgress}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemLabelPrimary
                            )
                        }

                        val progressRatio = (badge.currentProgress.toFloat() / badge.maxProgress.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = SystemOrange,
                            trackColor = SystemGray5
                        )

                        val remaining = badge.maxProgress - badge.currentProgress
                        if (remaining > 0) {
                            Text(
                                text = "$remaining more required to achieve this medal",
                                fontSize = 12.sp,
                                color = SystemLabelSecondary
                            )
                        }
                    }
                }
            }

            // Explicit Requirements & Profile Instructions Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SystemGroupedBackground,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Criteria Section
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Checklist,
                                contentDescription = null,
                                tint = SystemBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "CRITERIA & INSTRUCTIONS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemLabelSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = badge.unlockRequirement,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = SystemLabelPrimary,
                            lineHeight = 20.sp
                        )
                    }

                    HorizontalDivider(color = SystemDivider, thickness = 0.5.dp)

                    // Profile Action Tip
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = AppleStudy,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "PROFILE ACTION TIP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemLabelSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = badge.profileActionHint,
                            fontSize = 13.sp,
                            color = SystemLabelSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Dismiss Button
            Button(
                onClick = {
                    HapticManager.performClick(context)
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (badge.isUnlocked) "Awesome" else "Understood",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
