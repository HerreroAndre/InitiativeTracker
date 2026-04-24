package com.dmc.initiativetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dmc.initiativetracker.data.local.entity.ImageLibraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageLibraryDao {

    @Query("""
        SELECT * FROM image_library
        ORDER BY createdAt DESC
    """)
    fun observeImages(): Flow<List<ImageLibraryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ImageLibraryEntity): Long

    @Query("DELETE FROM image_library WHERE id = :id")
    suspend fun deleteById(id: Long)
    @Query("UPDATE image_library SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)
}