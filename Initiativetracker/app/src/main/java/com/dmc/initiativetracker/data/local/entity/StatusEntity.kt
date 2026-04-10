package com.dmc.initiativetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "statuses",
    indices = [
        Index("characterId"),
        Index("concentrationGroupId")
    ]
)
data class StatusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long,
    val name: String,
    val type: String, // "POSITIVE" | "NEGATIVE" | "NEUTRAL"
    val durationRounds: Int,
    val originCharacterId: Long?,
    val originLabel: String?,
    val concentrationGroupId: String?
)