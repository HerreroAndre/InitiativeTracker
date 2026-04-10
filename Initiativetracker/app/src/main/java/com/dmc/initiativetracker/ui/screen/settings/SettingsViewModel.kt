package com.dmc.initiativetracker.ui.screen.settings

import androidx.lifecycle.ViewModel
import com.dmc.initiativetracker.ui.preferences.ThemePreferences
import com.dmc.initiativetracker.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = themePreferences.getThemeMode()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun selectTheme(mode: AppThemeMode) {
        themePreferences.setThemeMode(mode)
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }
}