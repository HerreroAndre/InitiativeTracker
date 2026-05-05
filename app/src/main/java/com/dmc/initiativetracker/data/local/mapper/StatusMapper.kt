package com.dmc.initiativetracker.data.local.mapper

import com.dmc.initiativetracker.data.local.entity.StatusEntity
import com.dmc.initiativetracker.domain.model.Status
import com.dmc.initiativetracker.domain.model.StatusTickTiming
import com.dmc.initiativetracker.domain.model.StatusType

fun StatusEntity.toDomain(): Status = Status(
    id = id,
    characterId = characterId,
    name = name,
    type = runCatching { StatusType.valueOf(type) }.getOrDefault(StatusType.NEUTRAL),
    durationRounds = durationRounds,
    tickTiming = runCatching { StatusTickTiming.valueOf(tickTiming) }
        .getOrDefault(StatusTickTiming.TURN_END),
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
    tickTiming = tickTiming.name,
    originCharacterId = originCharacterId,
    originLabel = originLabel,
    concentrationGroupId = concentrationGroupId
)