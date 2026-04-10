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

    override suspend fun commitCharacterDraft(roundId: Long, draft: List<Character>) {
        val existingIds = characterDao.getIdsByRoundId(roundId)
        val draftIds = draft.mapNotNull { if (it.id > 0) it.id else null }

        val removedIds = existingIds.filter { it !in draftIds }
        if (removedIds.isNotEmpty()) {
            characterDao.deleteByIds(removedIds)
        }

        val entities = draft.map { it.copy(roundId = roundId).toEntity() }
        characterDao.upsertAll(entities)
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


}