package com.dmc.initiativetracker.data.local.mapper

import com.dmc.initiativetracker.data.local.entity.StatusEntity
import com.dmc.initiativetracker.domain.model.Status
import com.dmc.initiativetracker.domain.model.StatusType

fun StatusEntity.toDomain(): Status = Status(
    id = id,
    characterId = characterId,
    name = name,
    type = StatusType.valueOf(type),
    durationRounds = durationRounds,
    originCharacterId = originCharacterId,
    originLabel = originLabel,
    concentrationGroupId = concentrationGroupId
)

fun Status.toEntity(): StatusEntity = StatusEntity(
    id = id,
    characterId = characterId,
    name = name,
    type = type.name,
    durationRounds = durationRounds,
    originCharacterId = originCharacterId,
    originLabel = originLabel,
    concentrationGroupId = concentrationGroupId
)