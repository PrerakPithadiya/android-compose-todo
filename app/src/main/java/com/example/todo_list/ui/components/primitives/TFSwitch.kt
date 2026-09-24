package com.example.todo_list.ui.components.primitives

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.todo_list.ui.theme.*

/**
 * TaskFlow Apple (iOS HIG) Switch.
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 9.6
 *
 * Dimensions: 51dp x 31dp track, 27dp thumb.
 * On track = accentFill; Off track = controlStroke (>= 3:1 contrast against card).
 */
@Composable
fun TFSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles

    val trackColor by animateColorAsState(
        targetValue = if (checked) accentRoles.accentFill else colors.controlStroke.copy(alpha = 0.5f),
        animationSpec = TFMotion.snappy(),
        label = "tf_switch_track"
    )

    // Thumb offset: 2dp padding on left when false, (51 - 27 - 2) = 22dp on left when true
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 22f else 2f,
        animationSpec = TFMotion.snappy(),
        label = "tf_switch_thumb"
    )

    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) {
                TFHaptics.light(context)
                onCheckedChange(!checked)
            }
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(27.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
