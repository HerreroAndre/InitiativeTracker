package com.dmc.initiativetracker.domain.model

data class CombatState(
    val roundId: Long,
    val roundCounter: Int,
    val currentCharacterId: Long? // null si no hay personajes activos
)