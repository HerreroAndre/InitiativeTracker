package com.dmc.initiativetracker.viewmodel

import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.Status
import com.dmc.initiativetracker.ui.screen.round_prep.RoundPrepSortOption
import java.util.UUID

data class RoundPrepUiState(
    val roundId: Long,
    val roundName: String = "Ronda",
    val isEditing: Boolean = false,

    val characters: List<Character> = emptyList(),
    val draft: List<Character> = emptyList(),
    val statuses: List<Status> = emptyList(),

    val sortOption: RoundPrepSortOption = RoundPrepSortOption.INITIATIVE_DESC,
    val isSortMenuOpen: Boolean = false,

    val confirmRemoveStatusId: Long? = null,

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val toast: String? = null
) {
    val shownCharacters: List<Character> =
        if (isEditing) draft else characters

    val canPlay: Boolean =
        shownCharacters.any { it.isActive && !it.isDead }
}
