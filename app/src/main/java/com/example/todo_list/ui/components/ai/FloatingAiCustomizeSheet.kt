package com.example.todo_list.ui.components.ai

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.ai.FloatingAiButtonManager
import com.example.todo_list.ai.FloatingAiColor
import com.example.todo_list.ai.FloatingAiGlyph
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager

/**
 * Apple iOS HIG Sheet for customizing the Floating AI Assistant Shortcut Button.
 * Allows users to choose between 8 curated Apple palettes and 6 feature-relevant AI glyphs
 * with live real-time preview and instant tactile haptics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingAiCustomizeSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentColor = FloatingAiButtonManager.selectedColor
    val currentGlyph = FloatingAiButtonManager.selectedGlyph

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
                .fillMaxHeight(0.90f)
        ) {
            // Drag Indicator Bar
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(SystemGray2.copy(alpha = 0.5f))
                    .align(Alignment.CenterHorizontally)
            )

            // Header Bar with Done button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Floating AI Button",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )

                TextButton(
                    onClick = {
                        HapticManager.performClick(context)
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "Done",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SystemBlue
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // 1. Live Interactive Button Preview Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SystemSurface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Floating Button Visual Preview
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = CircleShape,
                                    ambientColor = currentColor.startColor.copy(alpha = 0.45f),
                                    spotColor = currentColor.endColor.copy(alpha = 0.45f)
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            currentColor.startColor,
                                            currentColor.endColor
                                        )
                                    )
                                )
                                .border(1.5.dp, currentColor.strokeColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = currentGlyph.iconVector,
                                contentDescription = currentGlyph.displayName,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Badge / Caption
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${currentColor.displayName} • ${currentGlyph.displayName}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemLabelPrimary
                            )
                            Text(
                                text = "Edge-docked AI shortcut over WhatsApp & all apps",
                                fontSize = 13.sp,
                                color = SystemLabelSecondary
                            )
                        }
                    }
                }

                // 2. Color Theme Picker Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "BUTTON COLOR THEME",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SystemLabelSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SystemSurface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 2 rows of 4 colors each
                            val colorChunks = FloatingAiColor.entries.chunked(4)
                            colorChunks.forEachIndexed { rowIndex, rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowColors.forEach { colorItem ->
                                        val isSelected = currentColor == colorItem
                                        val scale by animateFloatAsState(
                                            targetValue = if (isSelected) 1.12f else 1.0f,
                                            animationSpec = tween(durationMillis = 180),
                                            label = "colorScale"
                                        )

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    if (!isSelected) {
                                                        HapticManager.performClick(context)
                                                        FloatingAiButtonManager.setColor(context, colorItem)
                                                    }
                                                }
                                                .padding(vertical = 6.dp, horizontal = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .scale(scale)
                                                    .then(
                                                        if (isSelected) {
                                                            Modifier.border(
                                                                width = 2.5.dp,
                                                                color = colorItem.startColor,
                                                                shape = CircleShape
                                                            )
                                                        } else Modifier
                                                    )
                                                    .padding(if (isSelected) 3.5.dp else 0.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            colors = listOf(
                                                                colorItem.startColor,
                                                                colorItem.endColor
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = colorItem.displayName.split(" ").firstOrNull() ?: colorItem.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) SystemLabelPrimary else SystemLabelSecondary
                                            )
                                        }
                                    }
                                }

                                if (rowIndex < colorChunks.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                // 3. Feature Icon Glyph Picker Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FEATURE ICON",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SystemLabelSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SystemSurface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            FloatingAiGlyph.entries.forEachIndexed { index, glyphItem ->
                                val isSelected = currentGlyph == glyphItem
                                val rowBgColor by animateColorAsState(
                                    targetValue = if (isSelected) currentColor.startColor.copy(alpha = 0.08f) else Color.Transparent,
                                    animationSpec = tween(durationMillis = 150),
                                    label = "rowBg"
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBgColor)
                                        .clickable {
                                            if (!isSelected) {
                                                HapticManager.performClick(context)
                                                FloatingAiButtonManager.setGlyph(context, glyphItem)
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Icon Bubble
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) {
                                                        currentColor.startColor.copy(alpha = 0.20f)
                                                    } else {
                                                        SystemGray5
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = glyphItem.iconVector,
                                                contentDescription = glyphItem.displayName,
                                                tint = if (isSelected) currentColor.startColor else SystemLabelSecondary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        // Title and Description
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = glyphItem.displayName,
                                                fontSize = 15.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = SystemLabelPrimary
                                            )
                                            Text(
                                                text = glyphItem.description,
                                                fontSize = 12.sp,
                                                color = SystemLabelSecondary
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = currentColor.startColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (index < FloatingAiGlyph.entries.size - 1) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = SystemDivider,
                                        modifier = Modifier.padding(start = 70.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
