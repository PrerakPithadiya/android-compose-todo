package com.example.todo_list.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/**
 * High-performance, offline natural-language schedule parser for TaskFlow.
 * Automatically recognizes dates ("today", "tomorrow", "next monday"),
 * times ("5pm", "9:30 am", "17:00", "morning"), priorities ("urgent", "high priority"),
 * and category tags ("#work") to power Apple Intelligence smart suggestions.
 */
data class ParsedTaskSchedule(
    val originalTitle: String,
    val cleanedTitle: String,
    val suggestedDate: String? = null,
    val suggestedEpochDay: Long? = null,
    val suggestedTime: String? = null,
    val suggestedPriority: String? = null,
    val suggestedCategory: String? = null,
    val matchedDateText: String? = null,
    val matchedTimeText: String? = null,
    val matchedPriorityText: String? = null,
    val matchedCategoryText: String? = null
) {
    val hasSuggestions: Boolean
        get() = suggestedDate != null || suggestedTime != null || suggestedPriority != null || suggestedCategory != null

    /**
     * Generates a sleek, human-readable summary badge text for the Apple Intelligence pill.
     */
    fun getSummaryPillText(): String {
        val parts = mutableListOf<String>()
        if (suggestedDate != null) parts.add(suggestedDate)
        if (suggestedTime != null) parts.add(suggestedTime)
        if (suggestedPriority != null && suggestedPriority != "NONE") {
            parts.add(suggestedPriority.lowercase().replaceFirstChar { it.uppercase() } + " Priority")
        }
        if (suggestedCategory != null) parts.add("#$suggestedCategory")
        return parts.joinToString(" • ")
    }
}

object TaskScheduleParser {

