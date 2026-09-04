package com.killerduel.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.killerduel.app.data.AppTheme

/**
 * Toutes les couleurs de l'application, réunies pour qu'un thème soit un simple
 * échange de jeu de valeurs plutôt qu'une retouche éparpillée.
 */
@Immutable
data class AppPalette(
    val Background: Color,
    val Surface: Color,
    val GridLine: Color,
    val GridStrong: Color,
    val CageDash: Color,
    val Selected: Color,
    val SelectedCage: Color,
    val PeerHighlight: Color,
    val SameValue: Color,
    val Given: Color,
    val Entry: Color,
    val Error: Color,
    val ErrorBackground: Color,
    val Note: Color,
    val CageSum: Color,
    val TextPrimary: Color,
    val TextMuted: Color,
    val Accent: Color,
    val AccentSoft: Color,
    val Gold: Color,
    val Success: Color,
    val Divider: Color,
    val CageTints: List<Color>,
    val CageSeparator: Color,
    val NoteHighlight: Color,
    val isDark: Boolean
)

/** Thème clair : celui des jeux de Killer Sudoku du commerce. */
val LightPalette = AppPalette(
    Background = Color(0xFFF4F4F2),
    Surface = Color(0xFFFFFFFF),
    GridLine = Color(0xFFC9CCD1),
    GridStrong = Color(0xFF2B2B2B),
    CageDash = Color(0xFF7C838C),
    Selected = Color(0xFFFFD24A),
    SelectedCage = Color(0xFFFFF2C4),
    PeerHighlight = Color(0xFFE4EDF9),
    SameValue = Color(0xFFC6DDF7),
    Given = Color(0xFF20242A),
    Entry = Color(0xFF1A73E8),
    Error = Color(0xFFD93A3F),
    ErrorBackground = Color(0xFFFBE0E0),
    Note = Color(0xFF7C838C),
    CageSum = Color(0xFF4A4F57),
    TextPrimary = Color(0xFF20242A),
    TextMuted = Color(0xFF8A8F98),
    Accent = Color(0xFF1A73E8),
    AccentSoft = Color(0xFFE8F0FE),
    Gold = Color(0xFFF2A93B),
    Success = Color(0xFF2E9E5B),
    Divider = Color(0xFFE3E5E8),
    CageTints = listOf(
        Color(0xFFFAE7CD), Color(0xFFD7E5F7), Color(0xFFD9ECDD),
        Color(0xFFF6DAE4), Color(0xFFE1DAF4), Color(0xFFFAEEC2)
    ),
    CageSeparator = Color(0xFFFFFFFF),
    NoteHighlight = Color(0xFFAFCDF4),
    isDark = false
)

/** Thème crème : plus chaud, moins éblouissant en lecture prolongée. */
val CreamPalette = LightPalette.copy(
    Background = Color(0xFFF3ECDD),
    Surface = Color(0xFFFCF7EC),
    GridLine = Color(0xFFD6CCB6),
    GridStrong = Color(0xFF4A4235),
    Selected = Color(0xFFF5C64A),
    Divider = Color(0xFFE2D8C4),
    CageTints = listOf(
        Color(0xFFF3E3C6), Color(0xFFDCE5EC), Color(0xFFDCE8D6),
        Color(0xFFEFDCDC), Color(0xFFE2DCEC), Color(0xFFF6EDC4)
    ),
    CageSeparator = Color(0xFFFCF7EC),
    isDark = false
)

/** Thème sombre : mêmes rôles, valeurs inversées, teintes de cages assombries. */
val DarkPalette = AppPalette(
    Background = Color(0xFF16181C),
    Surface = Color(0xFF1F2228),
    GridLine = Color(0xFF3A3F47),
    GridStrong = Color(0xFFB9BFC7),
    CageDash = Color(0xFF6C737C),
    Selected = Color(0xFFB88A18),
    SelectedCage = Color(0xFF4A3D18),
    PeerHighlight = Color(0xFF2A3038),
    SameValue = Color(0xFF2F4666),
    Given = Color(0xFFE8EAED),
    Entry = Color(0xFF74A9F5),
    Error = Color(0xFFF06A6E),
    ErrorBackground = Color(0xFF4A2427),
    Note = Color(0xFF949AA3),
    CageSum = Color(0xFFB6BCC4),
    TextPrimary = Color(0xFFE8EAED),
    TextMuted = Color(0xFF9AA0A8),
    Accent = Color(0xFF74A9F5),
    AccentSoft = Color(0xFF25303F),
    Gold = Color(0xFFE0A33C),
    Success = Color(0xFF4FB878),
    Divider = Color(0xFF31363E),
    CageTints = listOf(
        Color(0xFF3A322A), Color(0xFF25313F), Color(0xFF26352B),
        Color(0xFF3A2A31), Color(0xFF2E2A3D), Color(0xFF3B3722)
    ),
    CageSeparator = Color(0xFF16181C),
    NoteHighlight = Color(0xFF3C5A85),
    isDark = true
)

fun paletteFor(theme: AppTheme): AppPalette = when (theme) {
    AppTheme.LIGHT -> LightPalette
    AppTheme.CREAM -> CreamPalette
    AppTheme.DARK -> DarkPalette
}

val LocalPalette = staticCompositionLocalOf { LightPalette }

/**
 * Accès aux couleurs du thème courant. Le nom est conservé pour que le reste de
 * l'interface s'écrive toujours `Palette.Surface`, sans porter la mécanique du
 * thème dans chaque composable.
 */
val Palette: AppPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalPalette.current

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
fun KillerDuelTheme(theme: AppTheme = AppTheme.LIGHT, content: @Composable () -> Unit) {
    val palette = paletteFor(theme)
    val scheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.Accent,
            onPrimary = Color.Black,
            background = palette.Background,
            onBackground = palette.TextPrimary,
            surface = palette.Surface,
            onSurface = palette.TextPrimary,
            error = palette.Error
        )
    } else {
        lightColorScheme(
            primary = palette.Accent,
            onPrimary = Color.White,
            background = palette.Background,
            onBackground = palette.TextPrimary,
            surface = palette.Surface,
            onSurface = palette.TextPrimary,
            error = palette.Error
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
