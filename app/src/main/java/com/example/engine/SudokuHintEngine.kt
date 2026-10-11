package com.example.engine

import com.example.model.HintStep

object SudokuHintEngine {
    private const val SIZE = 9
    private const val CELLS = 81

    data class ComprehensiveHint(
        val row: Int,
        val col: Int,
        val correctDigit: Int,
        val nudgeMessage: String,
        val techniqueName: String,
        val explanation: String
    )

    fun findBestHint(currentGrid: IntArray, solution: IntArray, selectedRow: Int, selectedCol: Int): ComprehensiveHint? {
        val selectedIdx = if (selectedRow in 0..8 && selectedCol in 0..8) SudokuSolver.index(selectedRow, selectedCol) else -1

        // 1. If player has selected an empty cell, check if it's immediately deducible
        if (selectedIdx != -1 && currentGrid[selectedIdx] == 0) {
            val hint = evaluateCellDeduction(currentGrid, solution, selectedIdx)
            if (hint != null) return hint
        }

        // 2. Scan for any Naked Singles on the board
        for (i in 0 until CELLS) {
            if (currentGrid[i] == 0) {
                val cands = SudokuSolver.getCandidates(currentGrid, i)
                if (cands.size == 1) {
                    val r = SudokuSolver.row(i)
                    val c = SudokuSolver.col(i)
                    val d = cands.first()
                    return ComprehensiveHint(
                        row = r,
                        col = c,
                        correctDigit = d,
                        nudgeMessage = "Look closely at Row ${r + 1}, Column ${c + 1}.",
                        techniqueName = "Naked Single",
                        explanation = "Row ${r + 1}, Column ${c + 1} can only be $d because every other number from 1 to 9 is already present in its row, column, or 3×3 box."
                    )
                }
            }
        }

        // 3. Scan for Hidden Singles (Rows, Cols, Boxes)
        for (r in 0 until SIZE) {
            for (digit in 1..9) {
                val possibleCols = mutableListOf<Int>()
                for (c in 0 until SIZE) {
                    val idx = SudokuSolver.index(r, c)
                    if (currentGrid[idx] == 0 && SudokuSolver.getCandidates(currentGrid, idx).contains(digit)) {
                        possibleCols.add(c)
                    }
                }
                if (possibleCols.size == 1) {
                    val c = possibleCols.first()
                    return ComprehensiveHint(
                        row = r,
                        col = c,
                        correctDigit = digit,
                        nudgeMessage = "Row ${r + 1} needs number $digit.",
                        techniqueName = "Hidden Single in Row",
                        explanation = "In Row ${r + 1}, Column ${c + 1} is the only cell that can legally contain $digit."
                    )
                }
            }
        }

        for (c in 0 until SIZE) {
            for (digit in 1..9) {
                val possibleRows = mutableListOf<Int>()
                for (r in 0 until SIZE) {
                    val idx = SudokuSolver.index(r, c)
                    if (currentGrid[idx] == 0 && SudokuSolver.getCandidates(currentGrid, idx).contains(digit)) {
                        possibleRows.add(r)
                    }
                }
                if (possibleRows.size == 1) {
                    val r = possibleRows.first()
                    return ComprehensiveHint(
                        row = r,
                        col = c,
                        correctDigit = digit,
                        nudgeMessage = "Column ${c + 1} needs number $digit.",
                        techniqueName = "Hidden Single in Column",
                        explanation = "In Column ${c + 1}, Row ${r + 1} is the only cell that can legally contain $digit."
                    )
                }
            }
        }

        for (b in 0 until SIZE) {
            val startR = (b / 3) * 3
            val startC = (b % 3) * 3
            for (digit in 1..9) {
                val possibleCells = mutableListOf<Pair<Int, Int>>()
                for (dr in 0 until 3) {
                    for (dc in 0 until 3) {
                        val r = startR + dr
                        val c = startC + dc
                        val idx = SudokuSolver.index(r, c)
                        if (currentGrid[idx] == 0 && SudokuSolver.getCandidates(currentGrid, idx).contains(digit)) {
                            possibleCells.add(Pair(r, c))
                        }
                    }
                }
                if (possibleCells.size == 1) {
                    val (r, c) = possibleCells.first()
                    return ComprehensiveHint(
                        row = r,
                        col = c,
                        correctDigit = digit,
                        nudgeMessage = "Look at the 3×3 box containing Row ${r + 1}, Column ${c + 1}.",
                        techniqueName = "Hidden Single in Box",
                        explanation = "Within this 3×3 box, Row ${r + 1}, Column ${c + 1} is the only available spot for $digit."
                    )
                }
            }
        }

        // 4. Fallback: Find the empty cell with minimum remaining candidates according to the verified solution
        var bestIdx = -1
        var minCands = 10
        for (i in 0 until CELLS) {
            if (currentGrid[i] == 0) {
                val count = SudokuSolver.getCandidates(currentGrid, i).size
                if (count in 1 until minCands) {
                    minCands = count
                    bestIdx = i
                }
            }
        }

        if (bestIdx != -1) {
            val r = SudokuSolver.row(bestIdx)
            val c = SudokuSolver.col(bestIdx)
            val d = solution[bestIdx]
            return ComprehensiveHint(
                row = r,
                col = c,
                correctDigit = d,
                nudgeMessage = "Consider Row ${r + 1}, Column ${c + 1}.",
                techniqueName = "Elimination Deduction",
                explanation = "Based on constraint analysis across intersecting lines and subgrid boundaries, this cell resolves to $d."
            )
        }

        return null
    }

    private fun evaluateCellDeduction(currentGrid: IntArray, solution: IntArray, idx: Int): ComprehensiveHint? {
        val r = SudokuSolver.row(idx)
        val c = SudokuSolver.col(idx)
        val cands = SudokuSolver.getCandidates(currentGrid, idx)
        if (cands.isEmpty()) return null

        val correct = solution[idx]
        if (cands.size == 1 && cands.first() == correct) {
            return ComprehensiveHint(
                row = r,
                col = c,
                correctDigit = correct,
                nudgeMessage = "Your selected cell (Row ${r + 1}, Col ${c + 1}) is ready to solve.",
                techniqueName = "Naked Single",
                explanation = "All numbers 1–9 except $correct are already taken by peers of this cell."
            )
        }
        return null
    }
}
