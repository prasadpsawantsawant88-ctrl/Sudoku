package com.example.engine

import com.example.model.Difficulty
import com.example.model.DifficultyScore

object SudokuDifficultyEvaluator {
    private const val SIZE = 9
    private const val CELLS = 81

    data class EvaluationResult(
        val difficulty: Difficulty,
        val score: Double,
        val nakedSingles: Int,
        val hiddenSingles: Int,
        val pointingPairs: Int,
        val nakedPairs: Int,
        val searchNodes: Int,
        val maxDepth: Int,
        val logicOnlySolvable: Boolean
    )

    fun evaluate(initialGrid: IntArray): DifficultyScore {
        val grid = initialGrid.copyOf()
        // Candidates map for each empty cell
        val candidates = Array(CELLS) { mutableSetOf<Int>() }

        fun refreshCandidates() {
            for (i in 0 until CELLS) {
                if (grid[i] == 0) {
                    candidates[i].clear()
                    candidates[i].addAll(SudokuSolver.getCandidates(grid, i))
                } else {
                    candidates[i].clear()
                }
            }
        }

        refreshCandidates()

        var nakedSingles = 0
        var hiddenSingles = 0
        var pointingPairs = 0
        var nakedPairs = 0

        // Step 1: Logical deduction loop
        var progress = true
        while (progress && grid.any { it == 0 }) {
            progress = false

            // 1. Naked singles
            for (i in 0 until CELLS) {
                if (grid[i] == 0 && candidates[i].size == 1) {
                    val digit = candidates[i].first()
                    grid[i] = digit
                    nakedSingles++
                    refreshCandidates()
                    progress = true
                    break
                }
            }
            if (progress) continue

            // 2. Hidden singles
            // Check rows, cols, boxes
            var hiddenFound = false
            for (unitType in 0..2) {
                for (unitIndex in 0 until SIZE) {
                    val unitCells = getUnitIndices(unitType, unitIndex)
                    for (digit in 1..9) {
                        val possibleCells = unitCells.filter { grid[it] == 0 && candidates[it].contains(digit) }
                        if (possibleCells.size == 1) {
                            val targetCell = possibleCells.first()
                            grid[targetCell] = digit
                            hiddenSingles++
                            refreshCandidates()
                            progress = true
                            hiddenFound = true
                            break
                        }
                    }
                    if (hiddenFound) break
                }
                if (hiddenFound) break
            }
            if (progress) continue

            // 3. Pointing Pairs / Box-Line Reductions
            for (box in 0 until SIZE) {
                val boxCells = getUnitIndices(2, box)
                for (digit in 1..9) {
                    val cellsWithDigit = boxCells.filter { grid[it] == 0 && candidates[it].contains(digit) }
                    if (cellsWithDigit.size in 2..3) {
                        val rows = cellsWithDigit.map { SudokuSolver.row(it) }.toSet()
                        val cols = cellsWithDigit.map { SudokuSolver.col(it) }.toSet()
                        if (rows.size == 1) {
                            val r = rows.first()
                            val rowCells = getUnitIndices(0, r)
                            var eliminated = false
                            for (cIdx in rowCells) {
                                if (cIdx !in boxCells && candidates[cIdx].remove(digit)) {
                                    eliminated = true
                                }
                            }
                            if (eliminated) {
                                pointingPairs++
                                progress = true
                                break
                            }
                        } else if (cols.size == 1) {
                            val c = cols.first()
                            val colCells = getUnitIndices(1, c)
                            var eliminated = false
                            for (rIdx in colCells) {
                                if (rIdx !in boxCells && candidates[rIdx].remove(digit)) {
                                    eliminated = true
                                }
                            }
                            if (eliminated) {
                                pointingPairs++
                                progress = true
                                break
                            }
                        }
                    }
                }
                if (progress) break
            }
            if (progress) continue

            // 4. Naked Pairs
            for (unitType in 0..2) {
                for (unitIndex in 0 until SIZE) {
                    val unitCells = getUnitIndices(unitType, unitIndex).filter { grid[it] == 0 }
                    val pairs = unitCells.filter { candidates[it].size == 2 }
                    if (pairs.size >= 2) {
                        for (i in 0 until pairs.size - 1) {
                            for (j in i + 1 until pairs.size) {
                                if (candidates[pairs[i]] == candidates[pairs[j]]) {
                                    val pairDigits = candidates[pairs[i]]
                                    var eliminated = false
                                    for (other in unitCells) {
                                        if (other != pairs[i] && other != pairs[j]) {
                                            if (candidates[other].removeAll(pairDigits)) {
                                                eliminated = true
                                            }
                                        }
                                    }
                                    if (eliminated) {
                                        nakedPairs++
                                        progress = true
                                        break
                                    }
                                }
                            }
                            if (progress) break
                        }
                    }
                    if (progress) break
                }
                if (progress) break
            }
        }

        val logicOnlySolvable = grid.all { it != 0 }
        var searchNodes = 0
        var maxDepth = 0

        if (!logicOnlySolvable) {
            // Measure search depth and branching using backtracking simulation
            val stats = measureSearchComplexity(grid)
            searchNodes = stats.first
            maxDepth = stats.second
        }

        // Weighted feature scoring formula S(P) = \sum w_i f_i(P)
        val score = (1.0 * nakedSingles) +
                (2.5 * hiddenSingles) +
                (12.0 * pointingPairs) +
                (18.0 * nakedPairs) +
                (15.0 * maxDepth) +
                (1.5 * searchNodes)

        val difficulty = when {
            logicOnlySolvable && pointingPairs == 0 && nakedPairs == 0 && score <= 85.0 -> Difficulty.EASY
            logicOnlySolvable && (pointingPairs > 0 || nakedPairs > 0 || score <= 180.0) -> Difficulty.MEDIUM
            maxDepth <= 3 && score <= 320.0 -> Difficulty.HARD
            else -> Difficulty.EXPERT
        }

        return DifficultyScore(
            difficulty = difficulty,
            score = score,
            nakedSingles = nakedSingles,
            hiddenSingles = hiddenSingles,
            pointingPairs = pointingPairs,
            nakedPairs = nakedPairs,
            searchNodes = searchNodes,
            maxBacktrackingDepth = maxDepth,
            logicOnlySolvable = logicOnlySolvable
        )
    }

