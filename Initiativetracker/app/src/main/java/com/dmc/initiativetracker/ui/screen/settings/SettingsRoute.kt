package com.dmc.initiativetracker.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dmc.initiativetracker.ui.theme.ThemeViewModel

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    themeViewModel: ThemeViewModel
) {
    val themeMode by themeViewModel.themeMode.collectAsState()

    SettingsScreen(
        uiState = SettingsUiState(themeMode = themeMode),
        onBack = onBack,
        onThemeSelected = themeViewModel::setTheme
    )
}