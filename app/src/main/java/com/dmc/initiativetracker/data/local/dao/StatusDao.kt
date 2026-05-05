package com.dmc.initiativetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dmc.initiativetracker.data.local.entity.StatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusDao {

    @Query("SELECT * FROM statuses WHERE characterId = :characterId ORDER BY id ASC")
    fun observeByCharacterId(characterId: Long): Flow<List<StatusEntity>>

    @Query("""
        SELECT s.* FROM statuses s
        INNER JOIN characters c ON c.id = s.characterId
        WHERE c.roundId = :roundId
        ORDER BY s.id ASC
    """)
    fun observeByRoundId(roundId: Long): Flow<List<StatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: StatusEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(statuses: List<StatusEntity>)

    @Update
    suspend fun update(status: StatusEntity)

    @Query("DELETE FROM statuses WHERE id = :statusId")
    suspend fun deleteById(statusId: Long)

    @Query("DELETE FROM statuses WHERE characterId = :characterId")
    suspend fun deleteByCharacterId(characterId: Long)

    @Query("DELETE FROM statuses WHERE concentrationGroupId = :groupId")
    suspend fun deleteByConcentrationGroup(groupId: String)

    @Query("""
    DELETE FROM statuses
    WHERE characterId IN (
        SELECT id FROM characters WHERE roundId = :roundId
    )
""")
    suspend fun deleteByRoundId(roundId: Long)

    @Query("""
    UPDATE statuses
    SET durationRounds = durationRounds - 1
    WHERE characterId = :characterId
    AND durationRounds > 0
    AND concentrationGroupId IS NULL
    AND tickTiming = :tickTiming
""")
    suspend fun decrementNormalStatusesForCharacter(
        characterId: Long,
        tickTiming: String
    )

    @Query("""
    UPDATE statuses
    SET durationRounds = durationRounds - 1
    WHERE durationRounds > 0
    AND concentrationGroupId IN (
        SELECT concentrationGroupId
        FROM statuses
        WHERE characterId = :characterId
        AND concentrationGroupId IS NOT NULL
        AND name LIKE 'Concentrando:%'
        AND tickTiming = :tickTiming
    )
""")
    suspend fun decrementConcentrationGroupsOwnedByCharacter(
        characterId: Long,
        tickTiming: String
    )

    @Query("""
    DELETE FROM statuses
    WHERE concentrationGroupId IS NULL
    AND durationRounds <= 0
""")
    suspend fun deleteExpiredNormalStatuses()

    @Query("""
    DELETE FROM statuses
    WHERE concentrationGroupId IN (
        SELECT concentrationGroupId
        FROM statuses
        WHERE concentrationGroupId IS NOT NULL
        AND name LIKE 'Concentrando:%'
        AND durationRounds <= 0
    )
""")
    suspend fun deleteExpiredConcentrationGroups()
}