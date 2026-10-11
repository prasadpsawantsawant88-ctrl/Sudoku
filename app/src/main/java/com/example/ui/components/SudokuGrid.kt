package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SudokuCell
import com.example.ui.theme.LocalSudokuColors

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs

@Composable
fun SudokuGrid(
    cells: List<SudokuCell>,
    selectedRow: Int,
    selectedCol: Int,
    selectedValue: Int,
    highlightMatching: Boolean,
    highlightConflicts: Boolean,
    highlightPeers: Boolean,
    onCellClick: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
    isVictorious: Boolean = false
) {
    val colors = LocalSudokuColors.current

    // Gentle celebratory wave animation sweeping across the grid
    val waveProgress = remember { Animatable(0f) }
    LaunchedEffect(isVictorious) {
        if (isVictorious) {
            waveProgress.snapTo(0f)
            waveProgress.animateTo(
                targetValue = 1.35f,
                animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
            )
        } else {
            waveProgress.snapTo(0f)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Constrain max width for tablets/desktop while maintaining square 1:1 ratio
        val boardModifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.boardSurface)
            .border(2.dp, colors.subgridBorder, RoundedCornerShape(8.dp))

        Box(modifier = boardModifier) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until 9) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (c in 0 until 9) {
                            val idx = r * 9 + c
                            val cell = if (idx < cells.size) cells[idx] else SudokuCell(r, c)
                            val isSelected = (r == selectedRow && c == selectedCol)
                            val isPeer = highlightPeers && (r == selectedRow || c == selectedCol ||
                                    ((r / 3 == selectedRow / 3) && (c / 3 == selectedCol / 3)))
                            val isMatchingDigit = highlightMatching && selectedValue in 1..9 && cell.value == selectedValue
                            val isConflict = highlightConflicts && cell.isConflict

                            // Calculate gentle diagonal wave glow intensity for this cell
                            val cellDiag = (r + c) / 16f
                            val diff = abs(waveProgress.value - cellDiag)
                            val waveGlow = if (isVictorious && diff < 0.16f) {
                                (1f - (diff / 0.16f)).coerceIn(0f, 1f)
                            } else 0f

                            SudokuCellView(
                                cell = cell,
                                isSelected = isSelected,
                                isPeer = isPeer,
                                isMatchingDigit = isMatchingDigit,
                                isConflict = isConflict,
                                waveGlow = waveGlow,
                                onClick = { onCellClick(r, c) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Draw clean 3x3 subgrid divider lines on top
            val subgridColor = colors.subgridBorder
            val thinBorderColor = colors.gridBorder

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        val cellW = w / 9f
                        val cellH = h / 9f

                        // Draw thin inner cell lines
                        for (i in 1 until 9) {
                            if (i % 3 != 0) {
                                // Vertical thin line
                                drawLine(
                                    color = thinBorderColor,
                                    start = Offset(cellW * i, 0f),
                                    end = Offset(cellW * i, h),
                                    strokeWidth = 1.dp.toPx()
                                )
                                // Horizontal thin line
                                drawLine(
                                    color = thinBorderColor,
                                    start = Offset(0f, cellH * i),
                                    end = Offset(w, cellH * i),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }

                        // Draw thicker subgrid lines for 3x3 blocks
                        for (b in 1..2) {
                            // Vertical 3x3 line
                            drawLine(
                                color = subgridColor,
                                start = Offset(cellW * b * 3, 0f),
                                end = Offset(cellW * b * 3, h),
                                strokeWidth = 2.5.dp.toPx()
                            )
                            // Horizontal 3x3 line
                            drawLine(
                                color = subgridColor,
                                start = Offset(0f, cellH * b * 3),
                                end = Offset(w, cellH * b * 3),
                                strokeWidth = 2.5.dp.toPx()
                            )
                        }
                    }
            )
        }
    }
}

@Composable
private fun SudokuCellView(
    cell: SudokuCell,
    isSelected: Boolean,
    isPeer: Boolean,
    isMatchingDigit: Boolean,
    isConflict: Boolean,
    waveGlow: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSudokuColors.current
    val interactionSource = remember { MutableInteractionSource() }

    // Determine background color based on visual hierarchy:
    // Selected cell is distinct from matching digits
    val baseBgColor = when {
        cell.isHintTarget -> colors.hint.copy(alpha = 0.25f)
        isConflict -> colors.conflictCell
        isSelected -> colors.selectedCell
        isMatchingDigit -> colors.matchingCell
        isPeer -> colors.peerCell
        else -> colors.boardSurface
    }

    val bgColor = if (waveGlow > 0f) {
        // Blend softly with celebratory sage highlight
        colors.accent.copy(alpha = 0.25f * waveGlow)
    } else baseBgColor

    val cellAccessibilityDesc = buildString {
        append("Row ${cell.row + 1}, Column ${cell.col + 1}. ")
        if (cell.value != 0) {
            append("Value: ${cell.value}. ")
            if (cell.isClue) append("Original clue. ") else append("Player entered. ")
        } else if (cell.notes.isNotEmpty()) {
            append("Notes: ${cell.notes.sorted().joinToString(", ")}. ")
        } else {
            append("Empty. ")
        }
        if (isSelected) append("Currently selected.")
    }

    Box(
        modifier = modifier
            .testTag("cell_${cell.row}_${cell.col}")
            .semantics { contentDescription = cellAccessibilityDesc }
            .background(bgColor)
            .then(
                if (isSelected) Modifier.border(2.dp, colors.accent)
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true)
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (cell.value in 1..9) {
            val digitColor = when {
                isConflict -> colors.error
                cell.isClue -> colors.primaryText // Original clue: Deep Charcoal
                else -> colors.accent             // Player-entered: Muted Sage
            }
            val digitWeight = if (cell.isClue) FontWeight.ExtraBold else FontWeight.SemiBold

            Text(
                text = cell.value.toString(),
                color = digitColor,
                fontSize = 22.sp,
                fontWeight = digitWeight,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = if (waveGlow > 0f) {
                    Modifier.graphicsLayer {
                        val s = 1f + 0.1f * waveGlow
                        scaleX = s
                        scaleY = s
                    }
                } else Modifier
            )
        } else if (cell.notes.isNotEmpty()) {
            NotesMiniGrid(notes = cell.notes, noteColor = colors.secondaryText)
        }

        // Active hint target indicator border
        if (cell.isHintTarget) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.5.dp, colors.hint)
            )
        }
    }
}

@Composable
private fun NotesMiniGrid(
    notes: Set<Int>,
    noteColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0 until 3) {
                    val digit = row * 3 + col + 1
                    val hasNote = notes.contains(digit)
                    Text(
                        text = if (hasNote) digit.toString() else " ",
                        color = noteColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}
