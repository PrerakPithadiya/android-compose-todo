package com.example.todo_list.ui.components.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.todo_list.ui.theme.*

/**
 * TaskFlow Inset Grouped Container (Apple iOS HIG Specification).
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 9.1
 *
 * Features:
 * - 12dp corner radius
 * - In light: flat white on #F2F2F7 grouped background (no border, no shadow)
 * - In dark: #1C1C1E elevated surface with 1px cardStroke (#545458 @ 55%) border
 * - 16dp horizontal margin
 * - Optional section header (title3) and footer (footnote)
 */
@Composable
fun TFCardGroup(
    modifier: Modifier = Modifier,
    headerTitle: String? = null,
    footerText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = TFTheme.colors
    val typography = TFTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TFSpace.lg)
    ) {
        if (!headerTitle.isNullOrBlank()) {
            Text(
                text = headerTitle,
                style = typography.title3,
                color = colors.labelPrimary,
                modifier = Modifier.padding(start = TFSpace.sm, bottom = TFSpace.sm)
            )
        }

        val shape = TFShape.card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.card)
                .then(
                    if (colors.isDark) {
                        Modifier.border(hairline(), colors.cardStroke, shape)
                    } else {
                        Modifier
                    }
                ),
            content = content
        )

        if (!footerText.isNullOrBlank()) {
            Text(
                text = footerText,
                style = typography.footnote,
                color = colors.labelSecondary,
                modifier = Modifier.padding(start = TFSpace.sm, top = TFSpace.sm)
            )
        }
    }
}

/**
 * Inset hairline divider for grouped items.
 * Starts at 56dp from start edge (aligning with text, bypassing 48dp checkbox target).
 */
@Composable
fun TFGroupDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = TFSpace.dividerInsetStart
) {
    val colors = TFTheme.colors
    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent),
        thickness = hairline(),
        color = colors.separator
    )
}
