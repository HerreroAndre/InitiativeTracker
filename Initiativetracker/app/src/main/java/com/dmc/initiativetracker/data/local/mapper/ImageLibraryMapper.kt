package com.dmc.initiativetracker.data.local.mapper

import com.dmc.initiativetracker.data.local.entity.ImageLibraryEntity
import com.dmc.initiativetracker.domain.model.LibraryImage

fun ImageLibraryEntity.toDomain(): LibraryImage =
    LibraryImage(
        id = id,
        name = name,
        imageUri = imageUri,
        createdAt = createdAt
    )

fun LibraryImage.toEntity(): ImageLibraryEntity =
    ImageLibraryEntity(
        id = id,
        name = name,
        imageUri = imageUri,
        createdAt = createdAt
    )