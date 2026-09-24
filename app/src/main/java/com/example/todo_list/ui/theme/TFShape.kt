package com.example.todo_list.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * TaskFlow Shape Tokens (Apple iOS HIG Specification).
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 5.2
 */
object TFShape {
    val card = RoundedCornerShape(12.dp)
    val searchField = RoundedCornerShape(10.dp)
    val chip = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(20.dp)
    val segmentedTrack = RoundedCornerShape(9.dp)
    val segmentedThumb = RoundedCornerShape(7.dp)
    val button = RoundedCornerShape(14.dp)
    val sheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val alertDialog = RoundedCornerShape(14.dp)
    val badge = RoundedCornerShape(10.dp)
    val circle = CircleShape
}
