package com.dmc.initiativetracker.repository

import com.dmc.initiativetracker.domain.model.CombatState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CombatRepositoryImpl : CombatRepository {
    private val _state = MutableStateFlow<CombatState?>(null)
    override val state: StateFlow<CombatState?> = _state

    override fun start(roundId: Long, initialCharacterId: Long?) {
        _state.value = CombatState(roundId, roundCounter = 1, currentCharacterId = initialCharacterId)
    }

    override fun setCurrentCharacter(characterId: Long?) {
        _state.value = _state.value?.copy(currentCharacterId = characterId)
    }

    override fun setRoundCounter(value: Int) {
        _state.value = _state.value?.copy(roundCounter = value.coerceAtLeast(1))
    }

    override fun end() { _state.value = null }
}