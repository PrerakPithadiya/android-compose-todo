package com.example.todo_list.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Apple HIG Accent Color Palette
enum class AppAccentColor(
    val id: String,
    val displayName: String,
    val lightColor: Color,
    val darkColor: Color,
    val lightContainer: Color,
    val darkContainer: Color
) {
    BLUE(
        id = "blue",
        displayName = "System Blue",
        lightColor = Color(0xFF007AFF),
        darkColor = Color(0xFF0A84FF),
        lightContainer = Color(0xFFE5F1FF),
        darkContainer = Color(0x33007AFF)
    ),
    PURPLE(
        id = "purple",
        displayName = "iOS Purple",
        lightColor = Color(0xFFAF52DE),
        darkColor = Color(0xFFBF5AF2),
        lightContainer = Color(0xFFF5E8FF),
        darkContainer = Color(0x33AF52DE)
    ),
    PINK(
        id = "pink",
        displayName = "System Pink",
        lightColor = Color(0xFFFF2D55),
        darkColor = Color(0xFFFF375F),
        lightContainer = Color(0xFFFFE5EA),
        darkContainer = Color(0x33FF2D55)
    ),
    ORANGE(
        id = "orange",
        displayName = "Sunset Orange",
        lightColor = Color(0xFFFF9500),
        darkColor = Color(0xFFFF9F0A),
        lightContainer = Color(0xFFFFF3E0),
        darkContainer = Color(0x33FF9500)
    ),
    GREEN(
        id = "green",
        displayName = "System Green",
        lightColor = Color(0xFF34C759),
        darkColor = Color(0xFF30D158),
        lightContainer = Color(0xFFE8F8ED),
        darkContainer = Color(0x3334C759)
    ),
    YELLOW(
        id = "yellow",
        displayName = "System Yellow",
        lightColor = Color(0xFFFFCC00),
        darkColor = Color(0xFFFFD60A),
        lightContainer = Color(0xFFFFFBE6),
        darkContainer = Color(0x33FFCC00)
    ),
    INDIGO(
        id = "indigo",
        displayName = "System Indigo",
        lightColor = Color(0xFF5856D6),
        darkColor = Color(0xFF5E5CE6),
        lightContainer = Color(0xFFEEEEFF),
        darkContainer = Color(0x335856D6)
    ),
    TEAL(
        id = "teal",
        displayName = "System Teal",
        lightColor = Color(0xFF30B0C7),
        darkColor = Color(0xFF40C8E0),
        lightContainer = Color(0xFFE6F7FA),
        darkContainer = Color(0x3330B0C7)
    );

    companion object {
        val DEFAULT = BLUE
        fun fromId(id: String): AppAccentColor = entries.find { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}

// TaskFlow Dynamic Color Palette Data Class
class TaskFlowColors(
    val isDark: Boolean,
    val systemBlue: Color,
    val systemBlueLight: Color,
    val systemBlueDark: Color,
    val systemGroupedBackground: Color,
    val systemSurface: Color,
    val systemSurfaceSecondary: Color,
    val systemLabelPrimary: Color,
    val systemLabelSecondary: Color,
    val systemLabelTertiary: Color,
    val systemGray: Color,
    val systemGray2: Color,
    val systemGray5: Color,
    val systemGray6: Color,
    val searchInputBackground: Color,
    val systemDivider: Color,
    val systemRed: Color,
    val systemGreen: Color
)

fun getAppleColors(isDark: Boolean, accent: AppAccentColor = AppAccentColor.BLUE): TaskFlowColors {
    val primaryAccent = if (isDark) accent.darkColor else accent.lightColor
    val primaryContainer = if (isDark) accent.darkContainer else accent.lightContainer
    val primaryDark = if (isDark) accent.lightColor else accent.darkColor

    return if (isDark) {
        TaskFlowColors(
            isDark = true,
            systemBlue = primaryAccent,
            systemBlueLight = primaryContainer,
            systemBlueDark = primaryDark,
            systemGroupedBackground = Color(0xFF000000), // iOS Dark Grouped Canvas (#000000)
            systemSurface = Color(0xFF1C1C1E),           // iOS Dark Inset Card Surface (#1C1C1E)
            systemSurfaceSecondary = Color(0xFF2C2C2E),  // iOS Dark Secondary Elevated Surface (#2C2C2E)
            systemLabelPrimary = Color(0xFFFFFFFF),      // Primary White Text
            systemLabelSecondary = Color(0x99EBEBF5),    // 60% Secondary White Text
            systemLabelTertiary = Color(0x4DEBEBF5),     // 30% Tertiary Text
            systemGray = Color(0xFF8E8E93),
            systemGray2 = Color(0xFF636366),
            systemGray5 = Color(0xFF2C2C2E),
            systemGray6 = Color(0xFF1C1C1E),
            searchInputBackground = Color(0x3D767680),   // 24% fill for search bar
            systemDivider = Color(0x54545865),           // iOS Dark Divider
            systemRed = Color(0xFFFF453A),               // iOS Dark System Red
            systemGreen = Color(0xFF30D158)              // iOS Dark System Green
        )
    } else {
        TaskFlowColors(
            isDark = false,
            systemBlue = primaryAccent,
            systemBlueLight = primaryContainer,
            systemBlueDark = primaryDark,
            systemGroupedBackground = Color(0xFFF2F2F7), // iOS Inset Grouped Light Background
            systemSurface = Color(0xFFFFFFFF),           // iOS Light Card Surface
            systemSurfaceSecondary = Color(0xFFF9F9FB),  // Subtle Secondary Card Fill
            systemLabelPrimary = Color(0xFF000000),      // Primary Black Text
            systemLabelSecondary = Color(0x993C3C43),    // 60% Secondary Text
            systemLabelTertiary = Color(0x4D3C3C43),     // 30% Tertiary Text
            systemGray = Color(0xFF8E8E93),
            systemGray2 = Color(0xFFAEAEB2),
            systemGray5 = Color(0xFFE5E5EA),
            systemGray6 = Color(0xFFF2F2F7),
            searchInputBackground = Color(0x1F767680),   // 12% Search Bar Fill
            systemDivider = Color(0x4A3C3C43),           // 29% Inset Divider
            systemRed = Color(0xFFFF3B30),
            systemGreen = Color(0xFF34C759)
        )
    }
}

val AppleLightColors = getAppleColors(isDark = false, accent = AppAccentColor.BLUE)
val AppleDarkColors = getAppleColors(isDark = true, accent = AppAccentColor.BLUE)

val LocalTaskFlowColors = staticCompositionLocalOf { AppleLightColors }

// Dynamic Composable Color Token Accessors
val SystemBlue: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemBlue

val SystemBlueLight: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemBlueLight

val SystemBlueDark: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemBlueDark

val SystemGroupedBackground: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemGroupedBackground

val SystemSurface: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemSurface

val SystemSurfaceSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemSurfaceSecondary

val SystemLabelPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemLabelPrimary

val SystemLabelSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemLabelSecondary

val SystemLabelTertiary: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemLabelTertiary

val SystemGray: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemGray

val SystemGray2: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemGray2

val SystemGray5: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemGray5

val SystemGray6: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemGray6

val SearchInputBackground: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.searchInputBackground

val SystemDivider: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemDivider

val SystemRed: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemRed

val SystemGreen: Color
    @Composable @ReadOnlyComposable get() = LocalTaskFlowColors.current.systemGreen

val SystemOrange = Color(0xFFFF9500)

// Category Colors (Apple HIG Palette)
val AppleWork = Color(0xFF007AFF)              // Work - System Blue
val ApplePersonal = Color(0xFFAF52DE)          // Personal - System Purple
val AppleHealth = Color(0xFF34C759)            // Health - System Green
val AppleStudy = Color(0xFFFF9500)             // Study - System Orange

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "work" -> AppleWork
        "personal" -> ApplePersonal
        "health" -> AppleHealth
        "study" -> AppleStudy
        else -> AppleWork
    }
}

