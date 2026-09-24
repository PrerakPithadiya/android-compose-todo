package com.example.todo_list.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.todo_list.ui.theme.*

/**
 * TaskFlow Large-Title Navigation Bar with Integrated Search.
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 10.2
 *
 * Features:
 * - 44dp nav row with 32dp circular avatar and AI Sparkle action
 * - 34sp Bold Large Title
 * - 36dp visual search field with 10dp radius and fillControl
 */
@Composable
fun TFLargeTitleNavBar(
    title: String,
    avatarUrl: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onSparkleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles
    val typography = TFTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.canvas)
            .statusBarsPadding()
            .padding(horizontal = TFSpace.lg, vertical = TFSpace.sm)
    ) {
        // Nav action row: 44dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Avatar (32dp, 48dp touch)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAvatarClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentRoles.accentContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Me",
                            style = typography.caption,
                            color = accentRoles.accentText
                        )
                    }
                }
            }

            // Trailing Action: AI Sparkle (24dp glyph, 48dp touch)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSparkleClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "TaskFlow Intelligence",
                    tint = accentRoles.accent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Large Title
        Text(
            text = title,
            style = typography.largeTitle,
            color = colors.labelPrimary,
            modifier = Modifier.padding(top = 4.dp, bottom = TFSpace.sm)
        )

        // Integrated iOS Search Field: 36dp visual (48dp touch container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(TFShape.searchField)
                .background(colors.fillControl)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.labelSecondary,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = typography.body.copy(color = colors.labelPrimary),
                    cursorBrush = SolidColor(accentRoles.accent),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search",
                                style = typography.body,
                                color = colors.labelSecondary
                            )
                        }
                        innerTextField()
                    }
                )

                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = colors.labelSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onSearchQueryChange("") }
                    )
                }
            }
        }
    }
}
