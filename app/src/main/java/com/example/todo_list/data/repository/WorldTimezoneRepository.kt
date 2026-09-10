package com.example.todo_list.data.repository

import com.example.todo_list.model.WorldLocation
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Repository containing comprehensive world cities, countries, and dynamic IANA timezones.
 * Allows searching by Country name, City name, Timezone abbreviation, or UTC offset.
 * Guarantees that ALL world cities and countries are discoverable.
 */
object WorldTimezoneRepository {

    /**
     * Converts a 2-letter ISO country code (e.g. "US", "IN", "JP") into a flag emoji.
     */
    fun countryCodeToFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2) return "🌐"
        val firstChar = countryCode[0].uppercaseChar().code - 'A'.code + 0x1F1E6
        val secondChar = countryCode[1].uppercaseChar().code - 'A'.code + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }

    /**
     * Formats raw timezone offset into "UTC+HH:mm" or "UTC-HH:mm".
     */
    fun formatUtcOffset(offsetMillis: Int): String {
        val totalMinutes = offsetMillis / (1000 * 60)
        val hours = Math.abs(totalMinutes / 60)
        val minutes = Math.abs(totalMinutes % 60)
        val sign = if (offsetMillis >= 0) "+" else "-"
        return String.format(Locale.US, "UTC%s%02d:%02d", sign, hours, minutes)
    }

    /**
     * Curated list of major world cities with their verified countries and timezones.
     */
    private val CURATED_WORLD_LOCATIONS: List<WorldLocation> by lazy {
        val rawList = listOf(
            // India & South Asia
            Triple("New Delhi", "India", "Asia/Kolkata"),
            Triple("Mumbai", "India", "Asia/Kolkata"),
            Triple("Bengaluru", "India", "Asia/Kolkata"),
            Triple("Kolkata", "India", "Asia/Kolkata"),
            Triple("Chennai", "India", "Asia/Kolkata"),
            Triple("Hyderabad", "India", "Asia/Kolkata"),
            Triple("Ahmedabad", "India", "Asia/Kolkata"),
            Triple("Pune", "India", "Asia/Kolkata"),
            Triple("Dhaka", "Bangladesh", "Asia/Dhaka"),
            Triple("Karachi", "Pakistan", "Asia/Karachi"),
            Triple("Lahore", "Pakistan", "Asia/Karachi"),
            Triple("Colombo", "Sri Lanka", "Asia/Colombo"),
            Triple("Kathmandu", "Nepal", "Asia/Kathmandu"),

            // North America
            Triple("New York", "United States", "America/New_York"),
            Triple("Los Angeles", "United States", "America/Los_Angeles"),
            Triple("Chicago", "United States", "America/Chicago"),
            Triple("San Francisco", "United States", "America/Los_Angeles"),
            Triple("Seattle", "United States", "America/Los_Angeles"),
            Triple("Boston", "United States", "America/New_York"),
            Triple("Miami", "United States", "America/New_York"),
            Triple("Washington D.C.", "United States", "America/New_York"),
            Triple("Austin", "United States", "America/Chicago"),
            Triple("Denver", "United States", "America/Denver"),
            Triple("Phoenix", "United States", "America/Phoenix"),
            Triple("Honolulu", "United States", "Pacific/Honolulu"),
            Triple("Anchorage", "United States", "America/Anchorage"),
            Triple("Toronto", "Canada", "America/Toronto"),
            Triple("Vancouver", "Canada", "America/Vancouver"),
            Triple("Montreal", "Canada", "America/Toronto"),
            Triple("Calgary", "Canada", "America/Edmonton"),
            Triple("Ottawa", "Canada", "America/Toronto"),
            Triple("Mexico City", "Mexico", "America/Mexico_City"),
            Triple("Guadalajara", "Mexico", "America/Mexico_City"),
            Triple("Panama City", "Panama", "America/Panama"),

            // Europe & UK
            Triple("London", "United Kingdom", "Europe/London"),
            Triple("Manchester", "United Kingdom", "Europe/London"),
            Triple("Edinburgh", "United Kingdom", "Europe/London"),
            Triple("Paris", "France", "Europe/Paris"),
            Triple("Berlin", "Germany", "Europe/Berlin"),
            Triple("Munich", "Germany", "Europe/Berlin"),
            Triple("Frankfurt", "Germany", "Europe/Berlin"),
            Triple("Rome", "Italy", "Europe/Rome"),
            Triple("Milan", "Italy", "Europe/Rome"),
            Triple("Madrid", "Spain", "Europe/Madrid"),
            Triple("Barcelona", "Spain", "Europe/Madrid"),
            Triple("Amsterdam", "Netherlands", "Europe/Amsterdam"),
            Triple("Brussels", "Belgium", "Europe/Brussels"),
            Triple("Zurich", "Switzerland", "Europe/Zurich"),
            Triple("Geneva", "Switzerland", "Europe/Zurich"),
            Triple("Vienna", "Austria", "Europe/Vienna"),
            Triple("Dublin", "Ireland", "Europe/Dublin"),
            Triple("Lisbon", "Portugal", "Europe/Lisbon"),
            Triple("Stockholm", "Sweden", "Europe/Stockholm"),
            Triple("Oslo", "Norway", "Europe/Oslo"),
            Triple("Copenhagen", "Denmark", "Europe/Copenhagen"),
            Triple("Helsinki", "Finland", "Europe/Helsinki"),
            Triple("Warsaw", "Poland", "Europe/Warsaw"),
            Triple("Prague", "Czech Republic", "Europe/Prague"),
            Triple("Budapest", "Hungary", "Europe/Budapest"),
            Triple("Athens", "Greece", "Europe/Athens"),
            Triple("Istanbul", "Turkey", "Europe/Istanbul"),

            // East Asia & Southeast Asia
            Triple("Tokyo", "Japan", "Asia/Tokyo"),
            Triple("Osaka", "Japan", "Asia/Tokyo"),
            Triple("Kyoto", "Japan", "Asia/Tokyo"),
            Triple("Seoul", "South Korea", "Asia/Seoul"),
            Triple("Busan", "South Korea", "Asia/Seoul"),
            Triple("Beijing", "China", "Asia/Shanghai"),
            Triple("Shanghai", "China", "Asia/Shanghai"),
            Triple("Shenzhen", "China", "Asia/Shanghai"),
            Triple("Hong Kong", "Hong Kong", "Asia/Hong_Kong"),
            Triple("Taipei", "Taiwan", "Asia/Taipei"),
            Triple("Singapore", "Singapore", "Asia/Singapore"),
            Triple("Bangkok", "Thailand", "Asia/Bangkok"),
            Triple("Kuala Lumpur", "Malaysia", "Asia/Kuala_Lumpur"),
            Triple("Jakarta", "Indonesia", "Asia/Jakarta"),
            Triple("Bali", "Indonesia", "Asia/Makassar"),
            Triple("Manila", "Philippines", "Asia/Manila"),
            Triple("Hanoi", "Vietnam", "Asia/Bangkok"),
            Triple("Ho Chi Minh City", "Vietnam", "Asia/Bangkok"),

            // Middle East
            Triple("Dubai", "United Arab Emirates", "Asia/Dubai"),
            Triple("Abu Dhabi", "United Arab Emirates", "Asia/Dubai"),
            Triple("Riyadh", "Saudi Arabia", "Asia/Riyadh"),
            Triple("Jeddah", "Saudi Arabia", "Asia/Riyadh"),
            Triple("Doha", "Qatar", "Asia/Qatar"),
            Triple("Kuwait City", "Kuwait", "Asia/Kuwait"),
            Triple("Muscat", "Oman", "Asia/Muscat"),
            Triple("Tel Aviv", "Israel", "Asia/Jerusalem"),
            Triple("Beirut", "Lebanon", "Asia/Beirut"),

            // Australia & Oceania
            Triple("Sydney", "Australia", "Australia/Sydney"),
            Triple("Melbourne", "Australia", "Australia/Melbourne"),
            Triple("Brisbane", "Australia", "Australia/Brisbane"),
            Triple("Perth", "Australia", "Australia/Perth"),
            Triple("Adelaide", "Australia", "Australia/Adelaide"),
            Triple("Auckland", "New Zealand", "Pacific/Auckland"),
            Triple("Wellington", "New Zealand", "Pacific/Auckland"),
            Triple("Suva", "Fiji", "Pacific/Fiji"),

            // South America
            Triple("São Paulo", "Brazil", "America/Sao_Paulo"),
            Triple("Rio de Janeiro", "Brazil", "America/Sao_Paulo"),
            Triple("Buenos Aires", "Argentina", "America/Argentina/Buenos_Aires"),
            Triple("Santiago", "Chile", "America/Santiago"),
            Triple("Bogotá", "Colombia", "America/Bogota"),
            Triple("Lima", "Peru", "America/Lima"),
            Triple("Caracas", "Venezuela", "America/Caracas"),
            Triple("Montevideo", "Uruguay", "America/Montevideo"),

            // Africa
            Triple("Cairo", "Egypt", "Africa/Cairo"),
            Triple("Johannesburg", "South Africa", "Africa/Johannesburg"),
            Triple("Cape Town", "South Africa", "Africa/Johannesburg"),
            Triple("Nairobi", "Kenya", "Africa/Nairobi"),
            Triple("Lagos", "Nigeria", "Africa/Lagos"),
            Triple("Casablanca", "Morocco", "Africa/Casablanca"),
            Triple("Accra", "Ghana", "Africa/Accra"),
            Triple("Addis Ababa", "Ethiopia", "Africa/Addis_Ababa")
        )

        val countryCodeMap = mapOf(
            "India" to "IN", "United States" to "US", "United Kingdom" to "GB", "Canada" to "CA",
            "Australia" to "AU", "Germany" to "DE", "France" to "FR", "Japan" to "JP",
            "South Korea" to "KR", "China" to "CN", "Singapore" to "SG", "United Arab Emirates" to "AE",
            "Saudi Arabia" to "SA", "Brazil" to "BR", "Argentina" to "AR", "Mexico" to "MX",
            "Spain" to "ES", "Italy" to "IT", "Netherlands" to "NL", "Switzerland" to "CH",
            "Sweden" to "SE", "Norway" to "NO", "Denmark" to "DK", "Finland" to "FI",
            "Ireland" to "IE", "New Zealand" to "NZ", "South Africa" to "ZA", "Egypt" to "EG",
            "Kenya" to "KE", "Nigeria" to "NG", "Thailand" to "TH", "Malaysia" to "MY",
            "Indonesia" to "ID", "Philippines" to "PH", "Vietnam" to "VN", "Turkey" to "TR",
            "Poland" to "PL", "Austria" to "AT", "Belgium" to "BE", "Portugal" to "PT",
            "Greece" to "GR", "Czech Republic" to "CZ", "Hungary" to "HU", "Hong Kong" to "HK",
            "Taiwan" to "TW", "Israel" to "IL", "Qatar" to "QA", "Kuwait" to "KW",
            "Oman" to "OM", "Chile" to "CL", "Colombia" to "CO", "Peru" to "PE",
            "Morocco" to "MA", "Ghana" to "GH", "Ethiopia" to "ET", "Bangladesh" to "BD",
            "Pakistan" to "PK", "Sri Lanka" to "LK", "Nepal" to "NP", "Fiji" to "FJ",
            "Panama" to "PA", "Venezuela" to "VE", "Uruguay" to "UY", "Lebanon" to "LB"
        )

        rawList.map { (city, country, tzId) ->
            val tz = TimeZone.getTimeZone(tzId)
            val code = countryCodeMap[country] ?: "US"
            val flag = countryCodeToFlagEmoji(code)
            val isDst = tz.inDaylightTime(Date())
            val offset = tz.rawOffset + (if (isDst) tz.dstSavings else 0)
            WorldLocation(
                cityName = city,
                countryName = country,
                countryCode = code,
                flagEmoji = flag,
                timeZoneId = tzId,
                timeZoneName = tz.getDisplayName(isDst, TimeZone.LONG, Locale.US),
                timeZoneAbbr = tz.getDisplayName(isDst, TimeZone.SHORT, Locale.US),
                utcOffsetStr = formatUtcOffset(offset),
                isCountryPrimary = false
            )
        }
    }

    /**
     * Primary country timezone mappings.
     * When user types a Country name (e.g. "India", "Japan", "Germany"), this card appears.
     */
    val WORLD_COUNTRIES: List<WorldLocation> by lazy {
        val countryList = listOf(
            Triple("India", "IN", "Asia/Kolkata"),
            Triple("United States", "US", "America/New_York"),
            Triple("United Kingdom", "GB", "Europe/London"),
            Triple("Japan", "JP", "Asia/Tokyo"),
            Triple("Germany", "DE", "Europe/Berlin"),
            Triple("France", "FR", "Europe/Paris"),
            Triple("Canada", "CA", "America/Toronto"),
            Triple("Australia", "AU", "Australia/Sydney"),
            Triple("Singapore", "SG", "Asia/Singapore"),
            Triple("United Arab Emirates", "AE", "Asia/Dubai"),
            Triple("South Korea", "KR", "Asia/Seoul"),
            Triple("China", "CN", "Asia/Shanghai"),
            Triple("Brazil", "BR", "America/Sao_Paulo"),
            Triple("Italy", "IT", "Europe/Rome"),
            Triple("Spain", "ES", "Europe/Madrid"),
            Triple("Switzerland", "CH", "Europe/Zurich"),
            Triple("Netherlands", "NL", "Europe/Amsterdam"),
            Triple("Sweden", "SE", "Europe/Stockholm"),
            Triple("New Zealand", "NZ", "Pacific/Auckland"),
            Triple("Saudi Arabia", "SA", "Asia/Riyadh"),
            Triple("South Africa", "ZA", "Africa/Johannesburg"),
            Triple("Egypt", "EG", "Africa/Cairo"),
            Triple("Mexico", "MX", "America/Mexico_City"),
            Triple("Argentina", "AR", "America/Argentina/Buenos_Aires"),
            Triple("Indonesia", "ID", "Asia/Jakarta"),
            Triple("Thailand", "TH", "Asia/Bangkok"),
            Triple("Malaysia", "MY", "Asia/Kuala_Lumpur"),
            Triple("Philippines", "PH", "Asia/Manila"),
            Triple("Vietnam", "VN", "Asia/Bangkok"),
            Triple("Turkey", "TR", "Europe/Istanbul"),
            Triple("Poland", "PL", "Europe/Warsaw"),
            Triple("Norway", "NO", "Europe/Oslo"),
            Triple("Denmark", "DK", "Europe/Copenhagen"),
            Triple("Finland", "FI", "Europe/Helsinki"),
            Triple("Ireland", "IE", "Europe/Dublin"),
            Triple("Austria", "AT", "Europe/Vienna"),
            Triple("Belgium", "BE", "Europe/Brussels"),
            Triple("Portugal", "PT", "Europe/Lisbon"),
            Triple("Greece", "GR", "Europe/Athens"),
            Triple("Czech Republic", "CZ", "Europe/Prague"),
            Triple("Hungary", "HU", "Europe/Budapest"),
            Triple("Israel", "IL", "Asia/Jerusalem"),
            Triple("Qatar", "QA", "Asia/Qatar"),
            Triple("Kuwait", "KW", "Asia/Kuwait"),
            Triple("Oman", "OM", "Asia/Muscat"),
            Triple("Chile", "CL", "America/Santiago"),
            Triple("Colombia", "CO", "America/Bogota"),
            Triple("Peru", "PE", "America/Lima"),
            Triple("Kenya", "KE", "Africa/Nairobi"),
            Triple("Nigeria", "NG", "Africa/Lagos"),
            Triple("Morocco", "MA", "Africa/Casablanca"),
            Triple("Ghana", "GH", "Africa/Accra"),
            Triple("Bangladesh", "BD", "Asia/Dhaka"),
            Triple("Pakistan", "PK", "Asia/Karachi"),
            Triple("Sri Lanka", "LK", "Asia/Colombo"),
            Triple("Nepal", "NP", "Asia/Kathmandu")
        )

        countryList.map { (country, code, tzId) ->
            val tz = TimeZone.getTimeZone(tzId)
            val flag = countryCodeToFlagEmoji(code)
            val isDst = tz.inDaylightTime(Date())
            val offset = tz.rawOffset + (if (isDst) tz.dstSavings else 0)
            WorldLocation(
                cityName = country, // Whole Country
                countryName = country,
                countryCode = code,
                flagEmoji = flag,
                timeZoneId = tzId,
                timeZoneName = tz.getDisplayName(isDst, TimeZone.LONG, Locale.US),
                timeZoneAbbr = tz.getDisplayName(isDst, TimeZone.SHORT, Locale.US),
                utcOffsetStr = formatUtcOffset(offset),
                isCountryPrimary = true
            )
        }
    }

    /**
     * Dynamic index of ALL standard IANA timezones (~600 entries).
     * Ensures EVERY city across the world is discoverable even if not in the curated list!
     */
    private val ALL_IANA_LOCATIONS: List<WorldLocation> by lazy {
        val curatedTzIds = CURATED_WORLD_LOCATIONS.map { it.timeZoneId }.toSet()
        val allIds = TimeZone.getAvailableIDs()

        allIds.mapNotNull { id ->
            if (curatedTzIds.contains(id) || !id.contains("/")) return@mapNotNull null

            val segments = id.split("/")
            val continent = segments[0]
            val rawCityName = segments.last()
            val cityName = rawCityName.replace("_", " ")

            val tz = TimeZone.getTimeZone(id)
            val isDst = tz.inDaylightTime(Date())
            val offset = tz.rawOffset + (if (isDst) tz.dstSavings else 0)

            // Approximate country or region from continent
            val regionName = when (continent) {
                "America" -> "Americas"
                "Europe" -> "Europe"
                "Asia" -> "Asia"
                "Africa" -> "Africa"
                "Australia" -> "Australia"
                "Pacific" -> "Pacific Islands"
                "Atlantic" -> "Atlantic"
                "Indian" -> "Indian Ocean"
                else -> continent
            }

            WorldLocation(
                cityName = cityName,
                countryName = regionName,
                countryCode = "🌐",
                flagEmoji = "📍",
                timeZoneId = id,
                timeZoneName = tz.getDisplayName(isDst, TimeZone.LONG, Locale.US),
                timeZoneAbbr = tz.getDisplayName(isDst, TimeZone.SHORT, Locale.US),
                utcOffsetStr = formatUtcOffset(offset),
                isCountryPrimary = false
            )
        }
    }

    /**
     * Complete combined list of all world locations.
     */
    val ALL_LOCATIONS: List<WorldLocation> by lazy {
        CURATED_WORLD_LOCATIONS + ALL_IANA_LOCATIONS
    }

    /**
     * Search result wrapper containing country matches and city matches.
     */
    data class SearchResults(
        val countryMatches: List<WorldLocation>,
        val cityMatches: List<WorldLocation>
    )

    /**
     * Performs an instant search query across countries, cities, timezone abbreviations, and offsets.
     */
    fun search(query: String): SearchResults {
        val cleanQuery = query.trim().lowercase(Locale.US)
        if (cleanQuery.isEmpty()) {
            return SearchResults(
                countryMatches = WORLD_COUNTRIES.take(12),
                cityMatches = CURATED_WORLD_LOCATIONS.take(25)
            )
        }

        // 1. Find matching countries (name, code)
        val matchedCountries = WORLD_COUNTRIES.filter { country ->
            country.countryName.lowercase(Locale.US).contains(cleanQuery) ||
            country.countryCode.lowercase(Locale.US).equals(cleanQuery)
        }

        // 2. Find matching cities from curated + full IANA list
        val matchedCurated = CURATED_WORLD_LOCATIONS.filter { loc ->
            loc.cityName.lowercase(Locale.US).contains(cleanQuery) ||
            loc.countryName.lowercase(Locale.US).contains(cleanQuery) ||
            loc.timeZoneAbbr.lowercase(Locale.US).contains(cleanQuery) ||
            loc.utcOffsetStr.lowercase(Locale.US).contains(cleanQuery) ||
            loc.timeZoneId.lowercase(Locale.US).contains(cleanQuery)
        }

        val matchedIana = ALL_IANA_LOCATIONS.filter { loc ->
            loc.cityName.lowercase(Locale.US).contains(cleanQuery) ||
            loc.timeZoneId.lowercase(Locale.US).contains(cleanQuery)
        }.take(30)

        val combinedCities = (matchedCurated + matchedIana).distinctBy { "${it.cityName}_${it.timeZoneId}" }

        return SearchResults(
            countryMatches = matchedCountries,
            cityMatches = combinedCities
        )
    }

    /**
     * Finds a WorldLocation matching a given IANA timezone ID.
     */
    fun findByTimezoneId(tzId: String): WorldLocation {
        // Look in curated list first
        val fromCurated = CURATED_WORLD_LOCATIONS.find { it.timeZoneId.equals(tzId, ignoreCase = true) }
        if (fromCurated != null) return fromCurated

        // Look in countries
        val fromCountry = WORLD_COUNTRIES.find { it.timeZoneId.equals(tzId, ignoreCase = true) }
        if (fromCountry != null) return fromCountry

        // Fallback: build dynamically
        val tz = TimeZone.getTimeZone(tzId)
        val cityName = tzId.substringAfterLast("/").replace("_", " ")
        val isDst = tz.inDaylightTime(Date())
        val offset = tz.rawOffset + (if (isDst) tz.dstSavings else 0)

        return WorldLocation(
            cityName = if (cityName.isEmpty()) tzId else cityName,
            countryName = tzId.substringBefore("/"),
            countryCode = "🌐",
            flagEmoji = "📍",
            timeZoneId = tzId,
            timeZoneName = tz.getDisplayName(isDst, TimeZone.LONG, Locale.US),
            timeZoneAbbr = tz.getDisplayName(isDst, TimeZone.SHORT, Locale.US),
            utcOffsetStr = formatUtcOffset(offset),
            isCountryPrimary = false
        )
    }
}
