package com.dmc.initiativetracker.repository

import com.dmc.initiativetracker.domain.model.CombatState
import kotlinx.coroutines.flow.StateFlow

interface CombatRepository {
    val state: StateFlow<CombatState?>
    fun start(roundId: Long, initialCharacterId: Long?)
    fun setCurrentCharacter(characterId: Long?)
    fun setRoundCounter(value: Int)
    fun end()
}