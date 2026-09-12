package com.example.todo_list.manager

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.todo_list.R

/**
 * Enumeration of available dynamic application launcher icons.
 */
enum class AppIcon(
    val id: String,
    val displayName: String,
    val aliasClassName: String,
    val subtitle: String,
    @DrawableRes val iconResId: Int
) {
    CLASSIC(
        id = "classic",
        displayName = "Classic iOS",
        aliasClassName = "com.example.todo_list.MainActivityClassic",
        subtitle = "Apple System Blue signature look",
        iconResId = R.mipmap.ic_launcher_classic
    ),
    DARK(
        id = "dark",
        displayName = "Dark Minimal",
        aliasClassName = "com.example.todo_list.MainActivityDark",
        subtitle = "Deep obsidian stealth aesthetic",
        iconResId = R.mipmap.ic_launcher_dark
    ),
    NEON(
        id = "neon",
        displayName = "Neon Blue Glow",
        aliasClassName = "com.example.todo_list.MainActivityNeon",
        subtitle = "Vibrant electric cyber glow",
        iconResId = R.mipmap.ic_launcher_neon
    ),
    GLASS(
        id = "glass",
        displayName = "Glassmorphism",
        aliasClassName = "com.example.todo_list.MainActivityGlass",
        subtitle = "Frosted glass chromatic gradient",
        iconResId = R.mipmap.ic_launcher_glass
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
     * Initializes the manager, reading stored preference or syncing with PackageManager.
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
        syncComponentStates(appContext, activeIcon)
    }

    /**
     * Dynamically switches the application launcher icon via PackageManager.
     * Uses PackageManager.DONT_KILL_APP to avoid killing the active application process.
     */
    fun setAppIcon(context: Context, newIcon: AppIcon): Boolean {
        val appContext = context.applicationContext
        try {
            val pm = appContext.packageManager

            // 1. Enable the target component first to guarantee at least one launcher entry exists
            val targetComponent = ComponentName(appContext.packageName, newIcon.aliasClassName)
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 2. Disable all other activity aliases
            AppIcon.entries.forEach { icon ->
                if (icon != newIcon) {
                    val comp = ComponentName(appContext.packageName, icon.aliasClassName)
                    pm.setComponentEnabledSetting(
                        comp,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }

            // 3. Persist selection and update reactive state
            currentAppIcon = newIcon
            prefs?.edit()?.putString(KEY_SELECTED_ICON, newIcon.id)?.apply()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun detectActiveIconFromPackageManager(context: Context): AppIcon? {
        val pm = context.packageManager
        for (icon in AppIcon.entries) {
            try {
                val comp = ComponentName(context.packageName, icon.aliasClassName)
                val state = pm.getComponentEnabledSetting(comp)
                if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    return icon
                }
            } catch (e: Exception) {
                // Ignore and proceed to next alias
            }
        }
        return null
    }

    private fun syncComponentStates(context: Context, activeIcon: AppIcon) {
        val pm = context.packageManager
        AppIcon.entries.forEach { icon ->
            val comp = ComponentName(context.packageName, icon.aliasClassName)
            val expectedState = if (icon == activeIcon) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                val currentState = pm.getComponentEnabledSetting(comp)
                if (currentState != expectedState) {
                    pm.setComponentEnabledSetting(
                        comp,
                        expectedState,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
