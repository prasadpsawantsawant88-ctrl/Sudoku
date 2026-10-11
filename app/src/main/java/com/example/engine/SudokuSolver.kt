package com.example.engine

object SudokuSolver {
    const val SIZE = 9
    const val CELLS = 81
    private const val ALL_DIGITS_MASK = 0x1FF // bits 0..8 represent digits 1..9

    fun row(index: Int): Int = index / SIZE
    fun col(index: Int): Int = index % SIZE
    fun box(index: Int): Int = (row(index) / 3) * 3 + (col(index) / 3)
    fun index(row: Int, col: Int): Int = row * SIZE + col

    /**
     * Solves the given 81-cell grid.
     * Returns a completed 81-element array, or null if unsolvable.
     */
    fun solve(grid: IntArray): IntArray? {
        val workingGrid = grid.copyOf()
        val rowMask = IntArray(SIZE)
        val colMask = IntArray(SIZE)
        val boxMask = IntArray(SIZE)

        for (i in 0 until CELLS) {
            val v = workingGrid[i]
            if (v in 1..9) {
                val bit = 1 shl (v - 1)
                val r = row(i)
                val c = col(i)
                val b = box(i)
                if ((rowMask[r] and bit) != 0 || (colMask[c] and bit) != 0 || (boxMask[b] and bit) != 0) {
                    return null // Contradiction in initial clues
                }
                rowMask[r] = rowMask[r] or bit
                colMask[c] = colMask[c] or bit
                boxMask[b] = boxMask[b] or bit
            }
        }

        return if (backtrackSolve(workingGrid, rowMask, colMask, boxMask)) workingGrid else null
    }

    private fun backtrackSolve(
        grid: IntArray,
        rowMask: IntArray,
        colMask: IntArray,
        boxMask: IntArray
    ): Boolean {
        var bestIndex = -1
        var minCandidates = 10
        var bestCandidateMask = 0

        // MRV (Minimum Remaining Values) search
        for (i in 0 until CELLS) {
            if (grid[i] == 0) {
                val r = row(i)
                val c = col(i)
                val b = box(i)
                val used = rowMask[r] or colMask[c] or boxMask[b]
                val available = ALL_DIGITS_MASK and used.inv()
                val count = Integer.bitCount(available)
                if (count == 0) return false // Dead end
                if (count < minCandidates) {
                    minCandidates = count
                    bestIndex = i
                    bestCandidateMask = available
                    if (count == 1) break // Naked single found, take immediately
                }
            }
        }

        if (bestIndex == -1) return true // All cells filled successfully

        val r = row(bestIndex)
        val c = col(bestIndex)
        val b = box(bestIndex)

        var mask = bestCandidateMask
        while (mask != 0) {
            val bit = mask and -mask
            val digit = Integer.numberOfTrailingZeros(bit) + 1
            mask = mask and (mask - 1)

            grid[bestIndex] = digit
            rowMask[r] = rowMask[r] or bit
            colMask[c] = colMask[c] or bit
            boxMask[b] = boxMask[b] or bit

            if (backtrackSolve(grid, rowMask, colMask, boxMask)) {
                return true
            }

            // Backtrack
            grid[bestIndex] = 0
            rowMask[r] = rowMask[r] and bit.inv()
            colMask[c] = colMask[c] and bit.inv()
            boxMask[b] = boxMask[b] and bit.inv()
        }

        return false
    }

