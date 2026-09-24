package com.example.todo_list.ui.components.primitives

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.todo_list.ui.theme.*

enum class TFButtonType {
    FILLED,
    TINTED,
    PLAIN,
    DESTRUCTIVE
}

/**
 * TaskFlow Apple (iOS HIG) Button Component.
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 9.3
 *
 * Features:
 * - 50dp height, 14dp radius
 * - No ripple: scales to 0.98 on press with TFMotion.press() spring
 * - Full contrast adherence via AccentRoles
 */
@Composable
fun TFButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: TFButtonType = TFButtonType.FILLED,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles
    val typography = TFTheme.typography

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.98f else 1.0f,
        animationSpec = TFMotion.press(),
        label = "tf_button_scale"
    )

    val (backgroundColor, textColor) = when (type) {
        TFButtonType.FILLED -> {
            if (enabled) {
                Pair(accentRoles.accentFill, accentRoles.onAccent)
            } else {
                Pair(colors.fillControl, colors.labelTertiary)
            }
        }
        TFButtonType.TINTED -> {
            if (enabled) {
                Pair(accentRoles.accentContainer, accentRoles.accentText)
            } else {
                Pair(colors.fillControl, colors.labelTertiary)
            }
        }
        TFButtonType.PLAIN -> {
            if (enabled) {
                Pair(Color.Transparent, accentRoles.accentText)
            } else {
                Pair(Color.Transparent, colors.labelTertiary)
            }
        }
        TFButtonType.DESTRUCTIVE -> {
            if (enabled) {
                Pair(colors.red.copy(alpha = 0.15f), colors.red)
            } else {
                Pair(Color.Transparent, colors.labelTertiary)
            }
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 50.dp)
            .clip(TFShape.button)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // No Material ripple, Apple HIG clean press
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = textColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(TFSpace.sm))
                }
                Text(
                    text = text,
                    style = typography.headline,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
