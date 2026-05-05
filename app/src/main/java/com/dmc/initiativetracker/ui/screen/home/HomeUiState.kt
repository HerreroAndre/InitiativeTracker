package com.dmc.initiativetracker.ui.screen.home

data class HomeUiState(
    val isCreateDialogOpen: Boolean = false,
    val newRoundName: String = "",
    val isWorking: Boolean = false,
    val toast: String? = null
)