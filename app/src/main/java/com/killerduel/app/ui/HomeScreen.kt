package com.killerduel.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.killerduel.app.data.DuelStats
import com.killerduel.app.ui.theme.Palette

@Composable
fun HomeScreen(
    duelStats: DuelStats,
    hasSavedGame: Boolean,
    dailyDone: Boolean,
    onDaily: () -> Unit,
    onTraining: () -> Unit,
    onDuel: () -> Unit,
    onResume: () -> Unit,
    onStats: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.height(64.dp))

        Text(
            "Killer",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Palette.TextPrimary,
            letterSpacing = (-1).sp
        )
        Text(
            "Duel",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Palette.Accent,
            letterSpacing = (-1).sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Sudoku à cages. Seul, ou contre quelqu'un.",
            fontSize = 15.sp,
            color = Palette.TextMuted
        )

        Spacer(Modifier.height(36.dp))

        DailyCard(done = dailyDone, onClick = onDaily)
        Spacer(Modifier.height(12.dp))

        if (hasSavedGame) {
            ModeCard(
                title = "Reprendre",
                subtitle = "Votre grille vous attend là où vous l'avez laissée",
                accent = Palette.Success,
                icon = AppIcon.Play,
                onClick = onResume
            )
            Spacer(Modifier.height(12.dp))
        }

        ModeCard(
            title = "Entraînement",
            subtitle = "Une grille, votre rythme, quatre niveaux",
            accent = Palette.Accent,
            icon = AppIcon.Pencil,
            onClick = onTraining
        )
        Spacer(Modifier.height(12.dp))
        ModeCard(
            title = "Duel",
            subtitle = duelSubtitle(duelStats),
            accent = Palette.Gold,
            icon = AppIcon.Trophy,
            onClick = onDuel
        )

        Spacer(Modifier.weight(1f))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onStats)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconView(AppIcon.Stats, Palette.TextMuted, size = 18.dp)
            Spacer(Modifier.size(8.dp))
            Text("Statistiques", color = Palette.TextMuted, fontSize = 15.sp)
        }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Le défi du jour : une grille par date, la même à chaque ouverture. Une fois
 * résolue, la carte le dit plutôt que de disparaître — revenir dessus reste
 * possible, cela ne change simplement plus rien.
 */
@Composable
private fun DailyCard(done: Boolean, onClick: () -> Unit) {
    val accent = if (done) Palette.Success else Palette.Gold
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = Palette.Surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (done) accent.copy(alpha = 0.4f) else Palette.Divider),
        shadowElevation = 1.dp
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                AppIconView(AppIcon.Calendar, accent, size = 24.dp)
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Défi du jour",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (done) "Résolu — ${todayLabel()}" else todayLabel(),
                    fontSize = 13.sp,
                    color = if (done) accent else Palette.TextMuted
                )
            }
        }
    }
}

private fun todayLabel(): String {
    val today = java.time.LocalDate.now()
    val month = java.time.format.TextStyle.FULL
    return "${today.dayOfMonth} ${today.month.getDisplayName(month, java.util.Locale.FRANCE)}"
}

private fun duelSubtitle(stats: DuelStats): String = when {
    stats.played == 0 -> "Course contre un adversaire, même grille"
    else -> "${stats.won} victoire${if (stats.won > 1) "s" else ""} sur ${stats.played} duel${if (stats.played > 1) "s" else ""}"
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    accent: Color,
    icon: AppIcon,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = Palette.Surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Palette.Divider),
        shadowElevation = 1.dp
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                AppIconView(icon, accent, size = 24.dp)
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Palette.TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 13.sp, color = Palette.TextMuted)
            }
        }
    }
}
