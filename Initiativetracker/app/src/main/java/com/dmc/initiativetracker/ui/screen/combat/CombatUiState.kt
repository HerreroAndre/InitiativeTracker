package com.dmc.initiativetracker.ui.screen.combat

import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.Status

data class CombatUiState(
    val roundId: Long = 0,
    val roundCounter: Int = 1,
    val ordered: List<Character> = emptyList(),
    val activeOrdered: List<Character> = emptyList(),
    val current: Character? = null,
    val statuses: List<Status> = emptyList(),
    val isBottomSheetOpen: Boolean = false,
    val previewImageUri: String? = null,
    val toast: String? = null
)