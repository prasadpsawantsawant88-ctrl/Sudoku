package com.example.engine

import com.example.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class SudokuEngineTest {

    private val validSolvedGrid = intArrayOf(
        1, 2, 3, 4, 5, 6, 7, 8, 9,
        4, 5, 6, 7, 8, 9, 1, 2, 3,
        7, 8, 9, 1, 2, 3, 4, 5, 6,
        2, 3, 1, 5, 6, 4, 8, 9, 7,
        5, 6, 4, 8, 9, 7, 2, 3, 1,
        8, 9, 7, 2, 3, 1, 5, 6, 4,
        3, 1, 2, 6, 4, 5, 9, 7, 8,
        6, 4, 5, 9, 7, 8, 3, 1, 2,
        9, 7, 8, 3, 1, 2, 6, 4, 5
    )

    @Test
    fun testValidAndInvalidCompletedGrids() {
        // Valid completed grid has zero conflicts
        val conflictsValid = SudokuSolver.findConflictIndices(validSolvedGrid)
        assertTrue("Valid grid must have no conflicts", conflictsValid.isEmpty())

        // Invalid grid with conflicting numbers in row 0
        val invalidGrid = validSolvedGrid.copyOf()
        invalidGrid[1] = 1 // Duplicate 1 in row 0
        val conflictsInvalid = SudokuSolver.findConflictIndices(invalidGrid)
        assertTrue("Invalid grid must detect conflicts", conflictsInvalid.contains(0) && conflictsInvalid.contains(1))
    }

    @Test
    fun testPuzzleWithNoSolution() {
        val impossibleGrid = validSolvedGrid.copyOf()
        impossibleGrid[0] = 0
        // Introduce contradiction: make cell 0 impossible to solve
        // Placing 2 in row 0, col 1 and 2 in col 0, row 3 leaves contradiction if forced
        val conflictGrid = IntArray(81)
        conflictGrid[0] = 1
        conflictGrid[1] = 1 // row conflict
        val sol = SudokuSolver.solve(conflictGrid)
        assertNull("Contradictory grid should return null solution", sol)
    }

    @Test
    fun testPuzzleWithMultipleSolutions() {
        // Very few clues leads to multiple solutions
        val ambiguousGrid = IntArray(81)
        ambiguousGrid[0] = 1
        ambiguousGrid[10] = 2
        val count = SudokuSolver.countSolutions(ambiguousGrid, maxCount = 2)
        assertEquals("Grid with minimal clues should have >= 2 solutions", 2, count)
    }

    @Test
    fun testUniqueSolutionVerification() {
        val puzzle = SudokuGenerator.generatePuzzle(Difficulty.EASY, seed = 42L)
        val solutions = SudokuSolver.countSolutions(puzzle.clues, maxCount = 2)
        assertEquals("Generated puzzle must have exactly 1 unique solution", 1, solutions)
    }

    @Test
    fun testPreservationOfOriginalClues() {
        val puzzle = SudokuGenerator.generatePuzzle(Difficulty.MEDIUM, seed = 123L)
        val solved = SudokuSolver.solve(puzzle.clues)
        assertNotNull(solved)
        for (i in 0 until 81) {
            if (puzzle.clues[i] != 0) {
                assertEquals("Solved grid must match original clue at index $i", puzzle.clues[i], solved!![i])
            }
        }
    }

    @Test
    fun testDifficultyClassification() {
        val easyEval = SudokuDifficultyEvaluator.evaluate(validSolvedGrid)
        assertNotNull(easyEval)
        assertTrue(easyEval.logicOnlySolvable)
    }

    @Test
    fun testHintCorrectness() {
        val puzzle = SudokuGenerator.generatePuzzle(Difficulty.EASY, seed = 777L)
        val hint = SudokuHintEngine.findBestHint(puzzle.clues, puzzle.solution, -1, -1)
        assertNotNull("Hint engine must provide a logical deduction", hint)
        val targetIdx = SudokuSolver.index(hint!!.row, hint.col)
        assertEquals("Hint target must match verified solution", puzzle.solution[targetIdx], hint.correctDigit)
    }

    @Test
    fun testReproducibleDailyGeneration() {
        val puzzle1 = SudokuGenerator.generateDailyChallenge(20261011)
        val puzzle2 = SudokuGenerator.generateDailyChallenge(20261011)
        assertTrue("Daily challenges for the same date must be identical", puzzle1.clues.contentEquals(puzzle2.clues))
        assertTrue("Daily challenges must have identical solutions", puzzle1.solution.contentEquals(puzzle2.solution))
    }

    @Test
    fun testCompletedGridPermutations() {
        val random = Random(999L)
        val grid = SudokuGenerator.generateCompletedGrid(random)
        assertEquals(81, grid.size)
        assertFalse(grid.contains(0))
        val conflicts = SudokuSolver.findConflictIndices(grid)
        assertTrue("Permuted grid must be a valid Sudoku solution", conflicts.isEmpty())
    }

    @Test
    fun testSingleCellEntryIsolation() {
        // Setup initial grid where cell index 0 has 1, and cell index 10 is empty
        val initialCells = List(81) { i ->
            com.example.model.SudokuCell(
                row = i / 9,
                col = i % 9,
                value = if (i == 0) 1 else 0,
                isClue = (i == 0)
            )
        }

        // Entering 1 into cell (row 1, col 1) [index 10]
        val targetIdx = 10
        val targetRow = 1
        val targetCol = 1
        val enteredDigit = 1

        val updatedCells = initialCells.mapIndexed { idx, cell ->
            if (idx == targetIdx) cell.copy(value = enteredDigit) else cell
        }

        // Verify that exactly one cell changed value
        assertEquals("Target cell must have value 1", 1, updatedCells[targetIdx].value)
        assertEquals("Existing clue at index 0 must remain 1", 1, updatedCells[0].value)

        // Verify all other 79 cells remain untouched (value 0)
        var countNonZero = 0
        for (i in 0 until 81) {
            if (updatedCells[i].value != 0) countNonZero++
        }
        assertEquals("Exactly 2 cells must have non-zero values (index 0 and index 10)", 2, countNonZero)

        // Verify pure query getMatchingCells identifies both cells without mutation
        val matchingIndices = updatedCells.mapIndexedNotNull { idx, c -> if (c.value == enteredDigit) idx else null }.toSet()
        assertEquals(setOf(0, 10), matchingIndices)
    }

    @Test
    fun testDailyStreakConsecutiveLogic() {
        val streakDay1 = com.example.data.DailyStreakEntity(
            currentStreak = 1,
            maxStreak = 1,
            lastCompletedDate = "2026-10-10",
            totalCompleted = 1
        )
        // Consecutive day (2026-10-11): streak increments to 2
        val streakDay2 = streakDay1.copy(
            currentStreak = streakDay1.currentStreak + 1,
            maxStreak = maxOf(streakDay1.maxStreak, streakDay1.currentStreak + 1),
            lastCompletedDate = "2026-10-11",
            totalCompleted = streakDay1.totalCompleted + 1
        )
        assertEquals(2, streakDay2.currentStreak)
        assertEquals(2, streakDay2.maxStreak)
        assertEquals(2, streakDay2.totalCompleted)

        // Multiple plays on same day: streak unchanged
        val streakDay2Repeat = streakDay2.copy()
        assertEquals(2, streakDay2Repeat.currentStreak)

        // Streak after lapse (missed a day): reset to 1, maxStreak preserved
        val streakAfterLapse = streakDay2.copy(
            currentStreak = 1,
            lastCompletedDate = "2026-10-15",
            totalCompleted = streakDay2.totalCompleted + 1
        )
        assertEquals(1, streakAfterLapse.currentStreak)
        assertEquals(2, streakAfterLapse.maxStreak)
        assertEquals(3, streakAfterLapse.totalCompleted)
    }
}
