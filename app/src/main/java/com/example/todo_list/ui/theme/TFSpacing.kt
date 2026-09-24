package com.example.todo_list.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * TaskFlow Spacing Scale & Physical Hairline (Apple iOS HIG Specification).
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 5.1 & Section 5.3
 */
object TFSpace {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val huge = 48.dp

    // Inset divider specifications
    val dividerInsetStart = 56.dp
}

/**
 * Renders exactly 1 physical pixel regardless of display density.
 * Prevents blurry hairlines on fractional scaling screens.
 */
@Composable
fun hairline(): Dp = (1f / LocalDensity.current.density).dp
