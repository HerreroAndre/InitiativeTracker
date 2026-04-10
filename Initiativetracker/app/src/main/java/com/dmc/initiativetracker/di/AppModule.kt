package com.dmc.initiativetracker.di

import android.content.Context
import androidx.room.Room
import com.dmc.initiativetracker.data.local.AppDatabase
import com.dmc.initiativetracker.repository.CombatRepository
import com.dmc.initiativetracker.repository.CombatRepositoryImpl
import com.dmc.initiativetracker.repository.RoundRepository
import com.dmc.initiativetracker.repository.RoundRepositoryImpl

object AppModule {
    @Volatile
    private var db: AppDatabase? = null

    @Volatile
    private var roundRepo: RoundRepository? = null

    @Volatile
    private var combatRepo: CombatRepository? = null

    fun provideDatabase(context: Context): AppDatabase =
        db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "initiative_tracker.db"
            )
                .fallbackToDestructiveMigration()
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
}