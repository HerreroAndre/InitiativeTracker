package com.dmc.initiativetracker.data.local.mapper

import com.dmc.initiativetracker.data.local.entity.CharacterEntity
import com.dmc.initiativetracker.data.local.entity.RoundEntity
import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.CharacterType
import com.dmc.initiativetracker.domain.model.Round

fun RoundEntity.toDomain() = Round(id, name, createdAt)
fun Round.toEntity() = RoundEntity(id, name, createdAt)

fun CharacterEntity.toDomain(): Character = Character(
    id = id,
    roundId = roundId,
    playerName = playerName,
    characterName = characterName,
    initiative = initiative,
    imageUri = imageUri,
    currentHp = currentHp,
    maxHp = maxHp,
    tempHp = tempHp,
    isActive = isActive,
    type = CharacterType.valueOf(type),
    deathSuccesses = deathSuccesses,
    deathFailures = deathFailures,
    isDead = isDead
)

fun Character.toEntity(): CharacterEntity = CharacterEntity(
    id = id,
    roundId = roundId,
    playerName = playerName,
    characterName = characterName,
    initiative = initiative,
    imageUri = imageUri,
    currentHp = currentHp,
    maxHp = maxHp,
    tempHp = tempHp,
    isActive = isActive,
    type = type.name,
    deathSuccesses = deathSuccesses,
    deathFailures = deathFailures,
    isDead = isDead
)