package com.example.todo_list.model

/**
 * Data model representing a global city, country, or timezone location.
 */
data class WorldLocation(
    val cityName: String,
    val countryName: String,
    val countryCode: String,
    val flagEmoji: String,
    val timeZoneId: String,
    val timeZoneName: String,
    val timeZoneAbbr: String,
    val utcOffsetStr: String,
    val isCountryPrimary: Boolean = false
) {
    /**
     * Unique key for Compose list rendering.
     */
    val id: String get() = if (isCountryPrimary) "country_${countryCode}_$timeZoneId" else "${cityName}_${countryCode}_$timeZoneId"

    /**
     * Searchable text blob containing city, country, abbreviation, and timezone ID.
     */
    val searchKeywords: String get() = "$cityName $countryName $countryCode $timeZoneAbbr $timeZoneName $utcOffsetStr $timeZoneId".lowercase()

    /**
     * Clean user-facing display string for this location:
     * e.g., "United States" for whole country, or "New York, United States" for a specific city.
     * Prevents duplicate text like "United States, United States".
     */
    val displayLocation: String
        get() = if (isCountryPrimary || cityName.equals(countryName, ignoreCase = true) || cityName.isBlank()) {
            countryName
        } else {
            "$cityName, $countryName"
        }
}
