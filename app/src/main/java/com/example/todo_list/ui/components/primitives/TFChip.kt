package com.example.todo_list.ui.components.primitives

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todo_list.ui.theme.*

/**
 * TaskFlow Apple (iOS HIG) Pill Chip.
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 9.7
 *
 * Dimensions: 40dp height, 20dp radius.
 * Unselected: fillControl + labelPrimary
 * Selected: accentContainer + accentText + 1.5dp accent border + leading check
 */
@Composable
fun TFChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: String? = null,
    glyphColor: Color? = null,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles
    val typography = TFTheme.typography

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1.0f,
        animationSpec = TFMotion.snappy(),
        label = "tf_chip_scale"
    )

    val shape = TFShape.chip
    val backgroundColor = if (selected) accentRoles.accentContainer else colors.fillControl
    val textColor = if (selected) accentRoles.accentText else colors.labelPrimary

    Box(
        modifier = modifier
            .scale(scale)
            .height(40.dp)
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (selected) {
                    Modifier.border(1.5.dp, accentRoles.accent, shape)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) {
                TFHaptics.selection(context)
                onClick()
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(visible = selected) {
                Row {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = accentRoles.accentText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }

            if (!glyph.isNullOrBlank()) {
                Text(
                    text = glyph,
                    style = typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = glyphColor ?: textColor,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Text(
                text = text,
                style = typography.subheadline,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}
