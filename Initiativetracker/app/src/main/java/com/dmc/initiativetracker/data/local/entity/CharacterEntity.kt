package com.dmc.initiativetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "characters",
    indices = [Index("roundId")]
)
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roundId: Long,

    val playerName: String,
    val characterName: String,
    val initiative: Int,

    val imageUri: String?,

    val currentHp: Int?,
    val maxHp: Int?,
    val tempHp: Int,

    val isActive: Boolean,
    val type: String, // "PLAYER" | "NPC"
    val deathSuccesses: Int,
    val deathFailures: Int,
    val isDead: Boolean
)