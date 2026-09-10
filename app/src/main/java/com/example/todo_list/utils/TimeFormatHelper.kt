package com.example.todo_list.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Utility helper for bidirectional 12-hour / 24-hour time formatting,
 * parsing, minute calculations, and quick-time selections.
 */
object TimeFormatHelper {

    /**
     * Parses any time string ("09:30 AM", "9:30 AM", "09:30", "14:15")
     * into a Pair of (hourOfDay in 0..23, minute in 0..59).
     */
    fun parseHourMinute(timeStr: String): Pair<Int, Int> {
        val trimmed = timeStr.trim()
        if (trimmed.isEmpty()) return Pair(9, 0)

        // Case 1: Contains AM or PM (12-hour format)
        if (trimmed.contains("AM", ignoreCase = true) || trimmed.contains("PM", ignoreCase = true)) {
            return try {
                val parts = trimmed.split(" ")
                val timeParts = parts[0].split(":")
                var hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()
                val amPm = parts.getOrNull(1)?.uppercase(Locale.US) ?: "AM"

                if (amPm == "PM" && hour < 12) hour += 12
                if (amPm == "AM" && hour == 12) hour = 0

                Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            } catch (e: Exception) {
                Pair(9, 0)
            }
        }

        // Case 2: 24-hour format ("HH:mm" or "H:mm")
        if (trimmed.contains(":")) {
            return try {
                val parts = trimmed.split(":")
                val hour = parts[0].trim().toInt()
                val minute = parts[1].trim().take(2).toInt()
                Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            } catch (e: Exception) {
                Pair(9, 0)
            }
        }

        return Pair(9, 0)
    }

    /**
     * Calculates the total minutes from midnight (0..1439) for sorting.
     */
    fun parseToMinutes(timeStr: String): Int {
        val (hour, minute) = parseHourMinute(timeStr)
        return hour * 60 + minute
    }

    /**
     * Formats hourOfDay (0..23) and minute (0..59) into 12-hour or 24-hour string.
     */
    fun formatHourMinute(hourOfDay: Int, minute: Int, is24Hour: Boolean): String {
        return if (is24Hour) {
            String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
        } else {
            val isPm = hourOfDay >= 12
            val displayHour = when {
                hourOfDay == 0 -> 12
                hourOfDay > 12 -> hourOfDay - 12
                else -> hourOfDay
            }
            val amPm = if (isPm) "PM" else "AM"
            String.format(Locale.US, "%02d:%02d %s", displayHour, minute, amPm)
        }
    }

    /**
     * Dynamically adapts any raw stored time string to the user's active display preference.
     * E.g. raw "09:30 AM" with is24Hour=true -> "09:30"
     * E.g. raw "14:15" with is24Hour=false -> "02:15 PM"
     */
    fun formatTimeForDisplay(rawTime: String, is24Hour: Boolean): String {
        if (rawTime.isBlank()) return if (is24Hour) "09:00" else "09:00 AM"
        val (hour, minute) = parseHourMinute(rawTime)
        return formatHourMinute(hour, minute, is24Hour)
    }

    /**
     * Generates a curated list of quick-time chips based on active format.
     */
    fun getQuickTimes(is24Hour: Boolean): List<String> {
        return if (is24Hour) {
            listOf("09:00", "11:30", "14:00", "17:00", "20:00")
        } else {
            listOf("09:00 AM", "11:30 AM", "02:00 PM", "05:00 PM", "08:00 PM")
        }
    }

    /**
     * Formats the current time in a given timezone and format.
     */
    fun getFormattedCurrentTime(timezoneId: String, is24Hour: Boolean): String {
        val tz = java.util.TimeZone.getTimeZone(timezoneId)
        val cal = Calendar.getInstance(tz)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        return formatHourMinute(hour, minute, is24Hour)
    }
}
