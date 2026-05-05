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
import com.dmc.initiativetracker.domain.model.StatusTickTiming
import java.util.UUID
import com.dmc.initiativetracker.domain.model.CharacterType

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
            val ordered = bundle.ordered.sortedByInitiativeDesc()
            val active = ordered.filter { it.isActive && !it.isDead }
            val currentId = bundle.combatState?.currentCharacterId
            val current = ordered.firstOrNull { it.id == currentId }

            CombatUiState(
                roundId = roundId,
                roundCounter = bundle.combatState?.roundCounter ?: 1,
                ordered = ordered,
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

        if (currentState == null || currentState.roundId != roundId) {
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

        val currentCharacterId = state.currentCharacterId
        val nextCharacterId = active[nextIdx].id

        if (currentCharacterId != null) {
            repo.decrementStatusesForCharacterTurn(
                characterId = currentCharacterId,
                tickTiming = StatusTickTiming.TURN_END
            )
            repo.deleteExpiredStatuses()
        }

        combatRepo.setCurrentCharacter(nextCharacterId)

        repo.decrementStatusesForCharacterTurn(
            characterId = nextCharacterId,
            tickTiming = StatusTickTiming.TURN_START
        )
        repo.deleteExpiredStatuses()

        if (wrapped) {
            combatRepo.setRoundCounter(state.roundCounter + 1)
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

    fun addCombatant(
        playerName: String,
        characterName: String,
        initiative: Double,
        currentHp: Int?,
        maxHp: Int?,
        tempHp: Int,
        type: CharacterType,
        imageUri: String?
    ) = viewModelScope.launch {
        val normalizedMaxHp = maxHp?.coerceAtLeast(0)
        val normalizedCurrentHp = when {
            currentHp == null -> null
            normalizedMaxHp != null -> currentHp.coerceIn(0, normalizedMaxHp)
            else -> currentHp.coerceAtLeast(0)
        }

        val combatant = Character(
            id = 0,
            roundId = roundId,
            playerName = playerName.trim(),
            characterName = characterName.trim(),
            initiative = initiative,
            imageUri = imageUri,
            currentHp = normalizedCurrentHp,
            maxHp = normalizedMaxHp,
            tempHp = tempHp.coerceAtLeast(0),
            isActive = true,
            type = type,
            deathSuccesses = 0,
            deathFailures = 0,
            isDead = false
        )

        repo.upsertCharacter(combatant)

        repairCurrentCharacterIfNeeded()

        _toast.value = "Combatiente agregado"
    }

    fun heal(character: Character, amount: Int) = viewModelScope.launch {
        if (amount <= 0) return@launch

        val newCurrentHp = when {
            character.currentHp == null -> null
            character.maxHp != null -> (character.currentHp + amount).coerceIn(0, character.maxHp)
            else -> (character.currentHp + amount).coerceAtLeast(0)
        }

        val revived = newCurrentHp != null && newCurrentHp > 0

        repo.upsertCharacter(
            character.copy(
                currentHp = newCurrentHp,
                deathSuccesses = if (revived) 0 else character.deathSuccesses,
                deathFailures = if (revived) 0 else character.deathFailures,
                isDead = if (revived) false else character.isDead,
                isActive = if (revived) true else character.isActive
            )
        )

        repairCurrentCharacterIfNeeded()
    }

    fun addStatus(
        characterId: Long,
        name: String,
        type: StatusType,
        durationRounds: Int,
        tickTiming: StatusTickTiming = StatusTickTiming.TURN_END,
        isConcentration: Boolean = false,
        linkedConcentrationGroupId: String? = null,
        linkedOriginCharacterId: Long? = null,
        linkedOriginLabel: String? = null
    ) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@launch
        if (durationRounds <= 0) return@launch

        val groupId = when {
            isConcentration -> UUID.randomUUID().toString()
            linkedConcentrationGroupId != null -> linkedConcentrationGroupId
            else -> null
        }

        val finalTickTiming = if (isConcentration) {
            StatusTickTiming.TURN_START
        } else {
            tickTiming
        }

        repo.addStatus(
            Status(
                id = 0,
                characterId = characterId,
                name = if (isConcentration) "Concentrando: $trimmed" else trimmed,
                type = if (isConcentration) StatusType.NEUTRAL else type,
                durationRounds = durationRounds.coerceAtLeast(1),
                tickTiming = finalTickTiming,
                originCharacterId = if (isConcentration) null else linkedOriginCharacterId,
                originLabel = if (isConcentration) null else linkedOriginLabel,
                concentrationGroupId = groupId
            )
        )

        _toast.value = if (isConcentration) {
            "Concentración agregada"
        } else {
            "Estado agregado"
        }
    }

    fun removeStatus(statusId: Long) = viewModelScope.launch {
        val statuses = statusesFlow.first()
        val status = statuses.firstOrNull { it.id == statusId }

        val groupId = status
            ?.takeIf { isConcentrationStatus(it) }
            ?.concentrationGroupId

        if (groupId != null) {
            repo.removeStatusesByConcentrationGroup(groupId)
            _toast.value = "Concentración eliminada"
        } else {
            repo.removeStatus(statusId)
            _toast.value = "Estado eliminado"
        }
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

    private fun isConcentrationStatus(status: Status): Boolean {
        return status.concentrationGroupId != null &&
                status.name.startsWith("Concentrando:")
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