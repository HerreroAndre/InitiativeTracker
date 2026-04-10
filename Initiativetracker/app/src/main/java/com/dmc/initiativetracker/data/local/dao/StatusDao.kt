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
        WHERE durationRounds <= 0
    """)
    suspend fun deleteExpired()

    @Query("""
        UPDATE statuses
        SET durationRounds = durationRounds - 1
        WHERE durationRounds > 0
    """)
    suspend fun decrementAllRoundDurations()
}