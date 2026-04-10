package com.dmc.initiativetracker.repository

import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.Round
import com.dmc.initiativetracker.domain.model.Status
import kotlinx.coroutines.flow.Flow
import com.dmc.initiativetracker.domain.model.RoundListItem

interface RoundRepository {
    fun observeRoundName(roundId: Long): Flow<String>
    fun observeCharacters(roundId: Long): Flow<List<Character>>
    fun observeStatuses(roundId: Long): Flow<List<Status>>

    suspend fun upsertCharacter(character: Character)
    suspend fun commitCharacterDraft(roundId: Long, draft: List<Character>)

    suspend fun addStatus(status: Status): Long
    suspend fun removeStatus(statusId: Long)
    suspend fun removeStatusesByConcentrationGroup(groupId: String)
    suspend fun decrementStatusDurations()
    suspend fun deleteExpiredStatuses()

    suspend fun updateCharacterHp(
        characterId: Long,
        currentHp: Int?,
        maxHp: Int?,
        tempHp: Int
    )

    fun observeRounds(): Flow<List<Round>>
    fun observeRoundListItems(): Flow<List<RoundListItem>>
    suspend fun createRound(name: String): Long
    suspend fun deleteRound(roundId: Long)
    suspend fun renameRound(roundId: Long, name: String)
}