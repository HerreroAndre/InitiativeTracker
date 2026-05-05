package com.dmc.initiativetracker.ui.screen.round_prep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.CharacterType
import com.dmc.initiativetracker.domain.model.Status
import com.dmc.initiativetracker.domain.model.StatusTickTiming
import com.dmc.initiativetracker.domain.model.StatusType
import com.dmc.initiativetracker.repository.RoundRepository
import com.dmc.initiativetracker.ui.preferences.SortPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

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
    private val tempStatuses = MutableStateFlow<List<Status>>(emptyList())
    private val pendingRemovedStatusIds = MutableStateFlow<Set<Long>>(emptySet())
    private val roundNameFlow = repo.observeRoundName(roundId)
    private val charactersFlow = repo.observeCharacters(roundId)
    private val statusesFlow = repo.observeStatuses(roundId)

    private data class RoundData(
        val roundName: String,
        val characters: List<Character>,
        val statuses: List<Status>
    )

    private data class EditData(
        val isEditing: Boolean,
        val draft: List<Character>,
        val tempStatuses: List<Status>,
        val pendingRemovedStatusIds: Set<Long>,
        val confirmRemoveStatusId: Long?
    )

    private data class UiFlags(
        val isSaving: Boolean,
        val toast: String?,
        val sortOption: RoundPrepSortOption,
        val isSortMenuOpen: Boolean
    )

    private val roundDataFlow: Flow<RoundData> =
        combine(
            roundNameFlow,
            charactersFlow,
            statusesFlow
        ) { roundName, characters, statuses ->
            RoundData(
                roundName = roundName,
                characters = characters,
                statuses = statuses
            )
        }

    private val editDataFlow: Flow<EditData> =
        combine(
            isEditing,
            draft,
            tempStatuses,
            pendingRemovedStatusIds,
            confirmRemoveStatusId
        ) { editing, draftList, temporaryStatuses, removedStatusIds, pendingRemoveStatusId ->
            EditData(
                isEditing = editing,
                draft = draftList,
                tempStatuses = temporaryStatuses,
                pendingRemovedStatusIds = removedStatusIds,
                confirmRemoveStatusId = pendingRemoveStatusId
            )
        }

    private val uiFlagsFlow: Flow<UiFlags> =
        combine(
            isSaving,
            toast,
            sortOption,
            isSortMenuOpen
        ) { saving, toastMessage, selectedSort, sortMenuOpen ->
            UiFlags(
                isSaving = saving,
                toast = toastMessage,
                sortOption = selectedSort,
                isSortMenuOpen = sortMenuOpen
            )
        }

    private val baseState: Flow<RoundPrepUiState> =
        combine(
            roundDataFlow,
            editDataFlow,
            uiFlagsFlow
        ) { roundData, editData, uiFlags ->

            val safeCharacters = sortCharacters(
                list = roundData.characters,
                option = uiFlags.sortOption
            )

            RoundPrepUiState(
                roundId = roundId,
                roundName = roundData.roundName,
                isEditing = editData.isEditing,
                characters = safeCharacters,
                draft = editData.draft,
                statuses = if (editData.isEditing) {
                    roundData.statuses.filterNot { it.id in editData.pendingRemovedStatusIds } +
                            editData.tempStatuses
                } else {
                    roundData.statuses
                },
                sortOption = uiFlags.sortOption,
                isSortMenuOpen = uiFlags.isSortMenuOpen,
                confirmRemoveStatusId = editData.confirmRemoveStatusId,
                isSaving = uiFlags.isSaving,
                errorMessage = null,
                toast = uiFlags.toast
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
        tempStatuses.value = emptyList()
        pendingRemovedStatusIds.value = emptySet()
    }

    fun cancelEdit() {
        isEditing.value = false
        draft.value = emptyList()
        error.value = null
        confirmRemoveStatusId.value = null
        tempStatuses.value = emptyList()
        pendingRemovedStatusIds.value = emptySet()
    }

    fun addCharacterToDraft() {
        val tempId = -(System.nanoTime())
        val newChar = Character(
            id = tempId,
            roundId = roundId,
            playerName = "",
            characterName = "",
            initiative = 10.0,
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

        if (characterId < 0) {
            tempStatuses.update { list ->
                list.filterNot { it.characterId == characterId }
            }
        }
    }

    fun confirmEdit() {
        if (!isEditing.value) return

        viewModelScope.launch {
            try {
                isSaving.value = true
                error.value = null

                val normalizedDraft = draft.value
                    .map { c ->
                        val normalizedId = c.id
                        val normalizedPlayerName = c.playerName.trim()
                        val normalizedCharacterName = c.characterName.trim()
                        val normalizedInitiative = if (c.initiative <= 0) 1.0 else c.initiative
                        val normalizedMaxHp = c.maxHp?.coerceAtLeast(0)
                        val normalizedTempHp = c.tempHp.coerceAtLeast(0)

                        val normalizedCurrentHp = when {
                            c.currentHp == null -> null
                            normalizedMaxHp != null -> c.currentHp.coerceIn(0, normalizedMaxHp)
                            else -> c.currentHp.coerceAtLeast(0)
                        }

                        val revived = normalizedCurrentHp != null && normalizedCurrentHp > 0

                        c.copy(
                            id = normalizedId,
                            playerName = normalizedPlayerName,
                            characterName = normalizedCharacterName,
                            initiative = normalizedInitiative,
                            currentHp = normalizedCurrentHp,
                            maxHp = normalizedMaxHp,
                            tempHp = normalizedTempHp,
                            deathSuccesses = if (revived) 0 else c.deathSuccesses,
                            deathFailures = if (revived) 0 else c.deathFailures,
                            isDead = if (revived) false else c.isDead,
                            isActive = if (revived) true else c.isActive
                        )
                    }
                val tempIdToRealId = repo.commitCharacterDraft(roundId, normalizedDraft)

                pendingRemovedStatusIds.value.forEach { statusId ->
                    repo.removeStatus(statusId)
                }

                val keptExistingCharacterIds = normalizedDraft
                    .mapNotNull { if (it.id > 0) it.id else null }
                    .toSet()

                val statusesToInsert = tempStatuses.value.mapNotNull { status ->
                    val realCharacterId = if (status.characterId < 0) {
                        tempIdToRealId[status.characterId]
                    } else {
                        status.characterId.takeIf { it in keptExistingCharacterIds }
                    }

                    realCharacterId?.let {
                        status.copy(
                            id = 0,
                            characterId = it
                        )
                    }
                }

                statusesToInsert.forEach { status ->
                    repo.addStatus(status)
                }

                isEditing.value = false
                draft.value = emptyList()
                confirmRemoveStatusId.value = null
                tempStatuses.value = emptyList()
                pendingRemovedStatusIds.value = emptySet()
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
        durationRounds: Int,
        isConcentration: Boolean = false,
        linkedConcentrationGroupId: String? = null,
        tickTiming: StatusTickTiming = StatusTickTiming.TURN_END,
        linkedOriginCharacterId: Long? = null,
        linkedOriginLabel: String? = null
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

        val safeDuration = durationRounds.coerceAtLeast(1)

        val concentrationGroupId = when {
            isConcentration -> UUID.randomUUID().toString()
            linkedConcentrationGroupId != null -> linkedConcentrationGroupId
            else -> null
        }

        val finalTickTiming = if (isConcentration) {
            StatusTickTiming.TURN_START
        } else {
            tickTiming
        }

        val status = Status(
            id = -System.nanoTime(),
            characterId = characterId,
            name = if (isConcentration) {
                "Concentrando: $cleanName"
            } else {
                cleanName
            },
            type = if (isConcentration) StatusType.NEUTRAL else type,
            durationRounds = safeDuration,
            originCharacterId = if (isConcentration) null else linkedOriginCharacterId,
            tickTiming = finalTickTiming,
            originLabel = if (isConcentration) null else linkedOriginLabel,
            concentrationGroupId = concentrationGroupId
        )

        tempStatuses.update { it + status }

        toast.value = if (isConcentration) {
            "Concentración agregada"
        } else {
            "Estado agregado"
        }
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

        val visibleStatuses = uiState.value.statuses
        val statusToRemove = visibleStatuses.firstOrNull { it.id == statusId }

        val concentrationGroupToRemove = statusToRemove
            ?.takeIf { isConcentrationStatus(it) }
            ?.concentrationGroupId

        if (concentrationGroupToRemove != null) {
            tempStatuses.update { list ->
                list.filterNot { it.concentrationGroupId == concentrationGroupToRemove }
            }

            val existingStatusIdsInGroup = visibleStatuses
                .filter { it.id > 0 && it.concentrationGroupId == concentrationGroupToRemove }
                .map { it.id }
                .toSet()

            pendingRemovedStatusIds.update { ids ->
                ids + existingStatusIdsInGroup
            }

            toast.value = "Concentración eliminada"
        } else {
            if (statusId < 0) {
                tempStatuses.update { list ->
                    list.filterNot { it.id == statusId }
                }
            } else {
                pendingRemovedStatusIds.update { ids ->
                    ids + statusId
                }
            }

            toast.value = "Estado eliminado"
        }

        confirmRemoveStatusId.value = null
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
    private fun isConcentrationStatus(status: Status): Boolean {
        return status.concentrationGroupId != null &&
                status.name.startsWith("Concentrando:")
    }
}