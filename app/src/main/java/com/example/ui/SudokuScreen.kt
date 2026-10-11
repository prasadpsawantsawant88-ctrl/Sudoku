package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CelebrationBanner
import com.example.ui.components.HintDialog
import com.example.ui.components.NewGameDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.StatsDialog
import com.example.ui.components.SudokuControls
import com.example.ui.components.SudokuGrid
import com.example.ui.components.SudokuHeader
import com.example.ui.components.VictoryDialog
import com.example.ui.theme.LocalSudokuColors

@Composable
fun SudokuScreen(
    viewModel: SudokuViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalSudokuColors.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyEvent.key) {
                        Key.One, Key.NumPad1 -> { viewModel.inputDigit(1); true }
                        Key.Two, Key.NumPad2 -> { viewModel.inputDigit(2); true }
                        Key.Three, Key.NumPad3 -> { viewModel.inputDigit(3); true }
                        Key.Four, Key.NumPad4 -> { viewModel.inputDigit(4); true }
                        Key.Five, Key.NumPad5 -> { viewModel.inputDigit(5); true }
                        Key.Six, Key.NumPad6 -> { viewModel.inputDigit(6); true }
                        Key.Seven, Key.NumPad7 -> { viewModel.inputDigit(7); true }
                        Key.Eight, Key.NumPad8 -> { viewModel.inputDigit(8); true }
                        Key.Nine, Key.NumPad9 -> { viewModel.inputDigit(9); true }
                        Key.Backspace, Key.Delete -> { viewModel.eraseCell(); true }
                        Key.DirectionUp -> {
                            val r = if (state.selectedRow > 0) state.selectedRow - 1 else 0
                            val c = if (state.selectedCol >= 0) state.selectedCol else 0
                            viewModel.selectCell(r, c)
                            true
                        }
                        Key.DirectionDown -> {
                            val r = if (state.selectedRow < 8) state.selectedRow + 1 else 8
                            val c = if (state.selectedCol >= 0) state.selectedCol else 0
                            viewModel.selectCell(r, c)
                            true
                        }
                        Key.DirectionLeft -> {
                            val r = if (state.selectedRow >= 0) state.selectedRow else 0
                            val c = if (state.selectedCol > 0) state.selectedCol - 1 else 0
                            viewModel.selectCell(r, c)
                            true
                        }
                        Key.DirectionRight -> {
                            val r = if (state.selectedRow >= 0) state.selectedRow else 0
                            val c = if (state.selectedCol < 8) state.selectedCol + 1 else 8
                            viewModel.selectCell(r, c)
                            true
                        }
                        Key.N -> { viewModel.toggleNotesMode(); true }
                        Key.H -> { viewModel.requestHint(); true }
                        Key.U -> { viewModel.undo(); true }
                        else -> false
                    }
                } else false
            },
        containerColor = colors.background
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = colors.accent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Generating Puzzle...",
                        color = colors.secondaryText,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Bar
                SudokuHeader(
                    difficulty = state.difficulty,
                    gameMode = state.gameMode,
                    elapsedSeconds = state.elapsedSeconds,
                    isPaused = state.isPaused,
                    mistakeCount = state.mistakeCount,
                    onDifficultySelect = { diff -> viewModel.startNewGame(diff, state.gameMode) },
                    onTogglePause = { viewModel.togglePause() },
                    onNewGameClick = { viewModel.setShowNewGameDialog(true) },
                    onStatsClick = { viewModel.setShowStatsDialog(true) },
                    onSettingsClick = { viewModel.setShowSettingsDialog(true) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Board with optional Pause Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentAlignment = Alignment.Center
                ) {
                    SudokuGrid(
                        cells = state.cells,
                        selectedRow = state.selectedRow,
                        selectedCol = state.selectedCol,
                        selectedValue = state.selectedValue,
                        highlightMatching = state.preferences.highlightMatching,
                        highlightConflicts = state.preferences.highlightConflicts,
                        highlightPeers = state.preferences.highlightPeers,
                        isVictorious = state.isVictorious,
                        onCellClick = { r, c -> viewModel.selectCell(r, c) }
                    )

                    // Paused Overlay
                    if (state.isPaused) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.boardSurface.copy(alpha = 0.95f))
                                .clickable { viewModel.togglePause() }
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleOutline,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Game Paused",
                                    color = colors.primaryText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap anywhere to resume",
                                    color = colors.secondaryText,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Non-intrusive Celebratory Banner Overlay when board is completed
                CelebrationBanner(
                    visible = state.isVictorious,
                    difficulty = state.difficulty,
                    elapsedSeconds = state.elapsedSeconds,
                    mistakes = state.mistakeCount,
                    hintsUsed = state.hintsUsed,
                    onNewGame = { viewModel.startNewGame(state.difficulty, state.gameMode) },
                    onViewStats = { viewModel.setShowStatsDialog(true) }
                )

                // Gameplay Controls (Actions & Numpad)
                if (!state.isVictorious) {
                    SudokuControls(
                        isNotesMode = state.isNotesMode,
                        canUndo = state.canUndo,
                        canRedo = state.canRedo,
                        hintsUsed = state.hintsUsed,
                        remainingCounts = state.remainingDigitsCount,
                        onDigitClick = { d -> viewModel.inputDigit(d) },
                        onEraseClick = { viewModel.eraseCell() },
                        onToggleNotes = { viewModel.toggleNotesMode() },
                        onUndoClick = { viewModel.undo() },
                        onRedoClick = { viewModel.redo() },
                        onHintClick = { viewModel.requestHint() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Modals & Dialogs
        val activeHint = state.activeHint
        if (activeHint != null && state.hintStep > 0) {
            HintDialog(
                hint = activeHint,
                step = state.hintStep,
                onAdvance = { viewModel.advanceHint() },
                onDismiss = { viewModel.dismissHint() }
            )
        }

        if (state.showNewGameDialog) {
            NewGameDialog(
                currentDifficulty = state.difficulty,
                onStartGame = { diff, mode -> viewModel.startNewGame(diff, mode) },
                onStartDaily = { viewModel.startDailyChallenge() },
                onDismiss = { viewModel.setShowNewGameDialog(false) }
            )
        }

        if (state.showStatsDialog) {
            StatsDialog(
                statsList = state.statsList,
                dailyActivities = state.dailyActivities,
                dailyStreak = state.dailyStreak,
                onResetStats = { viewModel.resetStats() },
                onDismiss = { viewModel.setShowStatsDialog(false) }
            )
        }

        if (state.showSettingsDialog) {
            SettingsDialog(
                preferences = state.preferences,
                onUpdateAutoClear = { viewModel.updateAutoClearNotes(it) },
                onUpdateHighlightConflicts = { viewModel.updateHighlightConflicts(it) },
                onUpdateHighlightMatching = { viewModel.updateHighlightMatching(it) },
                onUpdateHighlightPeers = { viewModel.updateHighlightPeers(it) },
                onUpdateDarkTheme = { viewModel.updateDarkTheme(it) },
                onDismiss = { viewModel.setShowSettingsDialog(false) }
            )
        }
    }
}
