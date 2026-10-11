package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSessionDao {
    @Query("SELECT * FROM game_session WHERE id = 1 LIMIT 1")
    fun getActiveSession(): Flow<GameSessionEntity?>

    @Query("SELECT * FROM game_session WHERE id = 1 LIMIT 1")
    suspend fun getActiveSessionOnce(): GameSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: GameSessionEntity)

    @Query("DELETE FROM game_session WHERE id = 1")
    suspend fun clearSession()
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM player_stats")
    fun getAllStats(): Flow<List<StatsEntity>>

    @Query("SELECT * FROM player_stats WHERE difficulty = :diff LIMIT 1")
    suspend fun getStatsForDifficulty(diff: String): StatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStats(stats: StatsEntity)

    @Query("DELETE FROM player_stats")
    suspend fun resetAllStats()
}

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    fun getPreferences(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    suspend fun getPreferencesOnce(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(prefs: UserPreferencesEntity)
}

@Dao
interface DailyActivityDao {
    @Query("SELECT * FROM daily_activity ORDER BY dateString ASC")
    fun getAllDailyActivity(): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity WHERE dateString = :date LIMIT 1")
    suspend fun getDailyActivity(date: String): DailyActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDailyActivity(activity: DailyActivityEntity)

    @Query("DELETE FROM daily_activity")
    suspend fun clearDailyActivity()
}

@Dao
interface DailyStreakDao {
    @Query("SELECT * FROM daily_challenge_streak WHERE id = 1 LIMIT 1")
    fun getDailyStreak(): Flow<DailyStreakEntity?>

    @Query("SELECT * FROM daily_challenge_streak WHERE id = 1 LIMIT 1")
    suspend fun getDailyStreakOnce(): DailyStreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDailyStreak(streak: DailyStreakEntity)

    @Query("DELETE FROM daily_challenge_streak WHERE id = 1")
    suspend fun resetDailyStreak()
}
