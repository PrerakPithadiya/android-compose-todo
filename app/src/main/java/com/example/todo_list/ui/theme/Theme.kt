package com.example.todo_list.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun TaskFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: AppAccentColor = AppAccentColor.BLUE,
    content: @Composable () -> Unit
) {
    val taskFlowColors = getAppleColors(isDark = darkTheme, accent = accentColor)

    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = taskFlowColors.systemBlue,
            onPrimary = Color.White,
            primaryContainer = taskFlowColors.systemBlueLight,
            onPrimaryContainer = Color.White,
            background = taskFlowColors.systemGroupedBackground,
            onBackground = taskFlowColors.systemLabelPrimary,
            surface = taskFlowColors.systemSurface,
            onSurface = taskFlowColors.systemLabelPrimary,
            surfaceVariant = taskFlowColors.systemGroupedBackground,
            onSurfaceVariant = taskFlowColors.systemLabelSecondary,
            outline = taskFlowColors.systemGray,
            outlineVariant = taskFlowColors.systemDivider,
            error = taskFlowColors.systemRed,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = taskFlowColors.systemBlue,
            onPrimary = Color.White,
            primaryContainer = taskFlowColors.systemBlueLight,
            onPrimaryContainer = taskFlowColors.systemBlueDark,
            background = taskFlowColors.systemGroupedBackground,
            onBackground = taskFlowColors.systemLabelPrimary,
            surface = taskFlowColors.systemSurface,
            onSurface = taskFlowColors.systemLabelPrimary,
            surfaceVariant = taskFlowColors.systemGroupedBackground,
            onSurfaceVariant = taskFlowColors.systemLabelSecondary,
            outline = taskFlowColors.systemGray,
            outlineVariant = taskFlowColors.systemDivider,
            error = taskFlowColors.systemRed,
            onError = Color.White
        )
    }

    CompositionLocalProvider(LocalTaskFlowColors provides taskFlowColors) {
        MaterialTheme(
            colorScheme = materialColors,
            content = content
        )
    }
}
