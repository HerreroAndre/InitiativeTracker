package com.dmc.initiativetracker.domain.model

import com.dmc.initiativetracker.data.local.entity.CharacterEntity

fun CharacterEntity.toDomainCharacter(): Character = Character(
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

fun Character.toCharacterEntity(): CharacterEntity = CharacterEntity(
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