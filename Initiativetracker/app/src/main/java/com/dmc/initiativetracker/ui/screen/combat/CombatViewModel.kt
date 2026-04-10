package com.dmc.initiativetracker.ui.screen.combat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.Status
import com.dmc.initiativetracker.domain.model.StatusType
import com.dmc.initiativetracker.repository.CombatRepository
import com.dmc.initiativetracker.repository.RoundRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import com.dmc.initiativetracker.domain.model.CombatState

class CombatViewModel(
    private val roundId: Long,
    private val repo: RoundRepository,
    private val combatRepo: CombatRepository
) : ViewModel() {

    private val _bottomSheet = MutableStateFlow(false)
    private val _preview = MutableStateFlow<String?>(null)
    private val _toast = MutableStateFlow<String?>(null)

    private val orderedFlow = repo.observeCharacters(roundId)
    private val statusesFlow = repo.observeStatuses(roundId)
    private val combatStateFlow = combatRepo.state

    private fun List<Character>.sortedByInitiativeDesc(): List<Character> =
        sortedByDescending { it.initiative }

    private data class CombatUiBundle(
        val ordered: List<Character>,
        val statuses: List<Status>,
        val combatState: CombatState?,
        val sheet: Boolean,
        val preview: String?,
        val toast: String?
    )

    val uiState: StateFlow<CombatUiState> =
        combine(orderedFlow, statusesFlow) { ordered, statuses ->
            ordered to statuses
        }.combine(combatStateFlow) { (ordered, statuses), combatState ->
            Triple(ordered, statuses, combatState)
        }.combine(_bottomSheet) { (ordered, statuses, combatState), sheet ->
            CombatUiBundle(
                ordered = ordered,
                statuses = statuses,
                combatState = combatState,
                sheet = sheet,
                preview = null,
                toast = null
            )
        }.combine(_preview) { bundle, preview ->
            bundle.copy(preview = preview)
        }.combine(_toast) { bundle, toast ->
            bundle.copy(toast = toast)
        }.map { bundle ->
            val active = bundle.ordered.filter { it.isActive && !it.isDead }
            val currentId = bundle.combatState?.currentCharacterId
            val current = bundle.ordered.firstOrNull { it.id == currentId }

            CombatUiState(
                roundId = roundId,
                roundCounter = bundle.combatState?.roundCounter ?: 1,
                ordered = bundle.ordered,
                activeOrdered = active,
                current = current,
                statuses = bundle.statuses,
                isBottomSheetOpen = bundle.sheet,
                previewImageUri = bundle.preview,
                toast = bundle.toast
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CombatUiState(roundId = roundId)
        )

    fun startIfNeeded() = viewModelScope.launch {
        val ordered = orderedFlow.first().sortedByInitiativeDesc()
        val active = ordered.filter { it.isActive && !it.isDead }
        val currentState = combatRepo.state.value

        if (currentState == null) {
            combatRepo.start(
                roundId = roundId,
                initialCharacterId = active.firstOrNull()?.id
            )
            return@launch
        }

        val currentStillExists = active.any { it.id == currentState.currentCharacterId }

        if (!currentStillExists) {
            combatRepo.setCurrentCharacter(active.firstOrNull()?.id)
        }
    }

    fun next() = viewModelScope.launch {
        val state = combatRepo.state.value ?: return@launch
        val ordered = orderedFlow.first().sortedByInitiativeDesc()
        val active = ordered.filter { it.isActive && !it.isDead }

        if (active.isEmpty()) {
            combatRepo.setCurrentCharacter(null)
            return@launch
        }

        val idx = active.indexOfFirst { it.id == state.currentCharacterId }
        val nextIdx = if (idx == -1) 0 else (idx + 1) % active.size
        val wrapped = idx != -1 && nextIdx == 0

        combatRepo.setCurrentCharacter(active[nextIdx].id)

        if (wrapped) {
            combatRepo.setRoundCounter(state.roundCounter + 1)
            repo.decrementStatusDurations()
            repo.deleteExpiredStatuses()
        }
    }

    fun prev() = viewModelScope.launch {
        val state = combatRepo.state.value ?: return@launch
        val ordered = orderedFlow.first().sortedByInitiativeDesc()
        val active = ordered.filter { it.isActive && !it.isDead }

        if (active.isEmpty()) {
            combatRepo.setCurrentCharacter(null)
            return@launch
        }

        val idx = active.indexOfFirst { it.id == state.currentCharacterId }

        if (idx == -1) {
            combatRepo.setCurrentCharacter(active.last().id)
            return@launch
        }

        val wrapped = idx == 0

        if (wrapped && state.roundCounter <= 1) {
            return@launch
        }

        val prevIdx = if (wrapped) active.lastIndex else idx - 1

        combatRepo.setCurrentCharacter(active[prevIdx].id)

        if (wrapped) {
            combatRepo.setRoundCounter(state.roundCounter - 1)
        }
    }

    fun toggleActive(character: Character) = viewModelScope.launch {
        repo.upsertCharacter(
            character.copy(isActive = !character.isActive)
        )
        repairCurrentCharacterIfNeeded()
    }

    fun setHp(character: Character, currentHp: Int?) = viewModelScope.launch {
        val normalizedHp = when {
            currentHp == null -> null
            character.maxHp != null -> currentHp.coerceIn(0, character.maxHp)
            else -> currentHp.coerceAtLeast(0)
        }

        val revived = normalizedHp != null && normalizedHp > 0

        repo.upsertCharacter(
            character.copy(
                currentHp = normalizedHp,
                deathSuccesses = if (revived) 0 else character.deathSuccesses,
                deathFailures = if (revived) 0 else character.deathFailures,
                isDead = if (revived) false else character.isDead,
                isActive = if (revived) true else character.isActive
            )
        )
        repairCurrentCharacterIfNeeded()
    }

    fun setTempHp(character: Character, tempHp: Int) = viewModelScope.launch {
        repo.upsertCharacter(
            character.copy(
                tempHp = tempHp.coerceAtLeast(0)
            )
        )
    }

    fun applyDamage(character: Character, damage: Int) = viewModelScope.launch {
        if (damage <= 0) return@launch

        val currentTempHp = character.tempHp.coerceAtLeast(0)
        val remainingDamage = (damage - currentTempHp).coerceAtLeast(0)
        val newTempHp = (currentTempHp - damage).coerceAtLeast(0)

        val newCurrentHp = if (character.currentHp == null) {
            null
        } else {
            (character.currentHp - remainingDamage).coerceAtLeast(0)
        }

        repo.upsertCharacter(
            character.copy(
                currentHp = newCurrentHp,
                tempHp = newTempHp
            )
        )
    }

    fun addStatus(
        characterId: Long,
        name: String,
        type: StatusType,
        durationRounds: Int,
        originCharacterId: Long? = null,
        originLabel: String? = null,
        concentrationGroupId: String? = null
    ) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@launch
        if (durationRounds <= 0) return@launch

        repo.addStatus(
            Status(
                id = 0,
                characterId = characterId,
                name = trimmed,
                type = type,
                durationRounds = durationRounds,
                originCharacterId = originCharacterId,
                originLabel = originLabel,
                concentrationGroupId = concentrationGroupId
            )
        )

        _toast.value = "Estado agregado"
    }

    fun removeStatus(statusId: Long) = viewModelScope.launch {
        repo.removeStatus(statusId)
        _toast.value = "Estado eliminado"
    }

    fun endConcentration(groupId: String) = viewModelScope.launch {
        repo.removeStatusesByConcentrationGroup(groupId)
    }

    fun addDeathSuccess(character: Character) = viewModelScope.launch {
        if (character.deathSuccesses >= 3 || character.deathFailures >= 3) return@launch

        repo.upsertCharacter(
            character.copy(
                deathSuccesses = (character.deathSuccesses + 1).coerceAtMost(3)
            )
        )
    }

    fun addDeathFailure(character: Character) = viewModelScope.launch {
        if (character.deathSuccesses >= 3 || character.deathFailures >= 3) return@launch

        val newFails = (character.deathFailures + 1).coerceAtMost(3)
        val dead = newFails >= 3

        repo.upsertCharacter(
            character.copy(
                deathFailures = newFails,
                isDead = dead || character.isDead,
                isActive = if (dead) false else character.isActive
            )
        )
        repairCurrentCharacterIfNeeded()
    }

    fun openSheet() {
        _bottomSheet.value = true
    }

    fun closeSheet() {
        _bottomSheet.value = false
    }

    fun openPreview(uri: String?) {
        _preview.value = uri
    }

    fun closePreview() {
        _preview.value = null
    }

    fun endCombat() {
        combatRepo.end()
        _toast.value = "Combate terminado"
    }

    fun consumeToast() {
        _toast.value = null
    }

    private suspend fun repairCurrentCharacterIfNeeded() {
        val state = combatRepo.state.value ?: return
        val ordered = orderedFlow.first().sortedByInitiativeDesc()
        val active = ordered.filter { it.isActive && !it.isDead }

        if (active.isEmpty()) {
            combatRepo.setCurrentCharacter(null)
            return
        }

        val currentId = state.currentCharacterId
        val existsInOrdered = ordered.any { it.id == currentId }

        if (currentId == null || !existsInOrdered) {
            combatRepo.setCurrentCharacter(active.first().id)
        }
    }
}