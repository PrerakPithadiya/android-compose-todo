package com.example.todo_list

import com.example.todo_list.data.repository.WorldTimezoneRepository
import com.example.todo_list.model.TaskItem
import com.example.todo_list.utils.TimeFormatHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeFormatHelperTest {

    @Test
    fun testParseHourMinute_12Hour() {
        val (h1, m1) = TimeFormatHelper.parseHourMinute("09:30 AM")
        assertEquals(9, h1)
        assertEquals(30, m1)

        val (h2, m2) = TimeFormatHelper.parseHourMinute("02:15 PM")
        assertEquals(14, h2)
        assertEquals(15, m2)

        val (h3, m3) = TimeFormatHelper.parseHourMinute("12:00 AM")
        assertEquals(0, h3)
        assertEquals(0, m3)

        val (h4, m4) = TimeFormatHelper.parseHourMinute("12:45 PM")
        assertEquals(12, h4)
        assertEquals(45, m4)
    }

    @Test
    fun testParseHourMinute_24Hour() {
        val (h1, m1) = TimeFormatHelper.parseHourMinute("09:30")
        assertEquals(9, h1)
        assertEquals(30, m1)

        val (h2, m2) = TimeFormatHelper.parseHourMinute("14:15")
        assertEquals(14, h2)
        assertEquals(15, m2)

        val (h3, m3) = TimeFormatHelper.parseHourMinute("00:00")
        assertEquals(0, h3)
        assertEquals(0, m3)

        val (h4, m4) = TimeFormatHelper.parseHourMinute("23:59")
        assertEquals(23, h4)
        assertEquals(59, m4)
    }

    @Test
    fun testParseToMinutes_sortingConsistency() {
        // "09:30 AM" and "09:30" should yield identical minute values
        assertEquals(TimeFormatHelper.parseToMinutes("09:30 AM"), TimeFormatHelper.parseToMinutes("09:30"))
        // "02:15 PM" and "14:15" should yield identical minute values
        assertEquals(TimeFormatHelper.parseToMinutes("02:15 PM"), TimeFormatHelper.parseToMinutes("14:15"))
        // 14:15 > 09:30
        assertTrue(TimeFormatHelper.parseToMinutes("14:15") > TimeFormatHelper.parseToMinutes("09:30 AM"))
    }

    @Test
    fun testFormatTimeForDisplay() {
        // Converting 12h to 24h
        assertEquals("09:30", TimeFormatHelper.formatTimeForDisplay("09:30 AM", is24Hour = true))
        assertEquals("14:15", TimeFormatHelper.formatTimeForDisplay("02:15 PM", is24Hour = true))
        assertEquals("00:00", TimeFormatHelper.formatTimeForDisplay("12:00 AM", is24Hour = true))
        assertEquals("12:30", TimeFormatHelper.formatTimeForDisplay("12:30 PM", is24Hour = true))

        // Converting 24h to 12h
        assertEquals("09:30 AM", TimeFormatHelper.formatTimeForDisplay("09:30", is24Hour = false))
        assertEquals("02:15 PM", TimeFormatHelper.formatTimeForDisplay("14:15", is24Hour = false))
        assertEquals("12:00 AM", TimeFormatHelper.formatTimeForDisplay("00:00", is24Hour = false))
        assertEquals("11:59 PM", TimeFormatHelper.formatTimeForDisplay("23:59", is24Hour = false))
    }

    @Test
    fun testTaskItemSortingIntegration() {
        val task1 = TaskItem(id = "1", title = "Task 1", category = "Work", time = "09:00 AM")
        val task2 = TaskItem(id = "2", title = "Task 2", category = "Work", time = "14:00")
        val task3 = TaskItem(id = "3", title = "Task 3", category = "Work", time = "01:00 PM")

        assertTrue(task1.getTimeInMinutes() < task3.getTimeInMinutes())
        assertTrue(task3.getTimeInMinutes() < task2.getTimeInMinutes())
    }

    @Test
    fun testWorldTimezoneRepository_searchCountry() {
        val resultsIndia = WorldTimezoneRepository.search("India")
        assertTrue(resultsIndia.countryMatches.any { it.countryName.equals("India", ignoreCase = true) })
        assertTrue(resultsIndia.countryMatches.first().flagEmoji.isNotEmpty())

        val resultsUS = WorldTimezoneRepository.search("United States")
        assertTrue(resultsUS.countryMatches.any { it.countryName.contains("United States", ignoreCase = true) })
    }

    @Test
    fun testWorldTimezoneRepository_searchCities() {
        val resultsTokyo = WorldTimezoneRepository.search("Tokyo")
        assertTrue(resultsTokyo.cityMatches.any { it.cityName.equals("Tokyo", ignoreCase = true) })
        val tokyo = resultsTokyo.cityMatches.first { it.cityName.equals("Tokyo", ignoreCase = true) }
        assertEquals("Asia/Tokyo", tokyo.timeZoneId)

        val resultsLondon = WorldTimezoneRepository.search("London")
        assertTrue(resultsLondon.cityMatches.any { it.cityName.equals("London", ignoreCase = true) })
    }

    @Test
    fun testWorldTimezoneRepository_allIanaAvailable() {
        // Test an IANA timezone that might not be in a simple top-20 list (e.g. Zurich or Reykjavik)
        val resultsZurich = WorldTimezoneRepository.search("Zurich")
        assertTrue(resultsZurich.cityMatches.any { it.cityName.contains("Zurich", ignoreCase = true) })
    }

    @Test
    fun testWorldLocation_displayLocation() {
        val resultsUS = WorldTimezoneRepository.search("United States")
        val usCountry = resultsUS.countryMatches.first { it.countryName.equals("United States", ignoreCase = true) }
        // Country primary location should cleanly display "United States", not "United States, United States"
        assertEquals("United States", usCountry.displayLocation)

        val resultsTokyo = WorldTimezoneRepository.search("Tokyo")
        val tokyoCity = resultsTokyo.cityMatches.first { it.cityName.equals("Tokyo", ignoreCase = true) }
        // City location should display "Tokyo, Japan"
        assertEquals("Tokyo, Japan", tokyoCity.displayLocation)
    }
}