// Backwards-compatible legacy aliases
val PrimaryBlue: Color @Composable @ReadOnlyComposable get() = SystemBlue
val PrimaryContainerBlue: Color @Composable @ReadOnlyComposable get() = SystemBlue
val PrimaryFixedBlue: Color @Composable @ReadOnlyComposable get() = SystemBlueLight
val OnPrimaryFixedBlue: Color @Composable @ReadOnlyComposable get() = SystemLabelPrimary
val OnPrimaryFixedVariantBlue: Color @Composable @ReadOnlyComposable get() = SystemBlueDark

val BackgroundLight: Color @Composable @ReadOnlyComposable get() = SystemGroupedBackground
val OnBackgroundDark: Color @Composable @ReadOnlyComposable get() = SystemLabelPrimary
val OnSurfaceVariant: Color @Composable @ReadOnlyComposable get() = SystemLabelSecondary

val SurfaceContainerLow: Color @Composable @ReadOnlyComposable get() = SystemSurface
val SurfaceContainer: Color @Composable @ReadOnlyComposable get() = SystemGroupedBackground
val SurfaceContainerHigh: Color @Composable @ReadOnlyComposable get() = SystemGray5

val SecondaryContainer: Color @Composable @ReadOnlyComposable get() = SystemBlueLight
val OnSecondaryContainer: Color @Composable @ReadOnlyComposable get() = SystemBlue
val OnSecondaryFixedVariant: Color @Composable @ReadOnlyComposable get() = SystemGray

val TertiaryContainer: Color = Color(0xFFE8F5E9)
val OnTertiaryContainer: Color = Color(0xFF34C759)
val OnTertiaryFixedVariant: Color = Color(0xFF1B5E20)
val TertiaryContainerSoft: Color = Color(0xFFE8F5E9)

val OutlineVariant: Color @Composable @ReadOnlyComposable get() = SystemDivider
val Outline: Color @Composable @ReadOnlyComposable get() = SystemGray2
