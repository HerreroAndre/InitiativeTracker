package com.dmc.initiativetracker.ui.screen.settings

import androidx.lifecycle.ViewModel
import com.dmc.initiativetracker.ui.preferences.ThemePreferences
import com.dmc.initiativetracker.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _themeMode = MutableStateFlow(themePreferences.getThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setTheme(mode: AppThemeMode) {
        themePreferences.setThemeMode(mode)
        _themeMode.value = mode
    }
}