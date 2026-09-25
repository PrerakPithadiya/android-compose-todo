package com.example.todo_list.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingAiButtonManagerTest {

    @Test
    fun testDockingTargetX_leftSideSnapsToLeftBorder() {
        val screenWidth = 1080
        val buttonWidth = 150 // e.g. 52dp in px
        
        // Button dragged to left side (e.g. currentX = 100)
        val targetX = FloatingAiButtonManager.calculateDockedTargetX(
            currentX = 100,
            buttonWidth = buttonWidth,
            screenWidth = screenWidth
        )
        assertEquals(0, targetX)
    }

    @Test
    fun testDockingTargetX_rightSideSnapsToRightBorder() {
        val screenWidth = 1080
        val buttonWidth = 150
        
        // Button dragged to right side (e.g. currentX = 800)
        val targetX = FloatingAiButtonManager.calculateDockedTargetX(
            currentX = 800,
            buttonWidth = buttonWidth,
            screenWidth = screenWidth
        )
        // Must snap exactly to screenWidth - buttonWidth (right border)
        assertEquals(screenWidth - buttonWidth, targetX)
    }

    @Test
    fun testDockingTargetX_releasedAtExactCenterSnapsToBorder() {
        val screenWidth = 1000
        val buttonWidth = 100
        
        // Center of button is at 500 (currentX = 450, buttonWidth = 100 -> centerX = 500)
        // It must NOT stay in the center! It must snap to either left or right border.
        val targetX = FloatingAiButtonManager.calculateDockedTargetX(
            currentX = 450,
            buttonWidth = buttonWidth,
            screenWidth = screenWidth
        )
        // Must resolve to border, never remaining at 450
        assertTrue(targetX == 0 || targetX == (screenWidth - buttonWidth))
    }

    @Test
    fun testDockingTargetX_releasedSlightlyLeftOfCenterSnapsToLeftBorder() {
        val screenWidth = 1000
        val buttonWidth = 100
        // centerX = 499 (left of 500)
        val targetX = FloatingAiButtonManager.calculateDockedTargetX(
            currentX = 449,
            buttonWidth = buttonWidth,
            screenWidth = screenWidth
        )
        assertEquals(0, targetX)
    }

    @Test
    fun testDockingTargetX_releasedSlightlyRightOfCenterSnapsToRightBorder() {
        val screenWidth = 1000
        val buttonWidth = 100
        // centerX = 501 (right of 500)
        val targetX = FloatingAiButtonManager.calculateDockedTargetX(
            currentX = 451,
            buttonWidth = buttonWidth,
            screenWidth = screenWidth
        )
        assertEquals(900, targetX)
    }

    @Test
    fun testClampYPosition_keepsWithinSafeMargins() {
        val screenHeight = 2400
        val buttonHeight = 150
        val topMargin = 120
        val bottomMargin = 160

        // Dragged above status bar (y = -50)
        val clampedTop = FloatingAiButtonManager.clampYPosition(
            currentY = -50,
            buttonHeight = buttonHeight,
            screenHeight = screenHeight,
            topMargin = topMargin,
            bottomMargin = bottomMargin
        )
        assertEquals(topMargin, clampedTop)

        // Dragged into navigation bar area (y = 2350)
        val clampedBottom = FloatingAiButtonManager.clampYPosition(
            currentY = 2350,
            buttonHeight = buttonHeight,
            screenHeight = screenHeight,
            topMargin = topMargin,
            bottomMargin = bottomMargin
        )
        val expectedMaxY = screenHeight - buttonHeight - bottomMargin
        assertEquals(expectedMaxY, clampedBottom)

        // Normal middle Y position
        val clampedMiddle = FloatingAiButtonManager.clampYPosition(
            currentY = 1000,
            buttonHeight = buttonHeight,
            screenHeight = screenHeight,
            topMargin = topMargin,
            bottomMargin = bottomMargin
        )
        assertEquals(1000, clampedMiddle)
    }

    @Test
    fun testClampYPosition_strictlyExcludesTopAndBottomNavBars() {
        val screenHeight = 2400
        val buttonHeight = 150
        val topNavBarBoundary = 160 // Top nav bar height with large title & search
        val bottomNavBarBoundary = 115 // Bottom nav bar with center Plus button & insets

        // Attempting to drag into the top navigation bar area
        val clampedTop = FloatingAiButtonManager.clampYPosition(
            currentY = 80,
            buttonHeight = buttonHeight,
            screenHeight = screenHeight,
            topMargin = topNavBarBoundary,
            bottomMargin = bottomNavBarBoundary
        )
        assertTrue("Must be at or below top navigation bar boundary", clampedTop >= topNavBarBoundary)

        // Attempting to drag into the bottom navigation bar area
        val clampedBottom = FloatingAiButtonManager.clampYPosition(
            currentY = screenHeight - 50,
            buttonHeight = buttonHeight,
            screenHeight = screenHeight,
            topMargin = topNavBarBoundary,
            bottomMargin = bottomNavBarBoundary
        )
        val maxAllowedY = screenHeight - buttonHeight - bottomNavBarBoundary
        assertTrue("Must be at or above bottom navigation bar boundary", clampedBottom <= maxAllowedY)
    }

    @Test
    fun testFloatingAiColor_paletteOptions() {
        val colors = FloatingAiColor.entries
        assertEquals(8, colors.size)
        assertTrue(colors.any { it.id == "purple" })
        assertTrue(colors.any { it.id == "blue" })
        assertTrue(colors.any { it.id == "coral" })
        assertTrue(colors.any { it.id == "emerald" })
        assertTrue(colors.any { it.id == "cyan" })
        assertTrue(colors.any { it.id == "obsidian" })
        assertTrue(colors.any { it.id == "gold" })
        assertTrue(colors.any { it.id == "rose" })

        assertEquals(FloatingAiColor.PURPLE, FloatingAiColor.fromId("purple"))
        assertEquals(FloatingAiColor.DEFAULT, FloatingAiColor.fromId("unknown_color"))
    }

    @Test
    fun testFloatingAiGlyph_assistantIcons() {
        val glyphs = FloatingAiGlyph.entries
        assertEquals(6, glyphs.size)
        assertTrue(glyphs.any { it.id == "sparkle" })
        assertTrue(glyphs.any { it.id == "chat" })
        assertTrue(glyphs.any { it.id == "bot" })
        assertTrue(glyphs.any { it.id == "bolt" })
        assertTrue(glyphs.any { it.id == "check" })
        assertTrue(glyphs.any { it.id == "brain" })

        assertEquals(FloatingAiGlyph.SPARKLE, FloatingAiGlyph.fromId("sparkle"))
        assertEquals(FloatingAiGlyph.DEFAULT, FloatingAiGlyph.fromId("invalid_glyph"))
    }
}

