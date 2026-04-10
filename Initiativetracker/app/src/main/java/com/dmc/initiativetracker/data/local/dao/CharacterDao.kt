package com.dmc.initiativetracker.data.local.dao

import androidx.room.*
import com.dmc.initiativetracker.data.local.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {

    @Query("""
        SELECT * FROM characters
        WHERE roundId = :roundId
        ORDER BY initiative DESC
    """)
    fun observeByRoundId(roundId: Long): Flow<List<CharacterEntity>>

    @Query("""
    UPDATE characters
    SET currentHp = :currentHp,
        maxHp = :maxHp,
        tempHp = :tempHp
    WHERE id = :characterId
""")
    suspend fun updateHp(
        characterId: Long,
        currentHp: Int?,
        maxHp: Int?,
        tempHp: Int
    )

    @Upsert
    suspend fun upsertAll(characters: List<CharacterEntity>)

    @Query("DELETE FROM characters WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT id FROM characters WHERE roundId = :roundId")
    suspend fun getIdsByRoundId(roundId: Long): List<Long>
}