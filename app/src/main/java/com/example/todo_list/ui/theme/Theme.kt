package com.example.todo_list.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppleLightColorScheme = lightColorScheme(
    primary = SystemBlue,
    onPrimary = Color.White,
    primaryContainer = SystemBlueLight,
    onPrimaryContainer = SystemBlueDark,
    background = SystemGroupedBackground,
    onBackground = SystemLabelPrimary,
    surface = SystemSurface,
    onSurface = SystemLabelPrimary,
    surfaceVariant = SystemGroupedBackground,
    onSurfaceVariant = SystemLabelSecondary,
    outline = SystemGray,
    outlineVariant = SystemDivider,
    error = SystemRed,
    onError = Color.White
)

@Composable
fun TaskFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppleLightColorScheme,
        content = content
    )
}

