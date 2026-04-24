package com.dmc.initiativetracker.export

import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.CharacterType
import com.dmc.initiativetracker.domain.model.Status
import com.dmc.initiativetracker.domain.model.StatusType

data class RoundImportCharacter(
    val character: Character,
    val statuses: List<Status>
)

fun buildRoundTransfer(
    roundName: String,
    characters: List<Character>,
    statuses: List<Status>,
    imageFileNameForCharacter: (Character) -> String? = { null }
): RoundTransfer {
    val statusesByCharacterId = statuses.groupBy { it.characterId }

    return RoundTransfer(
        version = 1,
        roundName = roundName.ifBlank { "Ronda exportada" },
        characters = characters.map { character ->
            RoundCharacterTransfer(
                playerName = character.playerName,
                characterName = character.characterName,
                initiative = character.initiative,
                imageFileName = imageFileNameForCharacter(character),

                currentHp = character.currentHp,
                maxHp = character.maxHp,
                tempHp = character.tempHp,

                isActive = character.isActive,
                type = character.type.name,
                deathSuccesses = character.deathSuccesses,
                deathFailures = character.deathFailures,
                isDead = character.isDead,

                statuses = statusesByCharacterId[character.id]
                    .orEmpty()
                    .map { status ->
                        RoundStatusTransfer(
                            name = status.name,
                            type = status.type.name,
                            durationRounds = status.durationRounds,
                            originLabel = status.originLabel,
                            concentrationGroupId = status.concentrationGroupId
                        )
                    }
            )
        }
    )
}

fun RoundTransfer.toImportCharacters(
    roundId: Long,
    imageUriForFileName: (String?) -> String? = { null }
): List<RoundImportCharacter> {
    return characters.map { transferCharacter ->
        val character = Character(
            id = 0,
            roundId = roundId,
            playerName = transferCharacter.playerName,
            characterName = transferCharacter.characterName,
            initiative = transferCharacter.initiative,
            imageUri = imageUriForFileName(transferCharacter.imageFileName),

            currentHp = transferCharacter.currentHp,
            maxHp = transferCharacter.maxHp,
            tempHp = transferCharacter.tempHp.coerceAtLeast(0),

            isActive = transferCharacter.isActive,
            type = safeCharacterType(transferCharacter.type),
            deathSuccesses = transferCharacter.deathSuccesses.coerceIn(0, 3),
            deathFailures = transferCharacter.deathFailures.coerceIn(0, 3),
            isDead = transferCharacter.isDead
        )

        val statuses = transferCharacter.statuses.map { transferStatus ->
            Status(
                id = 0,
                characterId = 0,
                name = transferStatus.name,
                type = safeStatusType(transferStatus.type),
                durationRounds = transferStatus.durationRounds.coerceAtLeast(1),
                originCharacterId = null,
                originLabel = transferStatus.originLabel,
                concentrationGroupId = transferStatus.concentrationGroupId
            )
        }

        RoundImportCharacter(
            character = character,
            statuses = statuses
        )
    }
}

private fun safeCharacterType(value: String): CharacterType {
    return runCatching { CharacterType.valueOf(value) }
        .getOrDefault(CharacterType.NPC)
}

private fun safeStatusType(value: String): StatusType {
    return runCatching { StatusType.valueOf(value) }
        .getOrDefault(StatusType.NEUTRAL)
}