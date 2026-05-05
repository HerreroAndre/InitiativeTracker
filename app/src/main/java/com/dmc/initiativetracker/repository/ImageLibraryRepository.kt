package com.dmc.initiativetracker.repository

import com.dmc.initiativetracker.domain.model.LibraryImage
import kotlinx.coroutines.flow.Flow

interface ImageLibraryRepository {
    fun observeImages(): Flow<List<LibraryImage>>
    suspend fun addImage(name: String, imageUri: String): Long
    suspend fun deleteImage(id: Long)
    suspend fun renameImage(id: Long, name: String)
}