package com.example.todo_list.manager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIconManagerTest {

    @Test
    fun testAppIconEntriesCountIsExactlyEight() {
        assertEquals("There must be exactly 8 application launcher icons", 8, AppIcon.entries.size)
    }

    @Test
    fun testAllAppIconsHaveUniqueIdsAndAliases() {
        val ids = AppIcon.entries.map { it.id }
        val aliasClassNames = AppIcon.entries.map { it.aliasClassName }
        val displayNames = AppIcon.entries.map { it.displayName }

        assertEquals("All icon IDs must be unique", ids.size, ids.distinct().size)
        assertEquals("All alias class names must be unique", aliasClassNames.size, aliasClassNames.distinct().size)
        assertEquals("All display names must be unique", displayNames.size, displayNames.distinct().size)
    }

    @Test
    fun testAllAppIconsHaveValidResourceIds() {
        AppIcon.entries.forEach { icon ->
            assertNotEquals("iconResId must be non-zero for ${icon.name}", 0, icon.iconResId)
            assertNotEquals("backgroundResId must be non-zero for ${icon.name}", 0, icon.backgroundResId)
            assertNotEquals("foregroundResId must be non-zero for ${icon.name}", 0, icon.foregroundResId)
            assertTrue("displayName must not be blank for ${icon.name}", icon.displayName.isNotBlank())
            assertTrue("subtitle must not be blank for ${icon.name}", icon.subtitle.isNotBlank())
        }
    }

    @Test
    fun testAppIconFromIdResolution() {
        assertEquals(AppIcon.CLASSIC, AppIcon.fromId("classic"))
        assertEquals(AppIcon.DARK, AppIcon.fromId("dark"))
        assertEquals(AppIcon.NEON, AppIcon.fromId("neon"))
        assertEquals(AppIcon.GLASS, AppIcon.fromId("glass"))
        assertEquals(AppIcon.SUNSET, AppIcon.fromId("sunset"))
        assertEquals(AppIcon.EMERALD, AppIcon.fromId("emerald"))
        assertEquals(AppIcon.PURPLE, AppIcon.fromId("purple"))
        assertEquals(AppIcon.GOLD, AppIcon.fromId("gold"))

        // Case insensitivity
        assertEquals(AppIcon.SUNSET, AppIcon.fromId("SuNsEt"))
        assertEquals(AppIcon.EMERALD, AppIcon.fromId("Emerald Mint"))

        // Unknown / null fallback to CLASSIC
        assertEquals(AppIcon.CLASSIC, AppIcon.fromId("non_existent_icon"))
        assertEquals(AppIcon.CLASSIC, AppIcon.fromId(null))
    }
}
