package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_session")
data class GameSessionEntity(
    @PrimaryKey val id: Int = 1,
    val mode: String,
    val difficulty: String,
    val boardData: String,        // 81 digits (0-9)
    val cluesData: String,        // 81 digits (0-9)
    val solutionData: String,     // 81 digits (1-9)
    val notesData: String,        // encoded notes: "idx:1,2;idx2:4,5"
    val elapsedSeconds: Long,
    val mistakeCount: Int,
    val hintsCount: Int,
    val isCompleted: Boolean,
    val seed: Long
)

@Entity(tableName = "player_stats")
data class StatsEntity(
    @PrimaryKey val difficulty: String,
    val played: Int = 0,
    val won: Int = 0,
    val bestTimeSeconds: Long = 0L,
    val totalTimeSeconds: Long = 0L,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val hintsUsed: Int = 0,
    val mistakesMade: Int = 0
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val autoClearNotes: Boolean = true,
    val highlightConflicts: Boolean = true,
    val highlightMatching: Boolean = true,
    val highlightPeers: Boolean = true,
    val darkTheme: Boolean = false
)

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val dateString: String, // "YYYY-MM-DD"
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0
)

@Entity(tableName = "daily_challenge_streak")
data class DailyStreakEntity(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val lastCompletedDate: String = "", // "YYYY-MM-DD"
    val totalCompleted: Int = 0
)
