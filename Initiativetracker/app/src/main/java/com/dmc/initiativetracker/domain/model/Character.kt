package com.dmc.initiativetracker.domain.model

data class Character(
    val id: Long,
    val roundId: Long,
    val playerName: String,
    val characterName: String,
    val initiative: Double,
    val imageUri: String?,

    val currentHp: Int?,   // null => "?"
    val maxHp: Int?,       // null => desconocido
    val tempHp: Int,       // 0 si no tiene

    val isActive: Boolean,
    val type: CharacterType,
    val deathSuccesses: Int,
    val deathFailures: Int,
    val isDead: Boolean
)