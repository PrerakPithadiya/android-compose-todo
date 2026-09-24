package com.example.todo_list.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentRolesTest {

    @Test
    fun testAllAccentsMeetContrastRequirementsInLightAndDark() {
        val lightCard = Color(0xFFFFFFFF)
        val lightCanvas = Color(0xFFF2F2F7)
        val darkCard = Color(0xFF1C1C1E)
        val darkCanvas = Color(0xFF000000)

        for (accent in AppAccentColor.entries) {
            // Light theme test
            val lightRoles = resolveAccent(
                brand = accent.lightColor,
                container = accent.lightContainer,
                card = lightCard,
                canvas = lightCanvas,
                dark = false
            )
            val lightTextCardContrast = contrast(lightRoles.accentText, lightCard)
            val lightTextCanvasContrast = contrast(lightRoles.accentText, lightCanvas)
            val lightBtnLabelContrast = contrast(lightRoles.onAccent, lightRoles.accentFill)

            assertTrue(
                "Accent ${accent.name} light text vs card failed contrast ($lightTextCardContrast)",
                lightTextCardContrast >= 4.45f
            )
            assertTrue(
                "Accent ${accent.name} light text vs canvas failed contrast ($lightTextCanvasContrast)",
                lightTextCanvasContrast >= 4.45f
            )
            assertTrue(
                "Accent ${accent.name} light button label contrast failed ($lightBtnLabelContrast)",
                lightBtnLabelContrast >= 4.45f
            )

            // Dark theme test
            val darkRoles = resolveAccent(
                brand = accent.darkColor,
                container = accent.darkContainer,
                card = darkCard,
                canvas = darkCanvas,
                dark = true
            )
            val darkTextCardContrast = contrast(darkRoles.accentText, darkCard)
            val darkTextCanvasContrast = contrast(darkRoles.accentText, darkCanvas)
            val darkBtnLabelContrast = contrast(darkRoles.onAccent, darkRoles.accentFill)

            assertTrue(
                "Accent ${accent.name} dark text vs card failed contrast ($darkTextCardContrast)",
                darkTextCardContrast >= 4.45f
            )
            assertTrue(
                "Accent ${accent.name} dark text vs canvas failed contrast ($darkTextCanvasContrast)",
                darkTextCanvasContrast >= 4.45f
            )
            assertTrue(
                "Accent ${accent.name} dark button label contrast failed ($darkBtnLabelContrast)",
                darkBtnLabelContrast >= 4.45f
            )
        }
    }
}
