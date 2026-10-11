package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SudokuRepository(
    private val gameDao: GameSessionDao,
    private val statsDao: StatsDao,
    private val preferencesDao: PreferencesDao,
    private val dailyDao: DailyActivityDao,
    private val streakDao: DailyStreakDao
) {
    val activeSession: Flow<GameSessionEntity?> = gameDao.getActiveSession()
    val allStats: Flow<List<StatsEntity>> = statsDao.getAllStats()
    val preferences: Flow<UserPreferencesEntity?> = preferencesDao.getPreferences()
    val allDailyActivity: Flow<List<DailyActivityEntity>> = dailyDao.getAllDailyActivity()

    // Expose daily streak with automatic active expiration check
    val dailyStreak: Flow<DailyStreakEntity> = streakDao.getDailyStreak().map { rawStreak ->
        val entity = rawStreak ?: DailyStreakEntity()
        computeLapsedStreak(entity)
    }

    suspend fun getActiveSessionOnce(): GameSessionEntity? = gameDao.getActiveSessionOnce()
    suspend fun saveSession(session: GameSessionEntity) = gameDao.saveSession(session)
    suspend fun clearSession() = gameDao.clearSession()

    suspend fun getPreferencesOnce(): UserPreferencesEntity =
        preferencesDao.getPreferencesOnce() ?: UserPreferencesEntity()

    suspend fun savePreferences(prefs: UserPreferencesEntity) =
        preferencesDao.savePreferences(prefs)

    suspend fun recordGameResult(
        difficulty: String,
        won: Boolean,
        timeSeconds: Long,
        hints: Int,
        mistakes: Int,
        isDailyChallenge: Boolean = false
    ) {
        val existing = statsDao.getStatsForDifficulty(difficulty) ?: StatsEntity(difficulty = difficulty)
        val newPlayed = existing.played + 1
        val newWon = existing.won + if (won) 1 else 0
        val newStreak = if (won) existing.currentStreak + 1 else 0
        val newMaxStreak = maxOf(existing.maxStreak, newStreak)
        val newBestTime = if (won) {
            if (existing.bestTimeSeconds == 0L) timeSeconds else minOf(existing.bestTimeSeconds, timeSeconds)
        } else existing.bestTimeSeconds
        val newTotalTime = existing.totalTimeSeconds + timeSeconds
        val newHints = existing.hintsUsed + hints
        val newMistakes = existing.mistakesMade + mistakes

        statsDao.saveStats(
            existing.copy(
                played = newPlayed,
                won = newWon,
                bestTimeSeconds = newBestTime,
                totalTimeSeconds = newTotalTime,
                currentStreak = newStreak,
                maxStreak = newMaxStreak,
                hintsUsed = newHints,
                mistakesMade = newMistakes
            )
        )

        // Record Daily Activity for today
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())
        val todayActivity = dailyDao.getDailyActivity(todayStr) ?: DailyActivityEntity(dateString = todayStr)
        dailyDao.saveDailyActivity(
            todayActivity.copy(
                gamesPlayed = todayActivity.gamesPlayed + 1,
                gamesWon = todayActivity.gamesWon + if (won) 1 else 0
            )
        )

        // If this game was a completed Daily Challenge, update consecutive Daily Streak
        if (won && isDailyChallenge) {
            recordDailyChallengeWin(todayStr)
        }
    }

    private suspend fun recordDailyChallengeWin(todayStr: String) {
        val existingStreak = streakDao.getDailyStreakOnce() ?: DailyStreakEntity()

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        val newCurrentStreak = when (existingStreak.lastCompletedDate) {
            todayStr -> existingStreak.currentStreak // Already recorded today
            yesterdayStr -> existingStreak.currentStreak + 1 // Consecutive day
            else -> 1 // Streak starts or restarts at 1
        }

        val newMaxStreak = maxOf(existingStreak.maxStreak, newCurrentStreak)
        val newTotal = if (existingStreak.lastCompletedDate == todayStr) existingStreak.totalCompleted else existingStreak.totalCompleted + 1

        streakDao.saveDailyStreak(
            DailyStreakEntity(
                id = 1,
                currentStreak = newCurrentStreak,
                maxStreak = newMaxStreak,
                lastCompletedDate = todayStr,
                totalCompleted = newTotal
            )
        )
    }

    private fun computeLapsedStreak(entity: DailyStreakEntity): DailyStreakEntity {
        if (entity.lastCompletedDate.isBlank()) return entity

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = dateFormat.format(cal.time)

        // If completed today or yesterday, streak is actively intact
        return if (entity.lastCompletedDate == todayStr || entity.lastCompletedDate == yesterdayStr) {
            entity
        } else {
            // Lapsed streak (missed yesterday)
            entity.copy(currentStreak = 0)
        }
    }

    suspend fun resetAllStats() {
        statsDao.resetAllStats()
        dailyDao.clearDailyActivity()
        streakDao.resetDailyStreak()
    }
}
