package com.dmc.initiativetracker.ui.screen.round_selector

import com.dmc.initiativetracker.domain.model.RoundListItem

data class RoundSelectorUiState(
    val rounds: List<RoundListItem> = emptyList(),
    val sortOption: RoundSortOption = RoundSortOption.CREATED_DESC,
    val isSortMenuOpen: Boolean = false,
    val confirmDeleteRoundId: Long? = null,
    val isWorking: Boolean = false,
    val toast: String? = null
)