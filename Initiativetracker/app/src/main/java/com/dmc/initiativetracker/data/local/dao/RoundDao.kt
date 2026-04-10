package com.dmc.initiativetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dmc.initiativetracker.data.local.entity.RoundEntity
import kotlinx.coroutines.flow.Flow

data class RoundWithCharacterCountDb(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val characterCount: Int
)

@Dao
interface RoundDao {
    @Query("SELECT * FROM rounds ORDER BY createdAt DESC")
    fun observeRounds(): Flow<List<RoundEntity>>

    @Query("""
        SELECT
            r.id AS id,
            r.name AS name,
            r.createdAt AS createdAt,
            (
                SELECT COUNT(*)
                FROM characters c
                WHERE c.roundId = r.id
            ) AS characterCount
        FROM rounds r
        ORDER BY r.createdAt DESC
    """)
    fun observeRoundsWithCharacterCount(): Flow<List<RoundWithCharacterCountDb>>

    @Query("SELECT * FROM rounds WHERE id = :id LIMIT 1")
    fun observeRound(id: Long): Flow<RoundEntity?>

    @Query("SELECT * FROM rounds WHERE id = :roundId LIMIT 1")
    fun observeById(roundId: Long): Flow<RoundEntity?>

    @Insert
    suspend fun insert(round: RoundEntity): Long

    @Update
    suspend fun update(round: RoundEntity)

    @Query("DELETE FROM rounds WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE rounds SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)
}