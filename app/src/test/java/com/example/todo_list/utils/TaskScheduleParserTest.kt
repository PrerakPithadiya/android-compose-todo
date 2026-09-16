package com.example.todo_list.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskScheduleParserTest {

    @Test
    fun testParse_todayAndTomorrow() {
        val todayResult = TaskScheduleParser.parse("Submit report today")
        assertEquals("Today", todayResult.suggestedDate)
        assertEquals("Submit report", todayResult.cleanedTitle)

        val tomorrowResult = TaskScheduleParser.parse("Doctor appointment tomorrow")
        assertEquals("Tomorrow", tomorrowResult.suggestedDate)
        assertEquals("Doctor appointment", tomorrowResult.cleanedTitle)
    }

    @Test
    fun testParse_time12Hour() {
        val result = TaskScheduleParser.parse("Team sync at 4:30 pm", is24Hour = false)
        assertEquals("04:30 PM", result.suggestedTime)
        assertEquals("Team sync", result.cleanedTitle)

        val result2 = TaskScheduleParser.parse("Standup at 9am", is24Hour = false)
        assertEquals("09:00 AM", result2.suggestedTime)
        assertEquals("Standup", result2.cleanedTitle)
    }

    @Test
    fun testParse_time24Hour() {
        val result = TaskScheduleParser.parse("Gym at 17:30", is24Hour = true)
        assertEquals("17:30", result.suggestedTime)

        val result2 = TaskScheduleParser.parse("Flight at 6am", is24Hour = true)
        assertEquals("06:00", result2.suggestedTime)
    }

    @Test
    fun testParse_timeKeywords() {
        val morning = TaskScheduleParser.parse("Jogging morning", is24Hour = false)
        assertEquals("09:00 AM", morning.suggestedTime)

        val evening = TaskScheduleParser.parse("Dinner evening", is24Hour = false)
        assertEquals("06:00 PM", evening.suggestedTime)
    }

    @Test
    fun testParse_priorityAndCategory() {
        val result = TaskScheduleParser.parse(
            "Fix production outage urgent #work",
            knownCategories = listOf("Work", "Personal")
        )
        assertEquals("HIGH", result.suggestedPriority)
        assertEquals("Work", result.suggestedCategory)
        assertTrue(result.hasSuggestions)
        assertTrue(result.getSummaryPillText().contains("High Priority"))
        assertTrue(result.getSummaryPillText().contains("#Work"))
    }

    @Test
    fun testParse_comprehensiveSentence() {
        val result = TaskScheduleParser.parse("Prepare pitch deck tomorrow at 3pm high priority #work")
        assertEquals("Tomorrow", result.suggestedDate)
        assertEquals("03:00 PM", result.suggestedTime)
        assertEquals("HIGH", result.suggestedPriority)
        assertEquals("Work", result.suggestedCategory)
        assertEquals("Prepare pitch deck", result.cleanedTitle)
    }
}