    /**
     * Counts solutions up to maxCount.
     * Early termination: stops as soon as solutionsFound >= maxCount.
     * Useful to distinguish 0, 1 (unique), or 2+ (ambiguous).
     */
    fun countSolutions(grid: IntArray, maxCount: Int = 2): Int {
        val workingGrid = grid.copyOf()
        val rowMask = IntArray(SIZE)
        val colMask = IntArray(SIZE)
        val boxMask = IntArray(SIZE)

        for (i in 0 until CELLS) {
            val v = workingGrid[i]
            if (v in 1..9) {
                val bit = 1 shl (v - 1)
                val r = row(i)
                val c = col(i)
                val b = box(i)
                if ((rowMask[r] and bit) != 0 || (colMask[c] and bit) != 0 || (boxMask[b] and bit) != 0) {
                    return 0
                }
                rowMask[r] = rowMask[r] or bit
                colMask[c] = colMask[c] or bit
                boxMask[b] = boxMask[b] or bit
            }
        }

        var solutions = 0
        fun backtrack(): Boolean {
            var bestIndex = -1
            var minCandidates = 10
            var bestCandidateMask = 0

            for (i in 0 until CELLS) {
                if (workingGrid[i] == 0) {
                    val r = row(i)
                    val c = col(i)
                    val b = box(i)
                    val used = rowMask[r] or colMask[c] or boxMask[b]
                    val available = ALL_DIGITS_MASK and used.inv()
                    val count = Integer.bitCount(available)
                    if (count == 0) return false
                    if (count < minCandidates) {
                        minCandidates = count
                        bestIndex = i
                        bestCandidateMask = available
                        if (count == 1) break
                    }
                }
            }

            if (bestIndex == -1) {
                solutions++
                return solutions >= maxCount
            }

            val r = row(bestIndex)
            val c = col(bestIndex)
            val b = box(bestIndex)
            var mask = bestCandidateMask

            while (mask != 0) {
                val bit = mask and -mask
                val digit = Integer.numberOfTrailingZeros(bit) + 1
                mask = mask and (mask - 1)

                workingGrid[bestIndex] = digit
                rowMask[r] = rowMask[r] or bit
                colMask[c] = colMask[c] or bit
                boxMask[b] = boxMask[b] or bit

                if (backtrack()) return true

                workingGrid[bestIndex] = 0
                rowMask[r] = rowMask[r] and bit.inv()
                colMask[c] = colMask[c] and bit.inv()
                boxMask[b] = boxMask[b] and bit.inv()
            }
            return false
        }

        backtrack()
        return solutions
    }

    /**
     * Returns valid candidate digits 1..9 for the given cell index.
     */
    fun getCandidates(grid: IntArray, index: Int): List<Int> {
        if (grid[index] != 0) return emptyList()
        val r = row(index)
        val c = col(index)
        val b = box(index)

        var used = 0
        for (i in 0 until CELLS) {
            val v = grid[i]
            if (v != 0 && (row(i) == r || col(i) == c || box(i) == b)) {
                used = used or (1 shl (v - 1))
            }
        }
        val available = ALL_DIGITS_MASK and used.inv()
        val result = mutableListOf<Int>()
        for (digit in 1..9) {
            if ((available and (1 shl (digit - 1))) != 0) {
                result.add(digit)
            }
        }
        return result
    }

    /**
     * Checks if placing digit at (row, col) violates any basic Sudoku rules.
     */
    fun isValidPlacement(grid: IntArray, r: Int, c: Int, digit: Int): Boolean {
        val b = (r / 3) * 3 + (c / 3)
        for (i in 0 until CELLS) {
            val v = grid[i]
            if (v == digit) {
                val cellR = row(i)
                val cellC = col(i)
                val cellB = box(i)
                if (cellR == r || cellC == c || cellB == b) {
                    if (!(cellR == r && cellC == c)) return false
                }
            }
        }
        return true
    }

    /**
     * Finds conflicting cells (duplicates in row, column, or 3x3 block).
     * Returns set of cell indices that are currently in conflict.
     */
    fun findConflictIndices(grid: IntArray): Set<Int> {
        val conflicts = mutableSetOf<Int>()

        // Check rows
        for (r in 0 until SIZE) {
            val seen = mutableMapOf<Int, Int>()
            for (c in 0 until SIZE) {
                val idx = index(r, c)
                val v = grid[idx]
                if (v in 1..9) {
                    if (seen.containsKey(v)) {
                        conflicts.add(seen[v]!!)
                        conflicts.add(idx)
                    } else {
                        seen[v] = idx
                    }
                }
            }
        }

        // Check cols
        for (c in 0 until SIZE) {
            val seen = mutableMapOf<Int, Int>()
            for (r in 0 until SIZE) {
                val idx = index(r, c)
                val v = grid[idx]
                if (v in 1..9) {
                    if (seen.containsKey(v)) {
                        conflicts.add(seen[v]!!)
                        conflicts.add(idx)
                    } else {
                        seen[v] = idx
                    }
                }
            }
        }

        // Check boxes
        for (b in 0 until SIZE) {
            val startR = (b / 3) * 3
            val startC = (b % 3) * 3
            val seen = mutableMapOf<Int, Int>()
            for (dr in 0 until 3) {
                for (dc in 0 until 3) {
                    val idx = index(startR + dr, startC + dc)
                    val v = grid[idx]
                    if (v in 1..9) {
                        if (seen.containsKey(v)) {
                            conflicts.add(seen[v]!!)
                            conflicts.add(idx)
                        } else {
                            seen[v] = idx
                        }
                    }
                }
            }
        }

        return conflicts
    }
}
