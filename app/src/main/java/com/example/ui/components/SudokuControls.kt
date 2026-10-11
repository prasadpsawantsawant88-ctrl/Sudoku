package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalSudokuColors

@Composable
fun SudokuControls(
    isNotesMode: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    hintsUsed: Int,
    remainingCounts: Map<Int, Int>,
    onDigitClick: (Int) -> Unit,
    onEraseClick: () -> Unit,
    onToggleNotes: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onHintClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSudokuColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Action Controls Row
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo Button
            ActionIconButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                label = "Undo",
                enabled = canUndo,
                onClick = onUndoClick,
                testTag = "undo_button"
            )

            // Redo Button
            ActionIconButton(
                icon = Icons.AutoMirrored.Filled.Redo,
                label = "Redo",
                enabled = canRedo,
                onClick = onRedoClick,
                testTag = "redo_button"
            )

            // Erase Button
            ActionIconButton(
                icon = Icons.Default.Backspace,
                label = "Erase",
                enabled = true,
                onClick = onEraseClick,
                testTag = "erase_button"
            )

            // Notes Mode Toggle
            ActionIconButton(
                icon = Icons.Default.Edit,
                label = if (isNotesMode) "Notes ON" else "Notes",
                isActive = isNotesMode,
                enabled = true,
                onClick = onToggleNotes,
                testTag = "notes_toggle_button"
            )

            // Hint Button
            ActionIconButton(
                icon = Icons.Default.AutoAwesome,
                label = if (hintsUsed > 0) "Hint ($hintsUsed)" else "Hint",
                isAccent = true,
                enabled = true,
                onClick = onHintClick,
                testTag = "hint_button"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Number Pad 1..9
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (digit in 1..9) {
                val countPlaced = remainingCounts[digit] ?: 0
                val isCompleted = countPlaced >= 9

                NumpadKey(
                    digit = digit,
                    countPlaced = countPlaced,
                    isCompleted = isCompleted,
                    onClick = { onDigitClick(digit) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    isActive: Boolean = false,
    isAccent: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = LocalSudokuColors.current
    val interactionSource = remember { MutableInteractionSource() }

    val tint = when {
        !enabled -> colors.secondaryText.copy(alpha = 0.35f)
        isActive -> colors.accent
        isAccent -> colors.hint
        else -> colors.primaryText
    }

    val bgModifier = if (isActive) {
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.accent.copy(alpha = 0.15f))
    } else {
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
    }

    Column(
        modifier = Modifier
            .then(bgModifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun NumpadKey(
    digit: Int,
    countPlaced: Int,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSudokuColors.current
    val interactionSource = remember { MutableInteractionSource() }

    val bg = if (isCompleted) colors.boardSurface.copy(alpha = 0.4f) else colors.boardSurface
    val textTint = if (isCompleted) colors.secondaryText.copy(alpha = 0.4f) else colors.primaryText
    val countTint = if (isCompleted) colors.secondaryText.copy(alpha = 0.3f) else colors.secondaryText

    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(
                enabled = !isCompleted,
                interactionSource = interactionSource,
                indication = ripple(bounded = true)
            ) { onClick() }
            .testTag("numpad_$digit")
            .semantics {
                contentDescription = if (isCompleted) "Number $digit completed" else "Enter number $digit ($countPlaced of 9 placed)"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit.toString(),
                color = textTint,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isCompleted) "✓" else "${9 - countPlaced}",
                color = countTint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
