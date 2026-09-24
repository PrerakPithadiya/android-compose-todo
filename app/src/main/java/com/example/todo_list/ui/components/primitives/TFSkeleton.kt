package com.example.todo_list.ui.components.primitives

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.todo_list.ui.theme.TFSpace
import com.example.todo_list.ui.theme.TFTheme

/**
 * TaskFlow Apple Shimmer Skeleton Loader.
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 12
 */
@Composable
fun TFSkeletonRow(
    modifier: Modifier = Modifier
) {
    val colors = TFTheme.colors

    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = TFSpace.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 24dp Circle shimmer
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(colors.fillControl.copy(alpha = alpha))
        )

        Spacer(modifier = Modifier.width(TFSpace.md))

        Column(modifier = Modifier.weight(1f)) {
            // Title line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.fillControl.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Subtitle line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.fillControl.copy(alpha = alpha * 0.7f))
            )
        }
    }
}
