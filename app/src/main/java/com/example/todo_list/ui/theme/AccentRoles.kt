package com.example.todo_list.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Dynamic Accent Role Solver (Apple iOS HIG Specification).
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 3.2 & Section 16
 *
 * Ensures all 8 user-selectable accents meet WCAG contrast requirements:
 * - accent: >= 3:1 vs its surface
 * - accentFill: background of filled buttons and Plus button
 * - onAccent: white if >= 4.5:1 against accentFill, else black
 * - accentText: >= 4.5:1 vs card AND vs canvas
 * - accentContainer: tinted background for chips and badges
 */
data class AccentRoles(
    val accent: Color,
    val accentFill: Color,
    val onAccent: Color,
    val accentText: Color,
    val accentContainer: Color
)

fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

private fun rgbToHsl(color: Color, outHsl: FloatArray) {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, maxOf(g, b))
    val min = minOf(r, minOf(g, b))
    val delta = max - min
    var h = 0f
    var s = 0f
    val l = (max + min) / 2f
    if (delta != 0f) {
        s = if (l <= 0.5f) delta / (max + min) else delta / (2f - max - min)
        h = when (max) {
            r -> ((g - b) / delta + (if (g < b) 6f else 0f)) * 60f
            g -> ((b - r) / delta + 2f) * 60f
            else -> ((r - g) / delta + 4f) * 60f
        }
    }
    outHsl[0] = h
    outHsl[1] = s
    outHsl[2] = l
}

private fun hslToColor(hsl: FloatArray): Color {
    val h = hsl[0]
    val s = hsl[1]
    val l = hsl[2]
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = 1f
    )
}

fun ensureContrast(fg: Color, bg: Color, min: Float, darkTheme: Boolean): Color {
    val hsl = FloatArray(3)
    rgbToHsl(fg, hsl)
    var c = fg
    var i = 0
    while (contrast(c, bg) < min && i++ < 50) {
        hsl[2] = (hsl[2] + if (darkTheme) 0.02f else -0.02f).coerceIn(0f, 1f)
        c = hslToColor(hsl)
    }
    return c
}

private val accentCache = mutableMapOf<String, AccentRoles>()

fun resolveAccent(
    brand: Color,
    container: Color,
    card: Color,
    canvas: Color,
    dark: Boolean
): AccentRoles {
    val cacheKey = "${brand.value}_${card.value}_${canvas.value}_$dark"
    accentCache[cacheKey]?.let { return it }

    // 1. Accent: >= 3:1 vs card surface
    val resolvedAccent = ensureContrast(brand, card, 3.0f, dark)

    // 2. AccentFill: filled buttons, Plus button
    // Nudge lightness until white or black label reaches >= 4.5:1
    val resolvedAccentFill = if (!dark) {
        val whiteContrast = contrast(Color.White, brand)
        if (whiteContrast >= 4.5f) {
            brand
        } else {
            ensureContrast(brand, Color.White, 4.5f, darkTheme = false)
        }
    } else {
        brand
    }

    // 3. onAccent: White if contrast >= 4.5:1, else Black
    val onAccent = if (contrast(Color.White, resolvedAccentFill) >= 4.5f) {
        Color.White
    } else {
        Color.Black
    }

    // 4. AccentText: >= 4.5:1 against card AND canvas
    var resolvedText = ensureContrast(brand, card, 4.5f, dark)
    if (contrast(resolvedText, canvas) < 4.5f) {
        resolvedText = ensureContrast(resolvedText, canvas, 4.5f, dark)
    }

    // 5. AccentContainer: container hex in light; 20% alpha over card in dark
    val resolvedContainer = if (!dark) {
        container
    } else {
        brand.copy(alpha = 0.20f)
    }

    val roles = AccentRoles(
        accent = resolvedAccent,
        accentFill = resolvedAccentFill,
        onAccent = onAccent,
        accentText = resolvedText,
        accentContainer = resolvedContainer
    )
    accentCache[cacheKey] = roles
    return roles
}

val DefaultAccentRoles = resolveAccent(
    brand = Color(0xFF007AFF),
    container = Color(0xFFE5F1FF),
    card = Color(0xFFFFFFFF),
    canvas = Color(0xFFF2F2F7),
    dark = false
)

val LocalAccentRoles = staticCompositionLocalOf { DefaultAccentRoles }
