package com.example.todo_list.manager

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.res.ResourcesCompat
import com.example.todo_list.R

/**
 * Enumeration of available dynamic application launcher icons.
 */
enum class AppIcon(
    val id: String,
    val displayName: String,
    val aliasClassName: String,
    val subtitle: String,
    @get:DrawableRes val iconResId: Int,
    @get:DrawableRes val backgroundResId: Int,
    @get:DrawableRes val foregroundResId: Int,
    val accentColorInt: Int
) {
    CLASSIC(
        id = "classic",
        displayName = "Classic iOS",
        aliasClassName = "com.example.todo_list.MainActivityClassic",
        subtitle = "Apple System Blue signature look",
        iconResId = R.mipmap.ic_launcher_classic,
        backgroundResId = R.drawable.ic_launcher_classic_bg,
        foregroundResId = R.drawable.ic_launcher_classic_fg,
        accentColorInt = 0xFF007AFF.toInt()
    ),
    DARK(
        id = "dark",
        displayName = "Dark Minimal",
        aliasClassName = "com.example.todo_list.MainActivityDark",
        subtitle = "Deep obsidian stealth aesthetic",
        iconResId = R.mipmap.ic_launcher_dark,
        backgroundResId = R.drawable.ic_launcher_dark_bg,
        foregroundResId = R.drawable.ic_launcher_dark_fg,
        accentColorInt = 0xFF8E8E93.toInt()
    ),
    NEON(
        id = "neon",
        displayName = "Neon Blue Glow",
        aliasClassName = "com.example.todo_list.MainActivityNeon",
        subtitle = "Vibrant electric cyber glow",
        iconResId = R.mipmap.ic_launcher_neon,
        backgroundResId = R.drawable.ic_launcher_neon_bg,
        foregroundResId = R.drawable.ic_launcher_neon_fg,
        accentColorInt = 0xFF00F0FF.toInt()
    ),
    GLASS(
        id = "glass",
        displayName = "Glassmorphism",
        aliasClassName = "com.example.todo_list.MainActivityGlass",
        subtitle = "Frosted glass chromatic gradient",
        iconResId = R.mipmap.ic_launcher_glass,
        backgroundResId = R.drawable.ic_launcher_glass_bg,
        foregroundResId = R.drawable.ic_launcher_glass_fg,
        accentColorInt = 0xFF6C5CE7.toInt()
    ),
    SUNSET(
        id = "sunset",
        displayName = "Sunset Coral",
        aliasClassName = "com.example.todo_list.MainActivitySunset",
        subtitle = "Warm California twilight gradient",
        iconResId = R.mipmap.ic_launcher_sunset,
        backgroundResId = R.drawable.ic_launcher_sunset_bg,
        foregroundResId = R.drawable.ic_launcher_sunset_fg,
        accentColorInt = 0xFFFF5E3A.toInt()
    ),
    EMERALD(
        id = "emerald",
        displayName = "Emerald Mint",
        aliasClassName = "com.example.todo_list.MainActivityEmerald",
        subtitle = "Fresh natural Apple health green",
        iconResId = R.mipmap.ic_launcher_emerald,
        backgroundResId = R.drawable.ic_launcher_emerald_bg,
        foregroundResId = R.drawable.ic_launcher_emerald_fg,
        accentColorInt = 0xFF34C759.toInt()
    ),
    PURPLE(
        id = "purple",
        displayName = "Royal Purple",
        aliasClassName = "com.example.todo_list.MainActivityPurple",
        subtitle = "Regal iOS ultraviolet gradient",
        iconResId = R.mipmap.ic_launcher_purple,
        backgroundResId = R.drawable.ic_launcher_purple_bg,
        foregroundResId = R.drawable.ic_launcher_purple_fg,
        accentColorInt = 0xFFAF52DE.toInt()
    ),
    GOLD(
        id = "gold",
        displayName = "Champagne Gold",
        aliasClassName = "com.example.todo_list.MainActivityGold",
        subtitle = "Luxe warm metallic champagne glow",
        iconResId = R.mipmap.ic_launcher_gold,
        backgroundResId = R.drawable.ic_launcher_gold_bg,
        foregroundResId = R.drawable.ic_launcher_gold_fg,
        accentColorInt = 0xFFF6D365.toInt()
    );

    companion object {
        fun fromId(id: String?): AppIcon {
            return entries.firstOrNull { 
                it.id.equals(id, ignoreCase = true) || it.displayName.equals(id, ignoreCase = true) 
            } ?: CLASSIC
        }
    }
}

/**
 * Reactive Singleton Manager for dynamic Android application launcher icons
 * using Android <activity-alias> and PackageManager component enablement.
 */
object AppIconManager {

    private const val PREFS_NAME = "taskflow_app_icon_prefs"
    private const val KEY_SELECTED_ICON = "key_selected_app_icon"

    private var prefs: SharedPreferences? = null

    // Reactive Compose state for UI observers
    var currentAppIcon by mutableStateOf(AppIcon.CLASSIC)
        private set

