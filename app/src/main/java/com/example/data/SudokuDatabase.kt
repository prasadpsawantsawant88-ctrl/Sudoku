package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        GameSessionEntity::class,
        StatsEntity::class,
        UserPreferencesEntity::class,
        DailyActivityEntity::class,
        DailyStreakEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class SudokuDatabase : RoomDatabase() {
    abstract fun gameSessionDao(): GameSessionDao
    abstract fun statsDao(): StatsDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun dailyStreakDao(): DailyStreakDao

    companion object {
        @Volatile
        private var INSTANCE: SudokuDatabase? = null

        fun getDatabase(context: Context): SudokuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SudokuDatabase::class.java,
                    "sudoku_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
