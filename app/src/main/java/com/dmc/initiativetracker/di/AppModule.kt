package com.dmc.initiativetracker.di

import android.content.Context
import androidx.room.Room
import com.dmc.initiativetracker.data.local.AppDatabase
import com.dmc.initiativetracker.repository.CombatRepository
import com.dmc.initiativetracker.repository.CombatRepositoryImpl
import com.dmc.initiativetracker.repository.RoundRepository
import com.dmc.initiativetracker.repository.RoundRepositoryImpl
import com.dmc.initiativetracker.repository.ImageLibraryRepository
import com.dmc.initiativetracker.repository.ImageLibraryRepositoryImpl

object AppModule {
    @Volatile
    private var db: AppDatabase? = null

    @Volatile
    private var roundRepo: RoundRepository? = null

    @Volatile
    private var combatRepo: CombatRepository? = null
    @Volatile
    private var imageLibraryRepo: ImageLibraryRepository? = null

    fun provideDatabase(context: Context): AppDatabase =
        db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "initiative_tracker.db"
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { db = it }
        }

    fun provideRoundRepository(context: Context): RoundRepository =
        roundRepo ?: synchronized(this) {
            roundRepo ?: RoundRepositoryImpl(
                roundDao = provideDatabase(context).roundDao(),
                characterDao = provideDatabase(context).characterDao(),
                statusDao = provideDatabase(context).statusDao()
            ).also { roundRepo = it }
        }

    fun provideCombatRepository(): CombatRepository =
        combatRepo ?: synchronized(this) {
            combatRepo ?: CombatRepositoryImpl().also { combatRepo = it }
        }

    fun provideImageLibraryRepository(context: Context): ImageLibraryRepository =
        imageLibraryRepo ?: synchronized(this) {
            imageLibraryRepo ?: ImageLibraryRepositoryImpl(
                dao = provideDatabase(context).imageLibraryDao()
            ).also { imageLibraryRepo = it }
        }


}