    private fun getUnitIndices(unitType: Int, unitIndex: Int): List<Int> {
        val result = mutableListOf<Int>()
        when (unitType) {
            0 -> { // Row
                for (c in 0 until SIZE) result.add(SudokuSolver.index(unitIndex, c))
            }
            1 -> { // Col
                for (r in 0 until SIZE) result.add(SudokuSolver.index(r, unitIndex))
            }
            2 -> { // Box
                val startR = (unitIndex / 3) * 3
                val startC = (unitIndex % 3) * 3
                for (r in 0 until 3) {
                    for (c in 0 until 3) {
                        result.add(SudokuSolver.index(startR + r, startC + c))
                    }
                }
            }
        }
        return result
    }

    private fun measureSearchComplexity(grid: IntArray): Pair<Int, Int> {
        var nodes = 0
        var maxDepth = 0
        val working = grid.copyOf()

        fun dfs(depth: Int): Boolean {
            nodes++
            if (depth > maxDepth) maxDepth = depth
            var bestIdx = -1
            var minCands = 10
            for (i in 0 until CELLS) {
                if (working[i] == 0) {
                    val cands = SudokuSolver.getCandidates(working, i)
                    if (cands.isEmpty()) return false
                    if (cands.size < minCands) {
                        minCands = cands.size
                        bestIdx = i
                        if (minCands == 1) break
                    }
                }
            }
            if (bestIdx == -1) return true

            val cands = SudokuSolver.getCandidates(working, bestIdx)
            for (d in cands) {
                working[bestIdx] = d
                if (dfs(depth + 1)) return true
                working[bestIdx] = 0
            }
            return false
        }

        dfs(0)
        return Pair(nodes, maxDepth)
    }
}
