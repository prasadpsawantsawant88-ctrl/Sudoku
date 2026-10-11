package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SudokuCustomColors(
    val background: Color,
    val boardSurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color,
    val gridBorder: Color,
    val subgridBorder: Color,
    val error: Color,
    val hint: Color,
    val selectedCell: Color,
    val peerCell: Color,
    val matchingCell: Color,
    val conflictCell: Color
)

val LocalSudokuColors = staticCompositionLocalOf {
    SudokuCustomColors(
        background = WarmIvoryBackground,
        boardSurface = WhiteBoardSurface,
        primaryText = CharcoalPrimaryText,
        secondaryText = NeutralGraySecondaryText,
        accent = MutedSageAccent,
        gridBorder = SoftGrayGridBorder,
        subgridBorder = SubgridBorderLight,
        error = MutedRedError,
        hint = MutedAmberHint,
        selectedCell = SelectedCellHighlightLight,
        peerCell = PeerCellHighlightLight,
        matchingCell = MatchingCellHighlightLight,
        conflictCell = ConflictCellHighlightLight
    )
}

private val LightColorScheme = lightColorScheme(
    primary = MutedSageAccent,
    onPrimary = Color.White,
    primaryContainer = SelectedCellHighlightLight,
    onPrimaryContainer = CharcoalPrimaryText,
    secondary = NeutralGraySecondaryText,
    onSecondary = Color.White,
    background = WarmIvoryBackground,
    onBackground = CharcoalPrimaryText,
    surface = WhiteBoardSurface,
    onSurface = CharcoalPrimaryText,
    error = MutedRedError,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkSageAccent,
    onPrimary = Color.White,
    primaryContainer = SelectedCellHighlightDark,
    onPrimaryContainer = DarkPrimaryText,
    secondary = DarkSecondaryText,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkBoardSurface,
    onSurface = DarkPrimaryText,
    error = DarkMutedRedError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val customColors = if (darkTheme) {
        SudokuCustomColors(
            background = DarkBackground,
            boardSurface = DarkBoardSurface,
            primaryText = DarkPrimaryText,
            secondaryText = DarkSecondaryText,
            accent = DarkSageAccent,
            gridBorder = DarkGridBorder,
            subgridBorder = SubgridBorderDark,
            error = DarkMutedRedError,
            hint = DarkMutedAmberHint,
            selectedCell = SelectedCellHighlightDark,
            peerCell = PeerCellHighlightDark,
            matchingCell = MatchingCellHighlightDark,
            conflictCell = ConflictCellHighlightDark
        )
    } else {
        SudokuCustomColors(
            background = WarmIvoryBackground,
            boardSurface = WhiteBoardSurface,
            primaryText = CharcoalPrimaryText,
            secondaryText = NeutralGraySecondaryText,
            accent = MutedSageAccent,
            gridBorder = SoftGrayGridBorder,
            subgridBorder = SubgridBorderLight,
            error = MutedRedError,
            hint = MutedAmberHint,
            selectedCell = SelectedCellHighlightLight,
            peerCell = PeerCellHighlightLight,
            matchingCell = MatchingCellHighlightLight,
            conflictCell = ConflictCellHighlightLight
        )
    }

    CompositionLocalProvider(LocalSudokuColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
