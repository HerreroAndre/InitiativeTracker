package com.dmc.initiativetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmc.initiativetracker.ui.navigation.AppNavHost
import com.dmc.initiativetracker.ui.theme.AppTheme
import com.dmc.initiativetracker.ui.theme.ThemeViewModel
import com.dmc.initiativetracker.ui.theme.ThemeViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeViewModel: ThemeViewModel = viewModel(
                factory = ThemeViewModelFactory(applicationContext)
            )
            val themeMode by themeViewModel.themeMode.collectAsState()

            AppTheme(themeMode = themeMode) {
                AppNavHost(themeViewModel = themeViewModel)
            }
        }
    }
}