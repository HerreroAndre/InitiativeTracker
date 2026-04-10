package com.dmc.initiativetracker.domain.model

data class Status(
    val id: Long,
    val characterId: Long,
    val name: String,
    val type: StatusType,
    val durationRounds: Int,
    val originCharacterId: Long?,
    val originLabel: String?,
    val concentrationGroupId: String?
)