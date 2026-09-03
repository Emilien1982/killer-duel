package com.killerduel.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Palette calquée sur les jeux de Killer Sudoku du commerce : fond clair,
 * grille noire, cages en pointillés, saisies en bleu. Le thème reste clair
 * en permanence, un damier de chiffres se lit mal sur fond sombre.
 */
object Palette {
    val Background = Color(0xFFF4F4F2)
    val Surface = Color(0xFFFFFFFF)
    val GridLine = Color(0xFFC9CCD1)
    val GridStrong = Color(0xFF2B2B2B)
    val CageDash = Color(0xFF7C838C)

    val Selected = Color(0xFFFFD24A)
    val SelectedCage = Color(0xFFFFF2C4)
    val PeerHighlight = Color(0xFFE4EDF9)
    val SameValue = Color(0xFFC6DDF7)

    val Given = Color(0xFF20242A)
    val Entry = Color(0xFF1A73E8)
    val Error = Color(0xFFD93A3F)
    val ErrorBackground = Color(0xFFFBE0E0)
    val Note = Color(0xFF7C838C)
    val CageSum = Color(0xFF4A4F57)

    val TextPrimary = Color(0xFF20242A)
    val TextMuted = Color(0xFF8A8F98)
    val Accent = Color(0xFF1A73E8)
    val AccentSoft = Color(0xFFE8F0FE)
    val Gold = Color(0xFFF2A93B)
    val Success = Color(0xFF2E9E5B)
    val Divider = Color(0xFFE3E5E8)
}

private val ColorScheme = lightColorScheme(
    primary = Palette.Accent,
    onPrimary = Color.White,
    background = Palette.Background,
    onBackground = Palette.TextPrimary,
    surface = Palette.Surface,
    onSurface = Palette.TextPrimary,
    error = Palette.Error
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun KillerDuelTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, typography = AppTypography, content = content)
}