    /**
     * Initializes the manager by restoring the active icon preference without triggering
     * runtime PackageManager component mutations on startup, preventing process termination.
     */
    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        val savedId = sp.getString(KEY_SELECTED_ICON, null)
        val activeIcon = if (savedId != null) {
            AppIcon.fromId(savedId)
        } else {
            detectActiveIconFromPackageManager(appContext) ?: AppIcon.CLASSIC
        }

        currentAppIcon = activeIcon
        // NOTE: We deliberately do NOT call syncComponentStates() here during startup.
        // Mutating launcher component settings inside onCreate() causes Android OS to kill
        // the active foreground process to rebuild manifest entry points.
    }

    private var pendingDisableOldIcon: AppIcon? = null

    /**
     * Dynamically switches the application launcher icon via PackageManager.
     * Enables the new alias immediately and defers disabling the old active alias
     * until onAppBackgrounded() to prevent Android OS from killing the foreground process.
     */
    fun setAppIcon(context: Context, newIcon: AppIcon): Boolean {
        if (currentAppIcon == newIcon) return true
        val appContext = context.applicationContext
        try {
            val pm = appContext.packageManager

            // 1. Enable the target component first to guarantee the new launcher entry exists
            val targetComponent = ComponentName(appContext.packageName, newIcon.aliasClassName)
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 2. Track previous active icon to disable cleanly when app is backgrounded
            val oldIcon = currentAppIcon
            pendingDisableOldIcon = oldIcon

            // 3. Disable all other dormant aliases (excluding the old active one for now)
            AppIcon.entries.forEach { icon ->
                if (icon != newIcon && icon != oldIcon) {
                    val comp = ComponentName(appContext.packageName, icon.aliasClassName)
                    pm.setComponentEnabledSetting(
                        comp,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }

            // 4. Persist selection and update reactive state
            currentAppIcon = newIcon
            prefs?.edit()?.putString(KEY_SELECTED_ICON, newIcon.id)?.commit()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Called when the application transitions to the background (ON_STOP).
     * Cleans up previously active aliases safely without killing the foreground UI.
     */
    fun onAppBackgrounded(context: Context) {
        val oldToDisable = pendingDisableOldIcon ?: return
        val appContext = context.applicationContext
        try {
            val pm = appContext.packageManager
            if (oldToDisable != currentAppIcon) {
                val comp = ComponentName(appContext.packageName, oldToDisable.aliasClassName)
                pm.setComponentEnabledSetting(
                    comp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            pendingDisableOldIcon = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun detectActiveIconFromPackageManager(context: Context): AppIcon? {
        val pm = context.packageManager
        // 1. Check for explicitly enabled alias
        for (icon in AppIcon.entries) {
            try {
                val comp = ComponentName(context.packageName, icon.aliasClassName)
                val state = pm.getComponentEnabledSetting(comp)
                if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    return icon
                }
            } catch (_: Exception) {}
        }
        // 2. Check for default enabled alias (Classic)
        for (icon in AppIcon.entries) {
            try {
                val comp = ComponentName(context.packageName, icon.aliasClassName)
                val state = pm.getComponentEnabledSetting(comp)
                if (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == AppIcon.CLASSIC) {
                    return icon
                }
            } catch (_: Exception) {}
        }
        return AppIcon.CLASSIC
    }

    /**
     * Safely returns the currently active AppIcon. Reads from SharedPreferences if currentAppIcon
     * is not yet initialized (such as when called from a cold BroadcastReceiver lifecycle).
     */
    fun getActiveIcon(context: Context): AppIcon {
        val appContext = context.applicationContext
        val sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = sp.getString(KEY_SELECTED_ICON, null)
        return if (savedId != null) {
            AppIcon.fromId(savedId)
        } else {
            currentAppIcon
        }
    }

    /**
     * Composites and renders the AppIcon layers into a high-resolution Bitmap with Apple squircle
     * curvature, suitable for NotificationCompat.Builder.setLargeIcon().
     */
    fun getIconBitmap(context: Context, appIcon: AppIcon): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (72 * density).toInt().coerceAtLeast(192)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Apple squircle continuous rounded corner curve
        val cornerRadius = sizePx * 0.225f
        val clipPath = Path().apply {
            addRoundRect(
                0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                cornerRadius, cornerRadius,
                Path.Direction.CW
            )
        }
        canvas.clipPath(clipPath)

        val bgDrawable = ResourcesCompat.getDrawable(
            context.resources, appIcon.backgroundResId, context.theme
        )
        val fgDrawable = ResourcesCompat.getDrawable(
            context.resources, appIcon.foregroundResId, context.theme
        )

        bgDrawable?.let {
            it.setBounds(0, 0, sizePx, sizePx)
            it.draw(canvas)
        }
        fgDrawable?.let {
            it.setBounds(0, 0, sizePx, sizePx)
            it.draw(canvas)
        }

        return bitmap
    }
}

