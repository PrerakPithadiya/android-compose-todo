package com.example.todo_list.ui.components.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.todo_list.ui.theme.*

/**
 * TaskFlow Empty State (Apple iOS HIG Specification).
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 12
 *
 * Features:
 * - 96dp layered vector art container
 * - title2 headline
 * - subheadline body
 * - Filled CTA button (optional)
 * - Up to 3 starter chips that prefill create sheet
 */
@Composable
fun TFEmptyState(
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.CheckCircle,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null,
    starterChips: List<String> = emptyList(),
    onChipClick: ((String) -> Unit)? = null
) {
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles
    val typography = TFTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 96dp Layered Vector Art Container
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(accentRoles.accentContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentRoles.accent,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(TFSpace.xl))

        Text(
            text = headline,
            style = typography.title2,
            color = colors.labelPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(TFSpace.sm))

        Text(
            text = body,
            style = typography.subheadline,
            color = colors.labelSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (!ctaText.isNullOrBlank() && onCtaClick != null) {
            Spacer(modifier = Modifier.height(TFSpace.xl))
            TFButton(
                text = ctaText,
                onClick = onCtaClick,
                type = TFButtonType.FILLED
            )
        }

        if (starterChips.isNotEmpty() && onChipClick != null) {
            Spacer(modifier = Modifier.height(TFSpace.xl))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                starterChips.take(3).forEach { chipText ->
                    TFChip(
                        text = chipText,
                        selected = false,
                        onClick = { onChipClick(chipText) }
                    )
                }
            }
        }
    }
}
