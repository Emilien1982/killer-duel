package com.killerduel.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.killerduel.app.core.Difficulty
import com.killerduel.app.data.DifficultyStats
import com.killerduel.app.data.GameMode
import com.killerduel.app.ui.theme.Palette

@Composable
fun LevelPickerScreen(
    mode: GameMode,
    stats: Map<Difficulty, DifficultyStats>,
    generating: Boolean,
    onBack: () -> Unit,
    onPick: (Difficulty) -> Unit
) {
    Box(Modifier.fillMaxSize().background(Palette.Background)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { AppIconView(AppIcon.Back, Palette.TextMuted, size = 22.dp) }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (mode == GameMode.DUEL) "Choisir le terrain" else "Choisir un niveau",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (mode == GameMode.DUEL) {
                    "Vous et votre adversaire recevrez la même grille."
                } else {
                    "Chaque grille est générée à la demande, jamais deux fois la même."
                },
                fontSize = 14.sp,
                color = Palette.TextMuted
            )
            Spacer(Modifier.height(24.dp))

            Difficulty.entries.forEach { difficulty ->
                LevelCard(
                    difficulty = difficulty,
                    stats = stats[difficulty] ?: DifficultyStats(),
                    onClick = { if (!generating) onPick(difficulty) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        if (generating) {
            Box(
                Modifier.fillMaxSize().background(Palette.Background.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Palette.Accent)
                    Spacer(Modifier.height(14.dp))
                    Text("Composition de la grille…", color = Palette.TextMuted, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun LevelCard(
    difficulty: Difficulty,
    stats: DifficultyStats,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = Palette.Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Palette.Divider)
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DifficultyGauge(difficulty)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    difficulty.label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.TextPrimary
                )
                Text(
                    if (stats.completed == 0) "Jamais jouée" else
                        "Record ${formatDuration(stats.bestMillis)} · ${stats.completed} terminée${if (stats.completed > 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = Palette.TextMuted
                )
            }
        }
    }
}

/** Quatre barres : la difficulté se lit d'un coup d'œil, sans lire le mot. */
@Composable
private fun DifficultyGauge(difficulty: Difficulty) {
    val level = Difficulty.entries.indexOf(difficulty) + 1
    val color = when (difficulty) {
        Difficulty.EASY -> Palette.Success
        Difficulty.MEDIUM -> Palette.Accent
        Difficulty.HARD -> Palette.Gold
        Difficulty.KILLER -> Palette.Error
    }
    Row(verticalAlignment = Alignment.Bottom) {
        for (i in 1..4) {
            Box(
                Modifier
                    .padding(end = 3.dp)
                    .size(width = 5.dp, height = (7 + i * 4).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i <= level) color else Palette.Divider)
            )
        }
    }
}
