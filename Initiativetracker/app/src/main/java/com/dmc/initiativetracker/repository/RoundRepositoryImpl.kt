package com.dmc.initiativetracker.repository

import com.dmc.initiativetracker.data.local.dao.CharacterDao
import com.dmc.initiativetracker.data.local.dao.RoundDao
import com.dmc.initiativetracker.data.local.dao.StatusDao
import com.dmc.initiativetracker.data.local.entity.RoundEntity
import com.dmc.initiativetracker.data.local.mapper.toDomain
import com.dmc.initiativetracker.data.local.mapper.toEntity
import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.Round
import com.dmc.initiativetracker.domain.model.RoundListItem
import com.dmc.initiativetracker.domain.model.Status
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.dmc.initiativetracker.export.RoundTransfer
import com.dmc.initiativetracker.export.toImportCharacters

class RoundRepositoryImpl(
    private val roundDao: RoundDao,
    private val characterDao: CharacterDao,
    private val statusDao: StatusDao,
) : RoundRepository {

    override fun observeRounds(): Flow<List<Round>> =
        roundDao.observeRounds()
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun createRound(name: String): Long =
        roundDao.insert(
            RoundEntity(
                name = name,
                createdAt = System.currentTimeMillis()
            )
        )

    override suspend fun deleteRound(roundId: Long) {
        roundDao.deleteById(roundId)
    }

    override fun observeRoundName(roundId: Long): Flow<String> =
        roundDao.observeById(roundId)
            .map { entity -> entity?.name ?: "Ronda" }

    override suspend fun upsertCharacter(character: Character) {
        characterDao.upsertAll(listOf(character.toEntity()))
    }

    override fun observeCharacters(roundId: Long): Flow<List<Character>> =
        characterDao.observeByRoundId(roundId)
            .map { list -> list.map { it.toDomain() } }

    override fun observeStatuses(roundId: Long): Flow<List<Status>> =
        statusDao.observeByRoundId(roundId)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun renameRound(roundId: Long, name: String) {
        roundDao.rename(roundId, name.trim())
    }

    override suspend fun commitCharacterDraft(
        roundId: Long,
        draft: List<Character>
    ): Map<Long, Long> {
        val existingIds = characterDao.getIdsByRoundId(roundId)
        val draftExistingIds = draft.mapNotNull { if (it.id > 0) it.id else null }

        val removedIds = existingIds.filter { it !in draftExistingIds }
        if (removedIds.isNotEmpty()) {
            characterDao.deleteByIds(removedIds)
        }

        val existingCharacters = draft
            .filter { it.id > 0 }
            .map { it.copy(roundId = roundId).toEntity() }

        if (existingCharacters.isNotEmpty()) {
            characterDao.upsertAll(existingCharacters)
        }

        val tempToRealIds = mutableMapOf<Long, Long>()

        draft
            .filter { it.id < 0 }
            .forEach { character ->
                val realId = characterDao.insert(
                    character
                        .copy(
                            id = 0,
                            roundId = roundId
                        )
                        .toEntity()
                )

                tempToRealIds[character.id] = realId
            }

        return tempToRealIds
    }

    override suspend fun addStatus(status: Status): Long {
        return statusDao.insert(status.toEntity())
    }

    override suspend fun removeStatus(statusId: Long) {
        statusDao.deleteById(statusId)
    }

    override suspend fun removeStatusesByConcentrationGroup(groupId: String) {
        statusDao.deleteByConcentrationGroup(groupId)
    }

    override suspend fun decrementStatusDurations() {
        statusDao.decrementAllRoundDurations()
    }

    override suspend fun deleteExpiredStatuses() {
        statusDao.deleteExpired()
    }

    override suspend fun updateCharacterHp(
        characterId: Long,
        currentHp: Int?,
        maxHp: Int?,
        tempHp: Int
    ) {
        characterDao.updateHp(
            characterId = characterId,
            currentHp = currentHp,
            maxHp = maxHp,
            tempHp = tempHp
        )
    }

    override fun observeRoundListItems(): Flow<List<RoundListItem>> =
        roundDao.observeRoundsWithCharacterCount()
            .map { list ->
                list.map {
                    RoundListItem(
                        id = it.id,
                        name = it.name,
                        createdAt = it.createdAt,
                        characterCount = it.characterCount
                    )
                }
            }
    override suspend fun importRound(
        transfer: RoundTransfer,
        replaceRoundId: Long?,
        imageUrisByFileName: Map<String, String>
    ): Long {
        val targetRoundId = if (replaceRoundId == null) {
            roundDao.insert(
                RoundEntity(
                    name = transfer.roundName.ifBlank { "Ronda importada" },
                    createdAt = System.currentTimeMillis()
                )
            )
        } else {
            roundDao.rename(
                id = replaceRoundId,
                name = transfer.roundName.ifBlank { "Ronda importada" }
            )

            statusDao.deleteByRoundId(replaceRoundId)
            characterDao.deleteByRoundId(replaceRoundId)

            replaceRoundId
        }

        val importCharacters = transfer.toImportCharacters(
            roundId = targetRoundId,
            imageUriForFileName = { fileName ->
                fileName?.let { imageUrisByFileName[it] }
            }
        )

        importCharacters.forEach { item ->
            val realCharacterId = characterDao.insert(
                item.character
                    .copy(
                        id = 0,
                        roundId = targetRoundId
                    )
                    .toEntity()
            )

            item.statuses.forEach { status ->
                statusDao.insert(
                    status
                        .copy(
                            id = 0,
                            characterId = realCharacterId
                        )
                        .toEntity()
                )
            }
        }

        return targetRoundId
    }
}