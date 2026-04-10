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

data class CharacterItem(
    val id: Long = 0L,
    val roundId: Long = 0L,
    val playerName: String = "",
    val characterName: String = "",
    val initiative: Int = 0,
    val hp: Int? = null,
    val isPlayer: Boolean = true,
    val isActive: Boolean = true,
    val imageUri: String? = null,
    val stableUid: String = UUID.randomUUID().toString()
)