package com.example.todo_list.ui.theme

import androidx.compose.ui.graphics.Color

// Apple HIG System Colors
val SystemBlue = Color(0xFF007AFF)          // iOS Primary Accent
val SystemBlueLight = Color(0xFFE5F1FF)     // Soft Blue Tint
val SystemBlueDark = Color(0xFF0051A8)      // Deep Accent Blue

val SystemGroupedBackground = Color(0xFFF2F2F7) // iOS Inset Grouped Page Background
val SystemSurface = Color(0xFFFFFFFF)           // iOS Card / Grouped Item Surface
val SystemSurfaceSecondary = Color(0xFFF9F9FB)  // Subtle Secondary Card Fill

val SystemLabelPrimary = Color(0xFF000000)      // iOS Primary Label Text
val SystemLabelSecondary = Color(0x993C3C43)    // iOS Secondary Text (60% Alpha)
val SystemLabelTertiary = Color(0x4D3C3C43)     // iOS Dimmed / Strikethrough Text (30% Alpha)

val SystemGray = Color(0xFF8E8E93)              // iOS System Gray
val SystemGray2 = Color(0xFFAEAEB2)             // iOS Light Gray
val SystemGray5 = Color(0xFFE5E5EA)             // iOS Divider Gray
val SystemGray6 = Color(0xFFF2F2F7)             // iOS Secondary Background Gray

val SearchInputBackground = Color(0x1F767680)   // iOS Search Bar Fill (12% Gray)
val SystemDivider = Color(0x4A3C3C43)           // iOS Thin Inset Divider (29% Alpha)

val SystemRed = Color(0xFFFF3B30)               // iOS System Red / Destructive
val SystemGreen = Color(0xFF34C759)             // iOS System Green / Success

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
        else -> SystemBlue
    }
}

// Legacy Color Aliases mapped to Apple HIG Palette for backwards compatibility
val PrimaryBlue = SystemBlue
val PrimaryContainerBlue = SystemBlue
val PrimaryFixedBlue = SystemBlueLight
val OnPrimaryFixedBlue = SystemLabelPrimary
val OnPrimaryFixedVariantBlue = SystemBlueDark

val BackgroundLight = SystemGroupedBackground
val OnBackgroundDark = SystemLabelPrimary
val OnSurfaceVariant = SystemLabelSecondary

val SurfaceContainerLow = SystemSurface
val SurfaceContainer = SystemGroupedBackground
val SurfaceContainerHigh = SystemGray5

val SecondaryContainer = SystemBlueLight
val OnSecondaryContainer = SystemBlue
val OnSecondaryFixedVariant = SystemGray

val TertiaryContainer = Color(0xFFE8F5E9)
val OnTertiaryContainer = SystemGreen
val OnTertiaryFixedVariant = Color(0xFF1B5E20)
val TertiaryContainerSoft = Color(0xFFE8F5E9)

val OutlineVariant = SystemDivider
val Outline = SystemGray2

