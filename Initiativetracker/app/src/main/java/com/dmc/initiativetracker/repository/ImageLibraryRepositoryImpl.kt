package com.dmc.initiativetracker.repository

import com.dmc.initiativetracker.data.local.dao.ImageLibraryDao
import com.dmc.initiativetracker.data.local.mapper.toDomain
import com.dmc.initiativetracker.data.local.entity.ImageLibraryEntity
import com.dmc.initiativetracker.domain.model.LibraryImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ImageLibraryRepositoryImpl(
    private val dao: ImageLibraryDao
) : ImageLibraryRepository {

    override fun observeImages(): Flow<List<LibraryImage>> =
        dao.observeImages().map { list -> list.map { it.toDomain() } }

    override suspend fun addImage(name: String, imageUri: String): Long {
        return dao.insert(
            ImageLibraryEntity(
                name = name.trim().ifBlank { "Imagen" },
                imageUri = imageUri,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteImage(id: Long) {
        dao.deleteById(id)
    }
    override suspend fun renameImage(id: Long, name: String) {
        dao.rename(id, name.trim().ifBlank { "Imagen" })
    }
}