    private val DATE_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(today)\\b"),
        Pattern.compile("(?i)\\b(tomorrow|tmrw)\\b"),
        Pattern.compile("(?i)\\b(tonight)\\b"),
        Pattern.compile("(?i)\\b(?:next\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b"),
        Pattern.compile("(?i)\\b(?:next\\s+)?(mon|tue|wed|thu|fri|sat|sun)\\b"),
        Pattern.compile("(?i)\\b(this\\s+weekend|next\\s+week)\\b")
    )

    private val TIME_PATTERNS = listOf(
        // e.g. "at 4:30 pm", "4:30pm", "14:30"
        Pattern.compile("(?i)\\b(?:at\\s+)?([0-1]?[0-9]|2[0-3]):([0-5][0-9])\\s*(am|pm)?\\b"),
        // e.g. "at 9am", "5pm", "9 am"
        Pattern.compile("(?i)\\b(?:at\\s+)?([1-9]|1[0-2])\\s*(am|pm)\\b"),
        // Relative times
        Pattern.compile("(?i)\\b(?:in\\s+the\\s+)?(morning)\\b"),
        Pattern.compile("(?i)\\b(?:in\\s+the\\s+)?(afternoon)\\b"),
        Pattern.compile("(?i)\\b(?:in\\s+the\\s+)?(evening)\\b"),
        Pattern.compile("(?i)\\b(?:at\\s+)?(night)\\b")
    )

    private val PRIORITY_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(urgent|high\\s+priority|important|asap|p1|critical)\\b") to "HIGH",
        Pattern.compile("(?i)\\b(medium\\s+priority|normal\\s+priority|p2)\\b") to "MEDIUM",
        Pattern.compile("(?i)\\b(low\\s+priority|p3)\\b") to "LOW"
    )

    private val CATEGORY_PATTERN = Pattern.compile("(?i)#([a-zA-Z0-9_-]+)")

    private val TASK_INTENT_PREFIX_PATTERN = Pattern.compile(
        "(?i)^(?:please\\s+)?(?:add(?:\\s+a)?(?:\\s+new)?\\s+task|create(?:\\s+a)?(?:\\s+new)?\\s+task|new\\s+task|remind\\s+me\\s+to|add\\s+to\\s+(?:my\\s+)?(?:list|tasks?)|schedule(?:\\s+a)?(?:\\s+meeting|\\s+task)?|todo:?)\\s*[:,-]?\\s*",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Determines whether the given user message expresses intent to create/schedule a new task.
     */
    fun isTaskCreationIntent(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return false
        val matcher = TASK_INTENT_PREFIX_PATTERN.matcher(trimmed)
        return matcher.find() && matcher.start() == 0
    }

    /**
     * Strips leading command intent phrases (e.g. "Add task", "Remind me to", "New task:")
     * so that only the semantic task title and attributes remain.
     */
    fun stripTaskIntentPrefix(input: String): String {
        val trimmed = input.trim()
        val matcher = TASK_INTENT_PREFIX_PATTERN.matcher(trimmed)
        return if (matcher.find() && matcher.start() == 0) {
            matcher.replaceFirst("").trim()
        } else {
            trimmed
        }
    }

    fun parse(
        input: String,
        is24Hour: Boolean = false,
        knownCategories: List<String> = emptyList()
    ): ParsedTaskSchedule {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return ParsedTaskSchedule(originalTitle = input, cleanedTitle = input)
        }

        var cleaned = trimmed
        var suggestedDate: String? = null
        var suggestedEpochDay: Long? = null
        var matchedDateText: String? = null

        val currentEpochDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)

        // 1. Date Detection
        for (pattern in DATE_PATTERNS) {
            val matcher = pattern.matcher(cleaned)
            if (matcher.find()) {
                val match = matcher.group().lowercase(Locale.US)
                matchedDateText = matcher.group()

                when {
                    match.contains("today") || match.contains("tonight") -> {
                        suggestedDate = "Today"
                        suggestedEpochDay = currentEpochDay
                    }
                    match.contains("tomorrow") || match.contains("tmrw") -> {
                        suggestedDate = "Tomorrow"
                        suggestedEpochDay = currentEpochDay + 1
                    }
                    match.contains("weekend") -> {
                        val cal = Calendar.getInstance()
                        val daysUntilSaturday = (Calendar.SATURDAY - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
                        val targetDays = if (daysUntilSaturday == 0) 7 else daysUntilSaturday
                        cal.add(Calendar.DAY_OF_YEAR, targetDays)
                        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                        suggestedDate = formatter.format(cal.time)
                        suggestedEpochDay = cal.timeInMillis / (1000 * 60 * 60 * 24)
                    }
                    match.contains("next week") -> {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, 7)
                        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                        suggestedDate = formatter.format(cal.time)
                        suggestedEpochDay = cal.timeInMillis / (1000 * 60 * 60 * 24)
                    }
                    else -> {
                        // Day of week
                        val targetDayOfWeek = when {
                            match.contains("mon") -> Calendar.MONDAY
                            match.contains("tue") -> Calendar.TUESDAY
                            match.contains("wed") -> Calendar.WEDNESDAY
                            match.contains("thu") -> Calendar.THURSDAY
                            match.contains("fri") -> Calendar.FRIDAY
                            match.contains("sat") -> Calendar.SATURDAY
                            match.contains("sun") -> Calendar.SUNDAY
                            else -> null
                        }

                        if (targetDayOfWeek != null) {
                            val cal = Calendar.getInstance()
                            val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                            var diff = targetDayOfWeek - currentDayOfWeek
                            if (diff <= 0) diff += 7
                            cal.add(Calendar.DAY_OF_YEAR, diff)

                            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                            suggestedDate = formatter.format(cal.time)
                            suggestedEpochDay = cal.timeInMillis / (1000 * 60 * 60 * 24)
                        }
                    }
                }
                break
            }
        }

        // 2. Time Detection
        var suggestedTime: String? = null
        var matchedTimeText: String? = null

        for (pattern in TIME_PATTERNS) {
            val matcher = pattern.matcher(cleaned)
            if (matcher.find()) {
                matchedTimeText = matcher.group()
                val fullMatch = matcher.group().lowercase(Locale.US)

                suggestedTime = when {
                    fullMatch.contains("morning") -> TimeFormatHelper.formatHourMinute(9, 0, is24Hour)
                    fullMatch.contains("afternoon") -> TimeFormatHelper.formatHourMinute(13, 0, is24Hour)
                    fullMatch.contains("evening") -> TimeFormatHelper.formatHourMinute(18, 0, is24Hour)
                    fullMatch.contains("night") -> TimeFormatHelper.formatHourMinute(21, 0, is24Hour)
                    else -> {
                        // Numeric parse
                        try {
                            if (fullMatch.contains(":")) {
                                val hour = matcher.group(1)?.toInt() ?: 9
                                val minute = matcher.group(2)?.toInt() ?: 0
                                val amPm = matcher.group(3)?.lowercase(Locale.US)

                                var adjustedHour = hour
                                if (amPm == "pm" && adjustedHour < 12) adjustedHour += 12
                                if (amPm == "am" && adjustedHour == 12) adjustedHour = 0

                                TimeFormatHelper.formatHourMinute(adjustedHour, minute, is24Hour)
                            } else {
                                val hour = matcher.group(1)?.toInt() ?: 9
                                val amPm = matcher.group(2)?.lowercase(Locale.US)

                                var adjustedHour = hour
                                if (amPm == "pm" && adjustedHour < 12) adjustedHour += 12
                                if (amPm == "am" && adjustedHour == 12) adjustedHour = 0

                                TimeFormatHelper.formatHourMinute(adjustedHour, 0, is24Hour)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                break
            }
        }

        // 3. Priority Detection
        var suggestedPriority: String? = null
        var matchedPriorityText: String? = null
        for ((pattern, prio) in PRIORITY_PATTERNS) {
            val matcher = pattern.matcher(cleaned)
            if (matcher.find()) {
                suggestedPriority = prio
                matchedPriorityText = matcher.group()
                break
            }
        }

        // 4. Category Tag Detection (e.g. #work, #personal)
        var suggestedCategory: String? = null
        var matchedCategoryText: String? = null
        val catMatcher = CATEGORY_PATTERN.matcher(cleaned)
        if (catMatcher.find()) {
            val tag = catMatcher.group(1).orEmpty()
            matchedCategoryText = catMatcher.group()
            val match = knownCategories.firstOrNull { it.equals(tag, ignoreCase = true) }
            suggestedCategory = match ?: tag.lowercase().replaceFirstChar { it.uppercase() }
        }

        // Clean title by optionally stripping matched keywords if needed
        val tokensToRemove = listOfNotNull(matchedDateText, matchedTimeText, matchedPriorityText, matchedCategoryText)
        for (token in tokensToRemove) {
            cleaned = cleaned.replaceFirst(Pattern.quote(token).toRegex(RegexOption.IGNORE_CASE), "").trim()
        }
        cleaned = cleaned.replace("\\s+".toRegex(), " ").trim()

        return ParsedTaskSchedule(
            originalTitle = trimmed,
            cleanedTitle = if (cleaned.isNotBlank()) cleaned else trimmed,
            suggestedDate = suggestedDate,
            suggestedEpochDay = suggestedEpochDay,
            suggestedTime = suggestedTime,
            suggestedPriority = suggestedPriority,
            suggestedCategory = suggestedCategory,
            matchedDateText = matchedDateText,
            matchedTimeText = matchedTimeText,
            matchedPriorityText = matchedPriorityText,
            matchedCategoryText = matchedCategoryText
        )
    }
}
