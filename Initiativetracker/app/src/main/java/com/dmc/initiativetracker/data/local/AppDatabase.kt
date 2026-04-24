package com.dmc.initiativetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dmc.initiativetracker.data.local.dao.CharacterDao
import com.dmc.initiativetracker.data.local.dao.RoundDao
import com.dmc.initiativetracker.data.local.dao.StatusDao
import com.dmc.initiativetracker.data.local.entity.CharacterEntity
import com.dmc.initiativetracker.data.local.entity.RoundEntity
import com.dmc.initiativetracker.data.local.entity.StatusEntity
import com.dmc.initiativetracker.data.local.dao.ImageLibraryDao
import com.dmc.initiativetracker.data.local.entity.ImageLibraryEntity

@Database(
    entities = [
        RoundEntity::class,
        CharacterEntity::class,
        StatusEntity::class,
        ImageLibraryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roundDao(): RoundDao
    abstract fun characterDao(): CharacterDao
    abstract fun statusDao(): StatusDao
    abstract fun imageLibraryDao(): ImageLibraryDao
}
