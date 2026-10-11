package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameSessionEntity
import com.example.data.StatsEntity
import com.example.data.SudokuDatabase
import com.example.data.SudokuRepository
import com.example.data.UserPreferencesEntity
import com.example.engine.SudokuGenerator
import com.example.engine.SudokuHintEngine
import com.example.engine.SudokuSolver
import com.example.model.Difficulty
import com.example.model.GameMode
import com.example.model.MoveAction
import com.example.model.SudokuCell
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SudokuUiState(
    val cells: List<SudokuCell> = emptyList(),
    val solution: IntArray = IntArray(81),
    val selectedRow: Int = -1,
    val selectedCol: Int = -1,
    val isNotesMode: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val difficulty: Difficulty = Difficulty.EASY,
    val gameMode: GameMode = GameMode.CLASSIC,
    val elapsedSeconds: Long = 0L,
    val isPaused: Boolean = false,
    val mistakeCount: Int = 0,
    val hintsUsed: Int = 0,
    val isVictorious: Boolean = false,
    val activeHint: SudokuHintEngine.ComprehensiveHint? = null,
    val hintStep: Int = 0, // 0=None, 1=Nudge, 2=Technique, 3=Reveal
    val showNewGameDialog: Boolean = false,
    val showStatsDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val preferences: UserPreferencesEntity = UserPreferencesEntity(),
    val statsList: List<StatsEntity> = emptyList(),
    val dailyActivities: List<com.example.data.DailyActivityEntity> = emptyList(),
    val dailyStreak: com.example.data.DailyStreakEntity = com.example.data.DailyStreakEntity(),
    val isLoading: Boolean = false
) {
    val remainingDigitsCount: Map<Int, Int>
        get() {
            val counts = mutableMapOf<Int, Int>()
            for (d in 1..9) counts[d] = 0
            for (cell in cells) {
                if (cell.value in 1..9) {
                    counts[cell.value] = (counts[cell.value] ?: 0) + 1
                }
            }
            return counts
        }

    val selectedValue: Int
        get() {
            if (selectedRow in 0..8 && selectedCol in 0..8) {
                val idx = SudokuSolver.index(selectedRow, selectedCol)
                if (idx < cells.size) return cells[idx].value
            }
            return 0
        }
}

class SudokuViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SudokuRepository

    private val _uiState = MutableStateFlow(SudokuUiState(isLoading = true))
    val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<MoveAction>()
    private val redoStack = mutableListOf<MoveAction>()

    private var timerJob: Job? = null
    private var currentSeed: Long = 0L

    init {
        val db = SudokuDatabase.getDatabase(application)
        repository = SudokuRepository(
            db.gameSessionDao(),
            db.statsDao(),
            db.preferencesDao(),
            db.dailyActivityDao(),
            db.dailyStreakDao()
        )

        viewModelScope.launch {
            // Observe preferences
            launch {
                repository.preferences.collect { prefs ->
                    if (prefs != null) {
                        _uiState.update { it.copy(preferences = prefs) }
                    }
                }
            }

            // Observe stats
            launch {
                repository.allStats.collect { stats ->
                    _uiState.update { it.copy(statsList = stats) }
                }
            }

            // Observe daily activities
            launch {
                repository.allDailyActivity.collect { activities ->
                    _uiState.update { it.copy(dailyActivities = activities) }
                }
            }

            // Observe daily challenge streak
            launch {
                repository.dailyStreak.collect { streak ->
                    _uiState.update { it.copy(dailyStreak = streak) }
                }
            }

            // Restore active session or generate new game
            val session = repository.getActiveSessionOnce()
            if (session != null && !session.isCompleted && session.boardData.length == 81) {
                restoreSession(session)
            } else {
                startNewGame(Difficulty.EASY, GameMode.CLASSIC)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_uiState.value.isPaused && !_uiState.value.isVictorious) {
                    _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                    persistCurrentSession()
                }
            }
        }
    }

    fun selectCell(row: Int, col: Int) {
        if (_uiState.value.isPaused) return
        _uiState.update {
            it.copy(
                selectedRow = row,
                selectedCol = col,
                // Clear hint highlighting when user manually taps another cell
                cells = it.cells.map { cell ->
                    if (cell.isHintTarget) cell.copy(isHintTarget = false) else cell
                }
            )
        }
    }

    fun inputDigit(digit: Int) {
        val state = _uiState.value
        val r = state.selectedRow
        val c = state.selectedCol
        enterDigit(r, c, digit)
    }

    /**
     * Updates ONLY the currently selected cell at (row, col) in the game state.
     * Never populates or alters values in any other cell.
     */
    fun enterDigit(row: Int, col: Int, digit: Int) {
        val state = _uiState.value
        if (state.isPaused || state.isVictorious) return
        if (row !in 0..8 || col !in 0..8) return

        val targetIdx = SudokuSolver.index(row, col)
        if (targetIdx !in state.cells.indices) return
        val targetCell = state.cells[targetIdx]

        // Rule: Never modify original clues
        if (targetCell.isClue) return

        if (state.isNotesMode) {
            // Notes Mode: Toggle note candidate in the target cell ONLY
            val newNotes = targetCell.notes.toMutableSet()
            val wasAdded = if (newNotes.contains(digit)) {
                newNotes.remove(digit)
                false
            } else {
                newNotes.add(digit)
                true
            }

            // Exactly one cell is copied with new notes
            val updatedCells = state.cells.mapIndexed { idx, cell ->
                if (idx == targetIdx) cell.copy(notes = newNotes) else cell
            }

            val action = MoveAction.ToggleNote(row, col, digit, wasAdded)
            undoStack.add(action)
            redoStack.clear()

            _uiState.update {
                it.copy(
                    cells = updatedCells,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
            }
            persistCurrentSession()
        } else {
            // Normal Digit Entry: Update ONLY the target cell's value
            val oldVal = targetCell.value
            val newVal = if (oldVal == digit) 0 else digit // toggle erase if same digit
            val oldNotes = targetCell.notes
            val newNotes = emptySet<Int>()

            val affectedNotes = mutableMapOf<Pair<Int, Int>, Set<Int>>()

            // If auto-clear notes enabled and placing a digit (not erasing):
            // Remove conflicting pencil marks only; never insert confirmed digits into other cells.
            val targetBox = SudokuSolver.box(targetIdx)
            if (newVal in 1..9 && state.preferences.autoClearNotes) {
                for (i in 0 until 81) {
                    if (i != targetIdx && state.cells[i].value == 0 && state.cells[i].notes.contains(newVal)) {
                        val cellR = SudokuSolver.row(i)
                        val cellC = SudokuSolver.col(i)
                        val cellB = SudokuSolver.box(i)
                        if (cellR == row || cellC == col || cellB == targetBox) {
                            affectedNotes[Pair(cellR, cellC)] = state.cells[i].notes
                        }
                    }
                }
            }

            // Check mistake on target cell only
            var newMistakes = state.mistakeCount
            if (newVal in 1..9 && state.gameMode != GameMode.PRACTICE) {
                val correctDigit = state.solution[targetIdx]
                if (newVal != correctDigit) {
                    newMistakes++
                }
            }

            // Immutable list update: strictly update target cell's value.
            // Peer candidate notes are pruned if auto-clear is enabled. No other cell's value is modified.
            val updatedCells = state.cells.mapIndexed { idx, cell ->
                when {
                    idx == targetIdx -> cell.copy(value = newVal, notes = newNotes)
                    affectedNotes.containsKey(Pair(cell.row, cell.col)) -> cell.copy(notes = cell.notes - newVal)
                    else -> cell
                }
            }

            // Compute conflicts using getConflictsForGrid
            val conflictIndices = getConflictsForGrid(updatedCells)
            val cellsWithConflicts = updatedCells.mapIndexed { idx, cell ->
                cell.copy(isConflict = conflictIndices.contains(idx))
            }

            val action = MoveAction.SetDigit(row, col, oldVal, newVal, oldNotes, newNotes, affectedNotes)
            undoStack.add(action)
            redoStack.clear()

            _uiState.update {
                it.copy(
                    cells = cellsWithConflicts,
                    mistakeCount = newMistakes,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
            }

            checkVictory(cellsWithConflicts)
            persistCurrentSession()
        }
    }

    /**
     * Pure query: returns the set of cell indices that currently have the given digit.
     * Never modifies any cell value or game state.
     */
    fun getMatchingCells(digit: Int): Set<Int> {
        if (digit !in 1..9) return emptySet()
        val matching = mutableSetOf<Int>()
        val cells = _uiState.value.cells
        for (i in cells.indices) {
            if (cells[i].value == digit) {
                matching.add(i)
            }
        }
        return matching
    }

    /**
     * Pure query: checks Sudoku rule violations for placing `digit` at (row, col)
     * without modifying the game state or filling any cells.
     */
    fun getConflicts(row: Int, col: Int, digit: Int): Set<Int> {
        if (digit !in 1..9 || row !in 0..8 || col !in 0..8) return emptySet()
        val conflicts = mutableSetOf<Int>()
        val targetIdx = SudokuSolver.index(row, col)
        val targetBox = (row / 3) * 3 + (col / 3)
        val cells = _uiState.value.cells

        for (i in cells.indices) {
            if (i != targetIdx && cells[i].value == digit) {
                val r = cells[i].row
                val c = cells[i].col
                val b = (r / 3) * 3 + (c / 3)
                if (r == row || c == col || b == targetBox) {
                    conflicts.add(i)
                    conflicts.add(targetIdx)
                }
            }
        }
        return conflicts
    }

    private fun getConflictsForGrid(cells: List<SudokuCell>): Set<Int> {
        val currentGrid = cells.map { it.value }.toIntArray()
        return SudokuSolver.findConflictIndices(currentGrid)
    }

    fun eraseCell() {
        val state = _uiState.value
        if (state.isPaused || state.isVictorious) return
        val r = state.selectedRow
        val c = state.selectedCol
        if (r !in 0..8 || c !in 0..8) return

        val idx = SudokuSolver.index(r, c)
        val cell = state.cells[idx]
        if (cell.isClue || (cell.value == 0 && cell.notes.isEmpty())) return

        val action = MoveAction.Erase(r, c, cell.value, cell.notes)
        undoStack.add(action)
        redoStack.clear()

        val updatedCells = state.cells.toMutableList()
        updatedCells[idx] = cell.copy(value = 0, notes = emptySet(), isConflict = false)

        val currentGrid = updatedCells.map { it.value }.toIntArray()
        val conflictIndices = SudokuSolver.findConflictIndices(currentGrid)
        val cellsWithConflicts = updatedCells.mapIndexed { i, cItem ->
            cItem.copy(isConflict = conflictIndices.contains(i))
        }

        _uiState.update {
            it.copy(
                cells = cellsWithConflicts,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        persistCurrentSession()
    }

    fun toggleNotesMode() {
        _uiState.update { it.copy(isNotesMode = !it.isNotesMode) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val action = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(action)

        val state = _uiState.value
        val updatedCells = state.cells.toMutableList()

        when (action) {
            is MoveAction.SetDigit -> {
                val idx = SudokuSolver.index(action.row, action.col)
                updatedCells[idx] = updatedCells[idx].copy(
                    value = action.oldDigit,
                    notes = action.oldNotes
                )
                // Restore affected peer notes
                for ((coord, oldPeerNotes) in action.affectedNotes) {
                    val peerIdx = SudokuSolver.index(coord.first, coord.second)
                    updatedCells[peerIdx] = updatedCells[peerIdx].copy(notes = oldPeerNotes)
                }
            }
            is MoveAction.ToggleNote -> {
                val idx = SudokuSolver.index(action.row, action.col)
                val notes = updatedCells[idx].notes.toMutableSet()
                if (action.wasAdded) {
                    notes.remove(action.digit)
                } else {
                    notes.add(action.digit)
                }
                updatedCells[idx] = updatedCells[idx].copy(notes = notes)
            }
            is MoveAction.Erase -> {
                val idx = SudokuSolver.index(action.row, action.col)
                updatedCells[idx] = updatedCells[idx].copy(
                    value = action.oldDigit,
                    notes = action.oldNotes
                )
            }
        }

        val currentGrid = updatedCells.map { it.value }.toIntArray()
        val conflictIndices = SudokuSolver.findConflictIndices(currentGrid)
        val cellsWithConflicts = updatedCells.mapIndexed { i, cItem ->
            cItem.copy(isConflict = conflictIndices.contains(i))
        }

        _uiState.update {
            it.copy(
                cells = cellsWithConflicts,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        persistCurrentSession()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val action = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(action)

        val state = _uiState.value
        val updatedCells = state.cells.toMutableList()

        when (action) {
            is MoveAction.SetDigit -> {
                val idx = SudokuSolver.index(action.row, action.col)
                updatedCells[idx] = updatedCells[idx].copy(
                    value = action.newDigit,
                    notes = action.newNotes
                )
                if (action.newDigit in 1..9 && state.preferences.autoClearNotes) {
                    for ((coord, _) in action.affectedNotes) {
                        val peerIdx = SudokuSolver.index(coord.first, coord.second)
                        updatedCells[peerIdx] = updatedCells[peerIdx].copy(
                            notes = updatedCells[peerIdx].notes - action.newDigit
                        )
                    }
                }
            }
            is MoveAction.ToggleNote -> {
                val idx = SudokuSolver.index(action.row, action.col)
                val notes = updatedCells[idx].notes.toMutableSet()
                if (action.wasAdded) {
                    notes.add(action.digit)
                } else {
                    notes.remove(action.digit)
                }
                updatedCells[idx] = updatedCells[idx].copy(notes = notes)
            }
            is MoveAction.Erase -> {
                val idx = SudokuSolver.index(action.row, action.col)
                updatedCells[idx] = updatedCells[idx].copy(value = 0, notes = emptySet())
            }
        }

        val currentGrid = updatedCells.map { it.value }.toIntArray()
        val conflictIndices = SudokuSolver.findConflictIndices(currentGrid)
        val cellsWithConflicts = updatedCells.mapIndexed { i, cItem ->
            cItem.copy(isConflict = conflictIndices.contains(i))
        }

        _uiState.update {
            it.copy(
                cells = cellsWithConflicts,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        persistCurrentSession()
    }

    fun requestHint() {
        val state = _uiState.value
        if (state.isPaused || state.isVictorious) return

        val currentGrid = state.cells.map { it.value }.toIntArray()
        val hint = SudokuHintEngine.findBestHint(
            currentGrid,
            state.solution,
            state.selectedRow,
            state.selectedCol
        ) ?: return

        val targetIdx = SudokuSolver.index(hint.row, hint.col)
        val updatedCells = state.cells.mapIndexed { i, cell ->
            cell.copy(isHintTarget = (i == targetIdx))
        }

        _uiState.update {
            it.copy(
                activeHint = hint,
                hintStep = 1, // Start with Nudge
                hintsUsed = it.hintsUsed + 1,
                selectedRow = hint.row,
                selectedCol = hint.col,
                cells = updatedCells
            )
        }
    }

    fun advanceHint() {
        val currentStep = _uiState.value.hintStep
        if (currentStep == 1) {
            // Nudge -> Technique
            _uiState.update { it.copy(hintStep = 2) }
        } else if (currentStep == 2) {
            // Technique -> Reveal
            val hint = _uiState.value.activeHint ?: return
            _uiState.update { it.copy(hintStep = 3) }
            // Automatically fill the correct digit
            revealHintDigit(hint.row, hint.col, hint.correctDigit)
        }
    }

    fun dismissHint() {
        _uiState.update {
            it.copy(
                activeHint = null,
                hintStep = 0,
                cells = it.cells.map { c -> c.copy(isHintTarget = false) }
            )
        }
    }

    private fun revealHintDigit(r: Int, c: Int, digit: Int) {
        val state = _uiState.value
        val idx = SudokuSolver.index(r, c)
        val cell = state.cells[idx]
        if (cell.isClue) return

        val oldVal = cell.value
        val oldNotes = cell.notes
        val updatedCells = state.cells.toMutableList()
        updatedCells[idx] = cell.copy(value = digit, notes = emptySet(), isHintTarget = false)

        val action = MoveAction.SetDigit(r, c, oldVal, digit, oldNotes, emptySet())
        undoStack.add(action)
        redoStack.clear()

        val currentGrid = updatedCells.map { it.value }.toIntArray()
        val conflictIndices = SudokuSolver.findConflictIndices(currentGrid)
        val cellsWithConflicts = updatedCells.mapIndexed { i, cItem ->
            cItem.copy(isConflict = conflictIndices.contains(i))
        }

        _uiState.update {
            it.copy(
                cells = cellsWithConflicts,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        checkVictory(cellsWithConflicts)
        persistCurrentSession()
    }

    fun startNewGame(difficulty: Difficulty, mode: GameMode = GameMode.CLASSIC) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showNewGameDialog = false, isVictorious = false) }
            undoStack.clear()
            redoStack.clear()

            currentSeed = System.currentTimeMillis()
            val puzzle = SudokuGenerator.generatePuzzle(difficulty, seed = currentSeed)
            initializeBoardWithPuzzle(puzzle, difficulty, mode)
        }
    }

    fun startDailyChallenge() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showNewGameDialog = false, isVictorious = false) }
            undoStack.clear()
            redoStack.clear()

            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val dateInt = dateFormat.format(Date()).toIntOrNull() ?: 20261011
            currentSeed = dateInt.toLong()

            val puzzle = SudokuGenerator.generateDailyChallenge(dateInt)
            initializeBoardWithPuzzle(puzzle, Difficulty.MEDIUM, GameMode.DAILY)
        }
    }

    fun restartCurrentGame() {
        val state = _uiState.value
        undoStack.clear()
        redoStack.clear()

        val restoredCells = state.cells.map { cell ->
            if (cell.isClue) cell else cell.copy(value = 0, notes = emptySet(), isConflict = false, isHintTarget = false)
        }

        _uiState.update {
            it.copy(
                cells = restoredCells,
                elapsedSeconds = 0L,
                mistakeCount = 0,
                hintsUsed = 0,
                isVictorious = false,
                canUndo = false,
                canRedo = false,
                isPaused = false,
                activeHint = null,
                hintStep = 0
            )
        }
        startTimer()
        persistCurrentSession()
    }

    private fun initializeBoardWithPuzzle(
        puzzle: com.example.model.GeneratedPuzzle,
        difficulty: Difficulty,
        mode: GameMode
    ) {
        val cells = List(81) { i ->
            val clue = puzzle.clues[i]
            SudokuCell(
                row = SudokuSolver.row(i),
                col = SudokuSolver.col(i),
                value = clue,
                isClue = clue != 0,
                notes = emptySet()
            )
        }

        _uiState.update {
            it.copy(
                cells = cells,
                solution = puzzle.solution,
                selectedRow = -1,
                selectedCol = -1,
                difficulty = difficulty,
                gameMode = mode,
                elapsedSeconds = 0L,
                isPaused = false,
                mistakeCount = 0,
                hintsUsed = 0,
                isVictorious = false,
                canUndo = false,
                canRedo = false,
                activeHint = null,
                hintStep = 0,
                isLoading = false
            )
        }

        startTimer()
        persistCurrentSession()
    }

    private fun checkVictory(cells: List<SudokuCell>) {
        val isFull = cells.all { it.value in 1..9 }
        if (!isFull) return

        val state = _uiState.value
        val hasConflicts = cells.any { it.isConflict }
        val matchesSolution = cells.indices.all { i -> cells[i].value == state.solution[i] }

        if (!hasConflicts && matchesSolution) {
            timerJob?.cancel()
            _uiState.update { it.copy(isVictorious = true) }

            viewModelScope.launch {
                repository.recordGameResult(
                    difficulty = state.difficulty.name,
                    won = true,
                    timeSeconds = state.elapsedSeconds,
                    hints = state.hintsUsed,
                    mistakes = state.mistakeCount,
                    isDailyChallenge = (state.gameMode == GameMode.DAILY)
                )
                repository.clearSession()
            }
        }
    }

    fun togglePause() {
        val newPaused = !_uiState.value.isPaused
        _uiState.update { it.copy(isPaused = newPaused) }
    }

    fun setShowNewGameDialog(show: Boolean) {
        _uiState.update { it.copy(showNewGameDialog = show) }
    }

    fun setShowStatsDialog(show: Boolean) {
        _uiState.update { it.copy(showStatsDialog = show) }
    }

    fun setShowSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun updateAutoClearNotes(value: Boolean) {
        val updated = _uiState.value.preferences.copy(autoClearNotes = value)
        savePrefs(updated)
    }

    fun updateHighlightConflicts(value: Boolean) {
        val updated = _uiState.value.preferences.copy(highlightConflicts = value)
        savePrefs(updated)
    }

    fun updateHighlightMatching(value: Boolean) {
        val updated = _uiState.value.preferences.copy(highlightMatching = value)
        savePrefs(updated)
    }

    fun updateHighlightPeers(value: Boolean) {
        val updated = _uiState.value.preferences.copy(highlightPeers = value)
        savePrefs(updated)
    }

    fun updateDarkTheme(value: Boolean) {
        val updated = _uiState.value.preferences.copy(darkTheme = value)
        savePrefs(updated)
    }

    fun resetStats() {
        viewModelScope.launch {
            repository.resetAllStats()
        }
    }

    private fun savePrefs(prefs: UserPreferencesEntity) {
        _uiState.update { it.copy(preferences = prefs) }
        viewModelScope.launch {
            repository.savePreferences(prefs)
        }
    }

    private fun persistCurrentSession() {
        val state = _uiState.value
        if (state.isLoading || state.cells.size != 81) return

        val boardStr = state.cells.joinToString("") { it.value.toString() }
        val cluesStr = state.cells.joinToString("") { if (it.isClue) it.value.toString() else "0" }
        val solStr = state.solution.joinToString("")

        // Encode notes: "0:1,2;5:3,9"
        val notesStr = state.cells.mapIndexedNotNull { i, c ->
            if (c.notes.isNotEmpty()) "$i:" + c.notes.sorted().joinToString(",") else null
        }.joinToString(";")

        val session = GameSessionEntity(
            id = 1,
            mode = state.gameMode.name,
            difficulty = state.difficulty.name,
            boardData = boardStr,
            cluesData = cluesStr,
            solutionData = solStr,
            notesData = notesStr,
            elapsedSeconds = state.elapsedSeconds,
            mistakeCount = state.mistakeCount,
            hintsCount = state.hintsUsed,
            isCompleted = state.isVictorious,
            seed = currentSeed
        )

        viewModelScope.launch {
            repository.saveSession(session)
        }
    }

    private fun restoreSession(session: GameSessionEntity) {
        val diff = try { Difficulty.valueOf(session.difficulty) } catch (e: Exception) { Difficulty.EASY }
        val mode = try { GameMode.valueOf(session.mode) } catch (e: Exception) { GameMode.CLASSIC }

        val notesMap = mutableMapOf<Int, Set<Int>>()
        if (session.notesData.isNotBlank()) {
            session.notesData.split(";").forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val idx = parts[0].toIntOrNull()
                    val set = parts[1].split(",").mapNotNull { it.toIntOrNull() }.toSet()
                    if (idx != null) notesMap[idx] = set
                }
            }
        }

        val cells = List(81) { i ->
            val v = session.boardData.getOrNull(i)?.digitToIntOrNull() ?: 0
            val clue = session.cluesData.getOrNull(i)?.digitToIntOrNull() ?: 0
            SudokuCell(
                row = SudokuSolver.row(i),
                col = SudokuSolver.col(i),
                value = v,
                isClue = clue != 0,
                notes = notesMap[i] ?: emptySet()
            )
        }

        val sol = IntArray(81) { i ->
            session.solutionData.getOrNull(i)?.digitToIntOrNull() ?: 0
        }

        val currentGrid = cells.map { it.value }.toIntArray()
        val conflictIndices = SudokuSolver.findConflictIndices(currentGrid)
        val cellsWithConflicts = cells.mapIndexed { i, cItem ->
            cItem.copy(isConflict = conflictIndices.contains(i))
        }

        currentSeed = session.seed

        _uiState.update {
            it.copy(
                cells = cellsWithConflicts,
                solution = sol,
                difficulty = diff,
                gameMode = mode,
                elapsedSeconds = session.elapsedSeconds,
                mistakeCount = session.mistakeCount,
                hintsUsed = session.hintsCount,
                isVictorious = false,
                isLoading = false
            )
        }
        startTimer()
    }
}
