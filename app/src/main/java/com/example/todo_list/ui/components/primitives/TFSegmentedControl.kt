package com.example.todo_list.ui.components.primitives

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.todo_list.ui.theme.*

/**
 * TaskFlow Apple Segmented Control (Apple iOS HIG Specification).
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 9.5
 *
 * Features:
 * - 32dp visual height (min 48dp touch)
 * - 9dp track radius with fillControl
 * - 7dp thumb radius with fillSelected & soft shadow
 * - TFMotion.snappy() thumb transition
 * - TFHaptics.selection() on change
 */
@Composable
fun TFSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val colors = TFTheme.colors
    val typography = TFTheme.typography

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = TFMotion.snappy(),
        label = "tf_segmented_thumb"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Layout(
            content = {
                // 1. Sliding Thumb
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(2.dp)
                        .shadow(
                            elevation = if (!colors.isDark) 2.dp else 0.dp,
                            shape = TFShape.segmentedThumb
                        )
                        .clip(TFShape.segmentedThumb)
                        .background(colors.fillSelected)
                )

                // 2. Segment items
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(TFShape.segmentedThumb)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (index != selectedIndex) {
                                    TFHaptics.selection(context)
                                    onItemSelected(index)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            style = typography.subheadline,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) colors.labelPrimary else colors.labelSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(TFShape.segmentedTrack)
                .background(colors.fillControl)
        ) { measurables, constraints ->
            val totalWidth = constraints.maxWidth
            val count = items.size
            val itemWidth = totalWidth / count
            val height = constraints.maxHeight

            val thumbPlaceable = measurables[0].measure(
                Constraints.fixed(width = itemWidth, height = height)
            )

            val itemPlaceables = measurables.subList(1, measurables.size).map { measurable ->
                measurable.measure(Constraints.fixed(width = itemWidth, height = height))
            }

            layout(totalWidth, height) {
                // Position the thumb at animated index position
                val thumbX = (animatedIndex * itemWidth).toInt()
                thumbPlaceable.placeRelative(thumbX, 0)

                // Place segment labels
                itemPlaceables.forEachIndexed { i, placeable ->
                    placeable.placeRelative(i * itemWidth, 0)
                }
            }
        }
    }
}
