# Minimalist Sudoku with Intelligent Puzzle Generation

A complete, distraction-free, production-grade Sudoku application built with Jetpack Compose, an algorithmic Constraint Satisfaction Problem (CSP) puzzle generator with guaranteed unique solutions, an intelligent difficulty classification engine, logical hint explanations, and local Room persistence.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions have been aligned based on user preferences and requirements:

- **Confirmed Game Modes**: Classic Mode, Daily Challenge (date-seeded reproducible puzzle), Practice Mode (zen mode without mistake counter or time pressure), and Timed Challenge.
- **Confirmed Error Detection**: Subtle conflict highlights with a mistake counter (distinguishing between duplicate rule violations and invalid cell values).
- **Confirmed Pencil Marks**: Intuitive Notes Mode with candidate toggle, plus an optional setting to auto-erase conflicting candidate notes when a digit is placed.
- **Confirmed Visual Identity**: Calm, minimalist warm ivory (`#F7F7F4`) light theme with white board surface (`#FFFFFF`), charcoal text (`#242622`), soft gray grid lines (`#DADCD6`), and restrained muted sage accent (`#477A68`); complemented by a deep charcoal dark theme.

---

## 1. Overview & Core Concept

- **What It Does**: Delivers an authentic, distraction-free Sudoku experience. Players can choose between Easy, Medium, Hard, and Expert difficulties or play the Daily Challenge. Puzzles are procedurally generated and mathematically verified to guarantee exactly one unique solution.
- **Target Audience**: Sudoku enthusiasts and casual players alike who value clean typography, instant responsiveness, accurate difficulty tiers, and educational hints over gamified clutter.
- **Key Value**:
  1. *Mathematically verified generation*: No ambiguous puzzles, no guesses required for Easy/Medium, transparent logical difficulty scoring.
  2. *Intelligent 3-Tier Hints*: Nudge (highlight next logical cell), Technique Explanation (e.g., Naked Single, Hidden Single, Box-Line Elimination), and Reveal Digit.
  3. *Distraction-Free Zen Interface*: High readability, tactile number pad, seamless pencil marks, full undo/redo history, and offline persistence.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Launch & Quick Play**: Starts instantly into the active game or a clean New Game prompt. Header features understated wordmark, difficulty pills, game mode badge, timer, and settings icon.
2. **Gameplay & Grid Interaction**:
   - Tap a cell to select it: highlights cell, row, column, and 3×3 box in subtle tint; highlights matching digits across the board.
   - Enter digits via 1–9 keypad or keyboard.
   - Toggle **Notes Mode** to enter candidate pencil marks (displayed in 3×3 mini-grid per cell).
   - Erase, Undo, Redo, and Hint controls directly underneath the board.
3. **Daily Challenge**: Automatically seeded by current date (YYYY-MM-DD), ensuring identical puzzles worldwide for that calendar day, tracked in Daily completion stats.
4. **Completion Celebration**: Clean, understated victory summary displaying difficulty, completion time, mistake count, hints used, and personal best comparisons.
5. **Statistics & Settings Modal**: Overview of games played, completion rate, best/average times per difficulty, current and longest streaks, and preference toggles (Highlight Conflicts, Auto-Clear Notes, Dark Theme).

### Visual Identity & Theme Tokens
- **Main Background**: Warm Ivory `#F7F7F4` (Light) / Charcoal `#1B1D1A` (Dark)
- **Board Surface**: Pure White `#FFFFFF` (Light) / Deep Slate `#242724` (Dark)
- **Primary Typography**: Charcoal `#242622` (Light) / Soft Cream `#EDEDE8` (Dark)
- **Accent**: Muted Sage `#477A68` (Light & Dark)
- **Secondary / Hints**: Muted Amber `#B68A43`
- **Error / Conflict**: Muted Red `#C75B58`
- **Grid Dividers**: Subtle borders `#DADCD6` (subgrid borders `#9A9E96`)
- **Cell Highlighting**:
  - Selected Cell: `#D8E5DF` (sage tint)
  - Related Peers (Row/Col/Box): `#F0F4F1`
  - Matching Digits: `#E2ECE7`

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Procedural Solver & Uniqueness Checking**
  - *Chosen Approach*: Backtracking solver with MRV (Minimum Remaining Values) heuristic + solution counter that halts immediately once count exceeds 1.
  - *Why*: Guarantees every accepted puzzle has strictly one solution while keeping clue removal fast on mobile devices without thread lockups.
