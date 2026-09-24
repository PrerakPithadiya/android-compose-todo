package com.example.todo_list.ui.components.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todo_list.ui.theme.*

/**
 * TaskFlow 5-Slot Bottom Navigation Bar with Center-Docked Plus Button.
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 10.1 & extra_instructions.md
 *
 * Slots: Home (0) | Calendar (1) | [ + ] (Center Docked) | Lists (2) | Settings (3)
 * Height: 56dp + system gesture inset.
 * Plus button rises 8dp above bar (52dp diameter, docked to slot 3).
 * FAB is completely eliminated.
 */
@Composable
fun TFTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onPlusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Bar surface with 1px top hairline
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card)
                .navigationBarsPadding()
        ) {
            HorizontalDivider(
                thickness = hairline(),
                color = colors.separator
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Slot 0: Home
                TFTabItem(
                    icon = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                    label = "Home",
                    isSelected = selectedTab == 0,
                    onClick = {
                        TFHaptics.light(context)
                        onTabSelected(0)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Slot 1: Calendar
                TFTabItem(
                    icon = Icons.Outlined.DateRange,
                    label = "Calendar",
                    isSelected = selectedTab == 1,
                    onClick = {
                        TFHaptics.light(context)
                        onTabSelected(1)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Slot 2: Empty Spacer for Center-Docked Plus
                Spacer(modifier = Modifier.weight(1.2f))

                // Slot 3: Lists
                TFTabItem(
                    icon = Icons.Outlined.Category,
                    label = "Lists",
                    isSelected = selectedTab == 2,
                    onClick = {
                        TFHaptics.light(context)
                        onTabSelected(2)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Slot 4: Settings
                TFTabItem(
                    icon = Icons.Outlined.Settings,
                    label = "Settings",
                    isSelected = selectedTab == 3,
                    onClick = {
                        TFHaptics.light(context)
                        onTabSelected(3)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Center-Docked 52dp Plus Button rising 8dp above bar
        val plusInteraction = remember { MutableInteractionSource() }
        val isPlusPressed by plusInteraction.collectIsPressedAsState()
        val plusScale by animateFloatAsState(
            targetValue = if (isPlusPressed) 0.92f else 1.0f,
            animationSpec = TFMotion.press(),
            label = "tf_plus_scale"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
                .scale(plusScale)
                .size(52.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    spotColor = accentRoles.accentFill.copy(alpha = 0.35f)
                )
                .clip(CircleShape)
                .background(accentRoles.accentFill)
                .clickable(
                    interactionSource = plusInteraction,
                    indication = null,
                    onClick = {
                        TFHaptics.light(context)
                        onPlusClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Task",
                tint = accentRoles.onAccent,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun TFTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles
    val typography = TFTheme.typography

    val tint = if (isSelected) accentRoles.accent else colors.labelSecondary

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = typography.caption,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = tint
        )
    }
}
