package com.dmc.initiativetracker.ui.preferences

import android.content.Context
import android.content.SharedPreferences
import com.dmc.initiativetracker.ui.theme.AppThemeMode

class ThemePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(): AppThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return AppThemeMode.entries.firstOrNull { it.name == raw } ?: AppThemeMode.SYSTEM
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "initiative_tracker_preferences"
        private const val KEY_THEME_MODE = "app_theme_mode"
    }
}