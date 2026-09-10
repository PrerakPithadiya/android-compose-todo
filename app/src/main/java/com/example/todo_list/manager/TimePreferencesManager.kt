package com.example.todo_list.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import com.example.todo_list.MainActivity
import com.example.todo_list.data.repository.WorldTimezoneRepository
import com.example.todo_list.model.WorldLocation
import com.example.todo_list.utils.TimeFormatHelper
import java.util.Locale
import java.util.TimeZone

/**
 * Reactive Singleton Manager for user Time Zone, Regional Location,
 * and 12-Hour vs 24-Hour Time Formatting preferences.
 */
object TimePreferencesManager {

    private const val PREFS_NAME = "taskflow_time_prefs"
    private const val KEY_IS_24_HOUR = "key_is_24_hour_format"
    private const val KEY_AUTO_DETECT = "key_auto_detect_timezone"
    private const val KEY_TZ_ID = "key_selected_tz_id"
    private const val KEY_CITY_NAME = "key_selected_city_name"
    private const val KEY_COUNTRY_NAME = "key_selected_country_name"
    private const val KEY_COUNTRY_CODE = "key_selected_country_code"
    private const val KEY_FLAG_EMOJI = "key_selected_flag_emoji"
    private const val KEY_LAST_NOTIFIED_TZ = "key_last_notified_tz"

    const val TIMEZONE_NOTIFICATION_CHANNEL_ID = "taskflow_timezone_channel"
    private const val NOTIFICATION_ID = 202611

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    // Reactive Compose States
    var is24HourFormat by mutableStateOf(false)
        private set

    var isAutoDetectEnabled by mutableStateOf(true)
        private set

    var selectedLocation by mutableStateOf(
        WorldLocation(
            cityName = "New Delhi",
            countryName = "India",
            countryCode = "IN",
            flagEmoji = "🇮🇳",
            timeZoneId = "Asia/Kolkata",
            timeZoneName = "India Standard Time",
            timeZoneAbbr = "IST",
            utcOffsetStr = "UTC+05:30",
            isCountryPrimary = false
        )
    )
        private set

    var showDetectionBanner by mutableStateOf(false)
        private set

    var detectedLocation by mutableStateOf<WorldLocation?>(null)
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        is24HourFormat = sp.getBoolean(KEY_IS_24_HOUR, false)
        isAutoDetectEnabled = sp.getBoolean(KEY_AUTO_DETECT, true)

        val savedTzId = sp.getString(KEY_TZ_ID, null)
        if (savedTzId != null) {
            val city = sp.getString(KEY_CITY_NAME, "New Delhi") ?: "New Delhi"
            val country = sp.getString(KEY_COUNTRY_NAME, "India") ?: "India"
            val code = sp.getString(KEY_COUNTRY_CODE, "IN") ?: "IN"
            val flag = sp.getString(KEY_FLAG_EMOJI, "🇮🇳") ?: "🇮🇳"
            val loc = WorldTimezoneRepository.findByTimezoneId(savedTzId).copy(
                cityName = city,
                countryName = country,
                countryCode = code,
                flagEmoji = flag
            )
            selectedLocation = loc
        } else {
            // Fallback to device default
            val defaultTz = TimeZone.getDefault()
            selectedLocation = WorldTimezoneRepository.findByTimezoneId(defaultTz.id)
        }

        createNotificationChannel(context)
        detectUserTimezone(context, forceNotify = false)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Time Zone & Location Alerts"
            val desc = "Alerts when your location or standard time zone changes"
            val channel = NotificationChannel(
                TIMEZONE_NOTIFICATION_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = desc
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    /**
     * Automatically detects the user's system time zone, city, and country.
     * Triggers notification if new or changed.
     */
    fun detectUserTimezone(context: Context, forceNotify: Boolean = false) {
        val defaultTz = TimeZone.getDefault()
        val defaultLocale = Locale.getDefault()
        val detectedTzId = defaultTz.id

        var loc = WorldTimezoneRepository.findByTimezoneId(detectedTzId)

        // Enhance country/flag if available from system locale
        val localeCountry = defaultLocale.displayCountry
        val localeCountryCode = defaultLocale.country
        if (localeCountry.isNotBlank() && localeCountryCode.length == 2) {
            val flag = WorldTimezoneRepository.countryCodeToFlagEmoji(localeCountryCode)
            loc = loc.copy(
                countryName = localeCountry,
                countryCode = localeCountryCode,
                flagEmoji = flag
            )
        }

        val lastNotified = prefs?.getString(KEY_LAST_NOTIFIED_TZ, null)
        val isFirstDetection = lastNotified == null
        val hasChanged = lastNotified != detectedTzId

        if (isAutoDetectEnabled) {
            selectedLocation = loc
            persistLocation(loc)
        }

        if (isFirstDetection || hasChanged || forceNotify) {
            detectedLocation = loc
            showDetectionBanner = true
            prefs?.edit()?.putString(KEY_LAST_NOTIFIED_TZ, detectedTzId)?.apply()

            // Post system notification
            postTimezoneSystemNotification(context, loc)
        }
    }

    private fun postTimezoneSystemNotification(context: Context, location: WorldLocation) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, TIMEZONE_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                .setContentTitle("🌍 Time Zone Detected: ${location.cityName}, ${location.countryName}")
                .setContentText("Tasks and reminders synchronized to ${location.timeZoneAbbr} (${location.utcOffsetStr})")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            // Ignored if permissions not granted or failed
        }
    }

    fun setTimeFormat(is24Hour: Boolean) {
        is24HourFormat = is24Hour
        prefs?.edit()?.putBoolean(KEY_IS_24_HOUR, is24Hour)?.apply()
    }

    fun setAutoDetectEnabled(enabled: Boolean, context: Context) {
        isAutoDetectEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_AUTO_DETECT, enabled)?.apply()
        if (enabled) {
            detectUserTimezone(context, forceNotify = true)
        }
    }

    fun selectLocation(location: WorldLocation) {
        selectedLocation = location
        persistLocation(location)
        showDetectionBanner = false
    }

    private fun persistLocation(location: WorldLocation) {
        prefs?.edit()?.apply {
            putString(KEY_TZ_ID, location.timeZoneId)
            putString(KEY_CITY_NAME, location.cityName)
            putString(KEY_COUNTRY_NAME, location.countryName)
            putString(KEY_COUNTRY_CODE, location.countryCode)
            putString(KEY_FLAG_EMOJI, location.flagEmoji)
            apply()
        }
    }

    fun dismissBanner() {
        showDetectionBanner = false
    }

    /**
     * Formats the current live time in the active timezone according to format preference.
     */
    fun getLiveLocalTime(): String {
        return TimeFormatHelper.getFormattedCurrentTime(selectedLocation.timeZoneId, is24HourFormat)
    }
}
