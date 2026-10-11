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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StatsEntity
import com.example.data.UserPreferencesEntity
import com.example.engine.SudokuHintEngine
import com.example.model.Difficulty
import com.example.model.GameMode
import com.example.ui.theme.LocalSudokuColors
import java.util.Locale

@Composable
fun HintDialog(
    hint: SudokuHintEngine.ComprehensiveHint,
    step: Int, // 1=Nudge, 2=Technique, 3=Reveal
    onAdvance: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalSudokuColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = colors.hint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (step) {
                            1 -> "Hint: Nudge"
                            2 -> "Technique: ${hint.techniqueName}"
                            else -> "Revealed Number"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.secondaryText
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (step) {
                    1 -> {
                        Text(
                            text = hint.nudgeMessage,
                            color = colors.primaryText,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Target Cell: Row ${hint.row + 1}, Col ${hint.col + 1}",
                            color = colors.hint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    2 -> {
                        Text(
                            text = hint.explanation,
                            color = colors.primaryText,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cell: Row ${hint.row + 1}, Col ${hint.col + 1}",
                            color = colors.hint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    else -> {
                        Text(
                            text = "The solution for Row ${hint.row + 1}, Column ${hint.col + 1} is ${hint.correctDigit}.",
                            color = colors.primaryText,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAdvance,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.boardSurface
                ),
                modifier = Modifier.testTag("hint_advance_button")
            ) {
                Text(
                    text = when (step) {
                        1 -> "Show Technique"
                        2 -> "Reveal Number"
                        else -> "Done"
                    }
                )
            }
        },
        dismissButton = {
            if (step < 3) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.secondaryText)
                ) {
                    Text("Dismiss")
                }
            }
        },
        containerColor = colors.boardSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun NewGameDialog(
    currentDifficulty: Difficulty,
    onStartGame: (Difficulty, GameMode) -> Unit,
    onStartDaily: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalSudokuColors.current
    var selectedDifficulty by remember { mutableStateOf(currentDifficulty) }
    var selectedMode by remember { mutableStateOf(GameMode.CLASSIC) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Puzzle",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.secondaryText
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Daily Challenge Card Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent.copy(alpha = 0.12f))
                        .clickable { onStartDaily() }
                        .padding(14.dp)
                        .testTag("daily_challenge_card")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Daily Challenge",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText
                            )
                            Text(
                                text = "Deterministic date-seeded puzzle for today",
                                fontSize = 12.sp,
                                color = colors.secondaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colors.gridBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Mode Selection
                Text(
                    text = "GAME MODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.secondaryText,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(GameMode.CLASSIC, GameMode.PRACTICE, GameMode.TIMED).forEach { mode ->
                        val isSel = (mode == selectedMode)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) colors.accent else colors.background)
                                .clickable { selectedMode = mode }
                                .padding(vertical = 10.dp)
                                .testTag("mode_${mode.name.lowercase(Locale.ROOT)}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) colors.boardSurface else colors.primaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Difficulty Selection
                Text(
                    text = "DIFFICULTY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.secondaryText,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Difficulty.values().forEach { diff ->
                    val isSel = (diff == selectedDifficulty)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) colors.selectedCell else colors.background)
                            .clickable { selectedDifficulty = diff }
                            .padding(12.dp)
                            .testTag("select_difficulty_${diff.name.lowercase(Locale.ROOT)}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = diff.label,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) colors.accent else colors.primaryText
                                )
                                Text(
                                    text = diff.description,
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                            }
                            if (isSel) {
                                Text(
                                    text = "✓",
                                    color = colors.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartGame(selectedDifficulty, selectedMode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.boardSurface
                ),
                modifier = Modifier.testTag("start_game_confirm_button")
            ) {
                Text("Start Game")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.secondaryText)
            ) {
                Text("Cancel")
            }
        },
        containerColor = colors.boardSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun VictoryDialog(
    difficulty: Difficulty,
    elapsedSeconds: Long,
    mistakes: Int,
    hintsUsed: Int,
    onNewGame: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalSudokuColors.current
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Puzzle Solved!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Excellent play! All 81 cells verified.",
                    fontSize = 14.sp,
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.background)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatRow("Difficulty", difficulty.label)
                        StatRow("Completion Time", timeFormatted)
                        StatRow("Mistakes", mistakes.toString())
                        StatRow("Hints Used", hintsUsed.toString())
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onNewGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.boardSurface
                ),
                modifier = Modifier.testTag("victory_new_game_button")
            ) {
                Text("Play Next Puzzle")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.secondaryText)
            ) {
                Text("Close")
            }
        },
        containerColor = colors.boardSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun StatsDialog(
    statsList: List<StatsEntity>,
    dailyActivities: List<com.example.data.DailyActivityEntity> = emptyList(),
    dailyStreak: com.example.data.DailyStreakEntity = com.example.data.DailyStreakEntity(),
    onResetStats: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalSudokuColors.current
    var showConfirmReset by remember { mutableStateOf(false) }

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            title = { Text("Reset All Statistics?", color = colors.primaryText) },
            text = { Text("This will permanently erase your games played, best times, and streaks.", color = colors.secondaryText) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetStats()
                        showConfirmReset = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReset = false }) {
                    Text("Cancel", color = colors.secondaryText)
                }
            },
            containerColor = colors.boardSurface
        )
    }

    val totalPlayed = statsList.sumOf { it.played }
    val totalWon = statsList.sumOf { it.won }
    val winRate = if (totalPlayed > 0) ((totalWon.toDouble() / totalPlayed) * 100).toInt() else 0
    val currentStreak = statsList.maxOfOrNull { it.currentStreak } ?: 0
    val maxStreak = statsList.maxOfOrNull { it.maxStreak } ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Player Statistics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.secondaryText
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Overview Highlights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Played", totalPlayed.toString(), Modifier.weight(1f))
                    StatCard("Win Rate", "$winRate%", Modifier.weight(1f))
                    StatCard("Best Streak", maxStreak.toString(), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 30-Day Activity, Daily Streak, and Win Frequency Chart
                StatsActivityChart(
                    dailyActivities = dailyActivities,
                    currentStreak = currentStreak,
                    maxStreak = maxStreak,
                    dailyStreak = dailyStreak
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PERFORMANCE BY DIFFICULTY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.secondaryText,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Difficulty.values().forEach { diff ->
                    val stat = statsList.find { it.difficulty == diff.name }
                    val played = stat?.played ?: 0
                    val won = stat?.won ?: 0
                    val bestTimeSec = stat?.bestTimeSeconds ?: 0L
                    val bestTimeFormatted = if (bestTimeSec > 0L) {
                        String.format(Locale.getDefault(), "%02d:%02d", bestTimeSec / 60, bestTimeSec % 60)
                    } else "--:--"

                    val avgTimeSec = if (won > 0) (stat?.totalTimeSeconds ?: 0L) / won else 0L
                    val avgTimeFormatted = if (avgTimeSec > 0L) {
                        String.format(Locale.getDefault(), "%02d:%02d", avgTimeSec / 60, avgTimeSec % 60)
                    } else "--:--"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.background)
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = diff.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = "Won: $won / $played",
                                    fontSize = 13.sp,
                                    color = colors.secondaryText
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Best: $bestTimeFormatted",
                                    fontSize = 12.sp,
                                    color = colors.accent
                                )
                                Text(
                                    text = "Avg: $avgTimeFormatted",
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showConfirmReset = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error)
                ) {
                    Text("Reset All Statistics")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.boardSurface
                )
            ) {
                Text("Close")
            }
        },
        containerColor = colors.boardSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun SettingsDialog(
    preferences: UserPreferencesEntity,
    onUpdateAutoClear: (Boolean) -> Unit,
    onUpdateHighlightConflicts: (Boolean) -> Unit,
    onUpdateHighlightMatching: (Boolean) -> Unit,
    onUpdateHighlightPeers: (Boolean) -> Unit,
    onUpdateDarkTheme: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalSudokuColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.secondaryText
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingToggleRow(
                    title = "Highlight 3×3 Block & Lines",
                    subtitle = "Softly shade the row, column, and 3×3 box of the selected cell",
                    checked = preferences.highlightPeers,
                    onCheckedChange = onUpdateHighlightPeers
                )

                HorizontalDivider(color = colors.gridBorder)

                SettingToggleRow(
                    title = "Auto-Clear Notes",
                    subtitle = "Automatically erase completed candidates from peer cells",
                    checked = preferences.autoClearNotes,
                    onCheckedChange = onUpdateAutoClear
                )

                HorizontalDivider(color = colors.gridBorder)

                SettingToggleRow(
                    title = "Highlight Conflicts",
                    subtitle = "Show subtle indicator on duplicate digits in rows, columns, or 3×3 blocks",
                    checked = preferences.highlightConflicts,
                    onCheckedChange = onUpdateHighlightConflicts
                )

                HorizontalDivider(color = colors.gridBorder)

                SettingToggleRow(
                    title = "Highlight Matching Digits",
                    subtitle = "Highlight all instances of the currently selected number",
                    checked = preferences.highlightMatching,
                    onCheckedChange = onUpdateHighlightMatching
                )

                HorizontalDivider(color = colors.gridBorder)

                SettingToggleRow(
                    title = "Dark Theme",
                    subtitle = "Switch between Warm Ivory and Deep Charcoal themes",
                    checked = preferences.darkTheme,
                    onCheckedChange = onUpdateDarkTheme
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.boardSurface
                )
            ) {
                Text("Done")
            }
        },
        containerColor = colors.boardSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    val colors = LocalSudokuColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = colors.secondaryText, fontSize = 14.sp)
        Text(text = value, color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalSudokuColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.background)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            Text(text = label, fontSize = 11.sp, color = colors.secondaryText)
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalSudokuColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
            Text(text = subtitle, fontSize = 12.sp, color = colors.secondaryText, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.boardSurface,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.secondaryText,
                uncheckedTrackColor = colors.background
            )
        )
    }
}
