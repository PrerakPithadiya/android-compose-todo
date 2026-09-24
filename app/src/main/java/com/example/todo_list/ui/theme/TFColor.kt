package com.example.todo_list.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * TaskFlow Semantic Neutral & System Colors (Apple iOS HIG Specification).
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 3.1
 */
data class TFColors(
    val isDark: Boolean,
    val canvas: Color,
    val card: Color,
    val cardRaised: Color,
    val cardSecondary: Color,
    val fillControl: Color,
    val fillSelected: Color,
    val labelPrimary: Color,
    val labelSecondary: Color,
    val labelTertiary: Color,
    val separator: Color,
    val controlStroke: Color,
    val cardStroke: Color,
    val pressedOverlay: Color,
    val scrim: Color,
    // Semantic system colors
    val red: Color,
    val orange: Color,
    val blue: Color,
    val green: Color
)

val LightTFColors = TFColors(
    isDark = false,
    canvas = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    cardRaised = Color(0xFFFFFFFF),
    cardSecondary = Color(0xFFF9F9FB),
    fillControl = Color(0x1F767680), // #767680 @ 12%
    fillSelected = Color(0xFFFFFFFF),
    labelPrimary = Color(0xFF000000),
    labelSecondary = Color(0xB83C3C43), // #3C3C43 @ 72% (~4.7:1 contrast on white)
    labelTertiary = Color(0x4D3C3C43),  // #3C3C43 @ 30% (decorative only)
    separator = Color(0x4A3C3C43),      // #3C3C43 @ 29%
    controlStroke = Color(0xFF8E8E93),  // >= 3:1 against surface
    cardStroke = Color.Transparent,     // Light cards have no border or shadow
    pressedOverlay = Color(0x0F000000), // #000000 @ 6%
    scrim = Color(0x52000000),          // #000000 @ 32%
    red = Color(0xFFFF3B30),
    orange = Color(0xFFFF9500),
    blue = Color(0xFF007AFF),
    green = Color(0xFF34C759)
)

val DarkTFColors = TFColors(
    isDark = true,
    canvas = Color(0xFF000000),         // OLED Pure Black
    card = Color(0xFF1C1C1E),           // Dark Inset Surface
    cardRaised = Color(0xFF2C2C2E),     // Sheets / Popovers
    cardSecondary = Color(0xFF2C2C2E),  // Nested Secondary Surface
    fillControl = Color(0x3D767680),    // #767680 @ 24%
    fillSelected = Color(0xFF636366),
    labelPrimary = Color(0xFFFFFFFF),
    labelSecondary = Color(0x99EBEBF5), // #EBEBF5 @ 60% (~5.9:1 contrast)
    labelTertiary = Color(0x4DEBEBF5),  // #EBEBF5 @ 30% (decorative only)
    separator = Color(0xA6545458),      // #545458 @ 65%
    controlStroke = Color(0xFF8E8E93),  // >= 3:1 against surface
    cardStroke = Color(0x8C545458),     // #545458 @ 55% (1px card border to prevent blending)
    pressedOverlay = Color(0x14FFFFFF), // #FFFFFF @ 8%
    scrim = Color(0x8F000000),          // #000000 @ 56%
    red = Color(0xFFFF453A),
    orange = Color(0xFFFF9F0A),
    blue = Color(0xFF0A84FF),
    green = Color(0xFF30D158)
)

val LocalTFColors = staticCompositionLocalOf { LightTFColors }

object TFTheme {
    val colors: TFColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTFColors.current

    val accentRoles: AccentRoles
        @Composable
        @ReadOnlyComposable
        get() = LocalAccentRoles.current

    val typography: TFTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTFTypography.current
}
