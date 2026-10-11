package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Difficulty
import com.example.model.GameMode
import com.example.ui.theme.LocalSudokuColors
import java.util.Locale

@Composable
fun SudokuHeader(
    difficulty: Difficulty,
    gameMode: GameMode,
    elapsedSeconds: Long,
    isPaused: Boolean,
    mistakeCount: Int,
    onDifficultySelect: (Difficulty) -> Unit,
    onTogglePause: () -> Unit,
    onNewGameClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSudokuColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Top Bar: Wordmark, Mode Badge, and Action Icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SUDOKU",
                    color = colors.primaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.SansSerif
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Game Mode Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = gameMode.title,
                        color = colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNewGameClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("new_game_button")
                        .semantics { contentDescription = "New Game" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Game",
                        tint = colors.primaryText
                    )
                }

                IconButton(
                    onClick = onStatsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("stats_button")
                        .semantics { contentDescription = "Statistics" }
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Statistics",
                        tint = colors.primaryText
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("settings_button")
                        .semantics { contentDescription = "Settings" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = colors.primaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Secondary Info Bar: Difficulty Selector & Timer / Mistakes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Difficulty Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Difficulty.values().forEach { diff ->
                    val isSelected = (diff == difficulty)
                    val pillBg = if (isSelected) colors.accent else colors.boardSurface
                    val pillText = if (isSelected) colors.boardSurface else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(pillBg)
                            .clickable { onDifficultySelect(diff) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("difficulty_${diff.name.lowercase(Locale.ROOT)}")
                            .semantics { contentDescription = "${diff.label} difficulty" }
                    ) {
                        Text(
                            text = diff.label,
                            color = pillText,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Timer and Mistakes Counter
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (gameMode != GameMode.PRACTICE) {
                    Text(
                        text = "Mistakes: $mistakeCount",
                        color = if (mistakeCount > 0) colors.error else colors.secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("mistakes_counter")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Timer
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.boardSurface)
                        .clickable { onTogglePause() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("timer_button")
                        .semantics { contentDescription = "Timer: $formattedTime. Tap to pause or resume." }
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = colors.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPaused) "Paused" else formattedTime,
                        color = colors.primaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
