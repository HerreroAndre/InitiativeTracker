package com.dmc.initiativetracker.ui.screen.round_prep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.CharacterType
import com.dmc.initiativetracker.domain.model.Status
import com.dmc.initiativetracker.domain.model.StatusType
import com.dmc.initiativetracker.repository.RoundRepository
import com.dmc.initiativetracker.ui.preferences.SortPreferences
import com.dmc.initiativetracker.viewmodel.RoundPrepUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RoundPrepViewModel(
    private val roundId: Long,
    private val repo: RoundRepository,
    private val sortPreferences: SortPreferences
) : ViewModel() {

    private val isEditing = MutableStateFlow(false)
    private val draft = MutableStateFlow<List<Character>>(emptyList())
    private val isSaving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val toast = MutableStateFlow<String?>(null)
    private val sortOption = MutableStateFlow(sortPreferences.getRoundPrepSort())
    private val isSortMenuOpen = MutableStateFlow(false)
    private val confirmRemoveStatusId = MutableStateFlow<Long?>(null)

    private val roundNameFlow = repo.observeRoundName(roundId)
    private val charactersFlow = repo.observeCharacters(roundId)
    private val statusesFlow = repo.observeStatuses(roundId)

    private val baseState: Flow<RoundPrepUiState> =
        combine(
            roundNameFlow,
            charactersFlow,
            statusesFlow,
            isEditing,
            draft,
            isSaving,
            toast,
            sortOption,
            isSortMenuOpen,
            confirmRemoveStatusId
        ) { arr ->
            val roundName = arr[0] as String
            val characters = arr[1] as List<Character>
            val statuses = arr[2] as List<Status>
            val editing = arr[3] as Boolean
            val draftList = arr[4] as List<Character>
            val saving = arr[5] as Boolean
            val toastMessage = arr[6] as String?
            val selectedSort = arr[7] as RoundPrepSortOption
            val sortMenuOpen = arr[8] as Boolean
            val pendingRemoveStatusId = arr[9] as Long?

            val safeCharacters = sortCharacters(characters, selectedSort)
            val safeDraft = sortCharacters(draftList, selectedSort)

            RoundPrepUiState(
                roundId = roundId,
                roundName = roundName,
                isEditing = editing,
                characters = safeCharacters,
                draft = safeDraft,
                statuses = statuses,
                sortOption = selectedSort,
                isSortMenuOpen = sortMenuOpen,
                confirmRemoveStatusId = pendingRemoveStatusId,
                isSaving = saving,
                errorMessage = null,
                toast = toastMessage
            )
        }

    val uiState: StateFlow<RoundPrepUiState> =
        combine(baseState, error) { state, err ->
            state.copy(errorMessage = err)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            RoundPrepUiState(
                roundId = roundId,
                sortOption = sortPreferences.getRoundPrepSort()
            )
        )

    fun openSortMenu() {
        isSortMenuOpen.value = true
    }

    fun closeSortMenu() {
        isSortMenuOpen.value = false
    }

    fun selectSort(option: RoundPrepSortOption) {
        sortOption.value = option
        sortPreferences.setRoundPrepSort(option)
        isSortMenuOpen.value = false
    }

    fun enterEditMode() {
        val snapshot = uiState.value.characters
        draft.value = snapshot
        isEditing.value = true
        error.value = null
        confirmRemoveStatusId.value = null
    }

    fun cancelEdit() {
        isEditing.value = false
        draft.value = emptyList()
        error.value = null
        confirmRemoveStatusId.value = null
    }

    fun addCharacterToDraft() {
        val tempId = -(System.nanoTime())
        val newChar = Character(
            id = tempId,
            roundId = roundId,
            playerName = "",
            characterName = "",
            initiative = 10,
            imageUri = null,
            currentHp = null,
            maxHp = null,
            tempHp = 0,
            isActive = true,
            type = CharacterType.NPC,
            deathSuccesses = 0,
            deathFailures = 0,
            isDead = false
        )
        error.value = null
        draft.update { it + newChar }
    }

    fun updateDraftCharacter(updated: Character) {
        error.value = null
        draft.update { list -> list.map { if (it.id == updated.id) updated else it } }
    }

    fun removeDraftCharacter(characterId: Long) {
        error.value = null
        draft.update { it.filterNot { c -> c.id == characterId } }
    }

    fun confirmEdit() {
        if (!isEditing.value) return

        viewModelScope.launch {
            try {
                isSaving.value = true
                error.value = null

                val normalizedDraft = draft.value
                    .map { c ->
                        val normalizedId = if (c.id < 0) 0 else c.id
                        val normalizedPlayerName = c.playerName.trim()
                        val normalizedCharacterName = c.characterName.trim()
                        val normalizedInitiative = if (c.initiative <= 0) 1 else c.initiative
                        val normalizedMaxHp = c.maxHp?.coerceAtLeast(0)
                        val normalizedTempHp = c.tempHp.coerceAtLeast(0)

                        val normalizedCurrentHp = when {
                            c.currentHp == null -> null
                            normalizedMaxHp != null -> c.currentHp.coerceIn(0, normalizedMaxHp)
                            else -> c.currentHp.coerceAtLeast(0)
                        }

                        c.copy(
                            id = normalizedId,
                            playerName = normalizedPlayerName,
                            characterName = normalizedCharacterName,
                            initiative = normalizedInitiative,
                            currentHp = normalizedCurrentHp,
                            maxHp = normalizedMaxHp,
                            tempHp = normalizedTempHp
                        )
                    }
                    .sortedByDescending { it.initiative }

                repo.commitCharacterDraft(roundId, normalizedDraft)

                isEditing.value = false
                draft.value = emptyList()
                confirmRemoveStatusId.value = null
                toast.value = "Ronda guardada"
            } catch (t: Throwable) {
                error.value = t.message ?: "Error guardando"
            } finally {
                isSaving.value = false
            }
        }
    }

    fun renameRound(newName: String) = viewModelScope.launch {
        val name = newName.trim()
        if (name.isBlank()) {
            error.value = "El nombre no puede estar vacío"
            return@launch
        }

        try {
            isSaving.value = true
            repo.renameRound(roundId, name)
            toast.value = "Ronda renombrada"
        } catch (t: Throwable) {
            error.value = t.message ?: "Error renombrando"
        } finally {
            isSaving.value = false
        }
    }

    fun addPreCombatStatus(
        characterId: Long,
        name: String,
        type: StatusType,
        durationRounds: Int
    ) = viewModelScope.launch {
        if (!isEditing.value) {
            error.value = "Entrá en edición para modificar estados"
            return@launch
        }

        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            error.value = "El estado necesita nombre"
            return@launch
        }

        repo.addStatus(
            Status(
                id = 0,
                characterId = characterId,
                name = cleanName,
                type = type,
                durationRounds = durationRounds.coerceAtLeast(1),
                originCharacterId = null,
                originLabel = null,
                concentrationGroupId = null
            )
        )

        toast.value = "Estado agregado"
    }

    fun requestRemovePreCombatStatus(statusId: Long) {
        if (!isEditing.value) {
            error.value = "Entrá en edición para modificar estados"
            return
        }
        confirmRemoveStatusId.value = statusId
    }

    fun cancelRemovePreCombatStatus() {
        confirmRemoveStatusId.value = null
    }

    fun confirmRemovePreCombatStatus() = viewModelScope.launch {
        val statusId = confirmRemoveStatusId.value ?: return@launch

        if (!isEditing.value) {
            confirmRemoveStatusId.value = null
            error.value = "Entrá en edición para modificar estados"
            return@launch
        }

        repo.removeStatus(statusId)
        confirmRemoveStatusId.value = null
        toast.value = "Estado eliminado"
    }

    fun clearError() {
        error.value = null
    }

    fun consumeToast() {
        toast.value = null
    }

    private fun sortCharacters(
        list: List<Character>,
        option: RoundPrepSortOption
    ): List<Character> {
        return when (option) {
            RoundPrepSortOption.INITIATIVE_DESC -> list.sortedByDescending { it.initiative }
            RoundPrepSortOption.INITIATIVE_ASC -> list.sortedBy { it.initiative }
            RoundPrepSortOption.CHARACTER_NAME_ASC -> list.sortedBy { it.characterName.lowercase() }
            RoundPrepSortOption.CHARACTER_NAME_DESC -> list.sortedByDescending { it.characterName.lowercase() }
            RoundPrepSortOption.PLAYER_NAME_ASC -> list.sortedBy { it.playerName.lowercase() }
            RoundPrepSortOption.PLAYER_NAME_DESC -> list.sortedByDescending { it.playerName.lowercase() }
            RoundPrepSortOption.HP_DESC -> list.sortedByDescending { it.currentHp ?: Int.MIN_VALUE }
            RoundPrepSortOption.HP_ASC -> list.sortedBy { it.currentHp ?: Int.MAX_VALUE }
        }
    }
}