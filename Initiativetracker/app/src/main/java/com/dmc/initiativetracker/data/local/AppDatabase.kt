package com.dmc.initiativetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dmc.initiativetracker.data.local.dao.CharacterDao
import com.dmc.initiativetracker.data.local.dao.RoundDao
import com.dmc.initiativetracker.data.local.dao.StatusDao
import com.dmc.initiativetracker.data.local.entity.CharacterEntity
import com.dmc.initiativetracker.data.local.entity.RoundEntity
import com.dmc.initiativetracker.data.local.entity.StatusEntity

@Database(
    entities = [RoundEntity::class, CharacterEntity::class, StatusEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roundDao(): RoundDao
    abstract fun characterDao(): CharacterDao
    abstract fun statusDao(): StatusDao
}