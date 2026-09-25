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
}
