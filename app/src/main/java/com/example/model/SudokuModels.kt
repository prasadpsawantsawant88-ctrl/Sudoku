package com.example.model

enum class Difficulty(val label: String, val description: String, val targetClues: Int) {
    EASY("Easy", "Straightforward naked and hidden singles", 38),
    MEDIUM("Medium", "Longer reasoning sequences and basic pairs", 32),
    HARD("Hard", "Advanced logic and deeper deductions", 28),
    EXPERT("Expert", "Complex candidate interactions and branching", 24)
}

enum class GameMode(val title: String) {
    CLASSIC("Classic"),
    DAILY("Daily Challenge"),
    PRACTICE("Practice"),
    TIMED("Timed")
}

data class SudokuCell(
    val row: Int,
    val col: Int,
    val value: Int = 0,
    val isClue: Boolean = false,
    val notes: Set<Int> = emptySet(),
    val isConflict: Boolean = false,
    val isHintTarget: Boolean = false
) {
    val isEmpty: Boolean get() = value == 0
}

data class DifficultyScore(
    val difficulty: Difficulty,
    val score: Double,
    val nakedSingles: Int,
    val hiddenSingles: Int,
    val pointingPairs: Int,
    val nakedPairs: Int,
    val searchNodes: Int,
    val maxBacktrackingDepth: Int,
    val logicOnlySolvable: Boolean
)

data class GeneratedPuzzle(
    val clues: IntArray,       // 81 elements (0 for empty)
    val solution: IntArray,    // 81 elements (1-9)
    val difficultyScore: DifficultyScore,
    val seed: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeneratedPuzzle) return false
        return clues.contentEquals(other.clues) && solution.contentEquals(other.solution) && seed == other.seed
    }

    override fun hashCode(): Int {
        var result = clues.contentHashCode()
        result = 31 * result + solution.contentHashCode()
        result = 31 * result + seed.hashCode()
        return result
    }
}

sealed class MoveAction {
    data class SetDigit(
        val row: Int,
        val col: Int,
        val oldDigit: Int,
        val newDigit: Int,
        val oldNotes: Set<Int>,
        val newNotes: Set<Int>,
        val affectedNotes: Map<Pair<Int, Int>, Set<Int>> = emptyMap()
    ) : MoveAction()

    data class ToggleNote(
        val row: Int,
        val col: Int,
        val digit: Int,
        val wasAdded: Boolean
    ) : MoveAction()

    data class Erase(
        val row: Int,
        val col: Int,
        val oldDigit: Int,
        val oldNotes: Set<Int>
    ) : MoveAction()
}

sealed class HintStep {
    data class Nudge(
        val row: Int,
        val col: Int,
        val message: String
    ) : HintStep()

    data class Technique(
        val row: Int,
        val col: Int,
        val techniqueName: String,
        val explanation: String,
        val targetDigit: Int
    ) : HintStep()

    data class Reveal(
        val row: Int,
        val col: Int,
        val correctDigit: Int,
        val message: String
    ) : HintStep()
}