- **Decision 2: Logical Difficulty Scoring Model**
  - *Chosen Approach*: Multi-pass deduction evaluator that measures required logical techniques (Naked Singles, Hidden Singles, Pointing Pairs, Naked Pairs, Box-Line Reductions) plus branching/search depth.
  - *Why*: Reflects human-perceived difficulty accurately rather than relying on arbitrary clue counts.
- **Decision 3: Local Data Storage Strategy**
  - *Chosen Approach*: SQLite via Android Room Database for game saves and player statistics; user preferences in persistent DataStore/Room.
  - *Why*: Zero network reliance, instant offline launches, rock-solid state persistence across app reloads.
- **Decision 4: Responsive Mobile-First Layout**
  - *Chosen Approach*: Centered square board constrained to 480–560dp on tablets/landscape and full viewport width with safe margins on mobile, with touch targets adhering strictly to 48dp minimums.

---

## 4. Technical Architecture & Data Strategy

```
┌──────────────────────────────────────────────────────────────┐
│                       Jetpack Compose UI                     │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│  │ SudokuHeader │  │  SudokuGrid  │  │ SudokuControlPanel  │ │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬──────────┘ │
│         │                 │                     │            │
└─────────┼─────────────────┼─────────────────────┼────────────┘
          ▼                 ▼                     ▼
┌──────────────────────────────────────────────────────────────┐
│                    SudokuViewModel                           │
│  - Board State & Selection     - Timer & Mistake Tracking    │
│  - Notes Mode State            - Undo / Redo Stack           │
│  - Hint Engine Coordinator     - Game Mode & Statistics      │
└──────────────┬───────────────────────────┬───────────────────┘
               │                           │
               ▼                           ▼
┌──────────────────────────────┐  ┌────────────────────────────┐
│      Sudoku Engine (CSP)     │  │   Room Database Storage    │
│  - Generator & Uniqueness    │  │  - GameSessionEntity       │
│  - Difficulty Evaluator      │  │  - UserStatsEntity         │
│  - Logical Hint Deductions   │  │  - UserPreferencesEntity   │
└──────────────────────────────┘  └────────────────────────────┘
```

### Core Components
1. **`SudokuEngine`**:
   - `generatePuzzle(difficulty, seed)`: Generates valid terminal grid, applies randomized symmetrical clue pruning, validates uniqueness with MRV solver, scores difficulty.
   - `verifyUniqueness(grid)`: Counts solutions with early termination at 2.
   - `evaluateDifficulty(grid)`: Computes feature vector $S(P) = \sum w_i f_i(P)$.
   - `getLogicalHint(currentGrid, solution)`: Analyzes step-by-step logical deductions (Naked Single -> Hidden Single -> Subgrid reduction).
2. **`SudokuState & MoveHistory`**:
   - Immutable grid cells with clue flag, value (0–9), candidates (Set<Int>), error flag, and highlight state.
   - Command pattern undo/redo stack supporting digit entry, batch note edits, and erases.
3. **`Room Persistence Layer`**:
   - `SudokuDatabase`, `GameDao`, `StatsDao`.
   - Automatic save of ongoing puzzle state on every move.
4. **`Automated JVM & Robolectric Tests`**:
   - Verification of 100% unique solutions, difficulty bounds, hint accuracy, daily seed reproducibility, and undo/redo state immutability.
