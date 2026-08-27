package com.example.todo_list.ui.screens.profile

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.todo_list.manager.UserProfileManager
import com.example.todo_list.model.UserProfile
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalCardBottomSheet(
    profile: UserProfile,
    totalTasksCompleted: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val avatarUrl = UserProfileManager.getAvatarUrl()
    val level = UserProfileManager.getLevel()
    val streak = UserProfileManager.currentStreak

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = SystemDivider) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Digital Productivity Pass",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary
            )

            // Apple Wallet / Titanium Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141416),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(Color(0xFF43434B), Color(0xFF222226), Color(0xFF555562))
                        ),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1C1C20),
                                    Color(0xFF0F0F12),
                                    Color(0xFF18181D)
                                )
                            )
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header inside card
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
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = "TaskFlow",
                                tint = Color(0xFF0A84FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "TASKFLOW",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x33007AFF),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF0A84FF))
                        ) {
                            Text(
                                text = "PRO PASS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0A84FF),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // User Identity Row inside Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF0A84FF), CircleShape)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = profile.name,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Outlined.Verified,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF0A84FF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = profile.username,
                                fontSize = 14.sp,
                                color = Color(0xFF8E8E93)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x22FFFFFF)
                            ) {
                                Text(
                                    text = profile.focusStatus,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE5E5EA),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Perforated Line Separator
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    ) {
                        drawLine(
                            color = Color(0x44FFFFFF),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Key Stats Grid inside Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "MASTERY", fontSize = 10.sp, color = Color(0xFF8E8E93), fontWeight = FontWeight.Bold)
                            Text(text = "Level $level", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text(text = "ACTIVE STREAK", fontSize = 10.sp, color = Color(0xFF8E8E93), fontWeight = FontWeight.Bold)
                            Text(text = "$streak Days 🔥", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9F0A))
                        }
                        Column {
                            Text(text = "COMPLETED", fontSize = 10.sp, color = Color(0xFF8E8E93), fontWeight = FontWeight.Bold)
                            Text(text = "$totalTasksCompleted Tasks", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF30D158))
                        }
                    }

                    // QR Code Barcode Representation
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode,
                                contentDescription = "QR Code",
                                tint = Color.Black,
                                modifier = Modifier.size(54.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "SCAN TO CONNECT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "taskflow.app/${profile.username.removePrefix("@")}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF636366)
                                )
                            }
                        }
                    }
                }
            }

            // Share & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        HapticManager.performSuccess(context)
                        Toast.makeText(context, "Pass saved to photos & files", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SystemBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SystemBlue),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Download, contentDescription = "Save", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Save Pass", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        HapticManager.performClick(context)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out my TaskFlow Productivity Profile: ${profile.name} (${profile.username}) • Level $level • $streak Day Streak 🔥\nhttps://taskflow.app/${profile.username.removePrefix("@")}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Productivity Pass"))
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Share Pass", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
