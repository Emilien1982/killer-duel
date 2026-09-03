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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.killerduel.app.data.DuelStats
import com.killerduel.app.ui.theme.Palette

@Composable
fun StatsScreen(
    stats: Map<Difficulty, DifficultyStats>,
    duelStats: DuelStats,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { AppIconView(AppIcon.Back, Palette.TextMuted, size = 22.dp) }

        Spacer(Modifier.height(10.dp))
        Text("Statistiques", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Palette.TextPrimary)
        Spacer(Modifier.height(20.dp))

        Surface(
            Modifier.fillMaxWidth(),
            color = Palette.Surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Palette.Divider)
        ) {
            Row(
                Modifier.padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Metric("Duels", duelStats.played.toString())
                Metric("Victoires", duelStats.won.toString())
                Metric(
                    "Taux",
                    if (duelStats.played == 0) "—"
                    else "${duelStats.won * 100 / duelStats.played} %"
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Entraînement", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.TextMuted)
        Spacer(Modifier.height(10.dp))

        Difficulty.entries.forEach { difficulty ->
            val row = stats[difficulty] ?: DifficultyStats()
            Surface(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = Palette.Surface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Palette.Divider)
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        difficulty.label,
                        Modifier.weight(1f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Palette.TextPrimary
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (row.completed == 0) "—" else formatDuration(row.bestMillis),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Palette.Accent
                        )
                        Text(
                            "${row.completed} terminée${if (row.completed > 1) "s" else ""} sur ${row.played}",
                            fontSize = 11.sp,
                            color = Palette.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Palette.TextPrimary)
        Text(label, fontSize = 12.sp, color = Palette.TextMuted)
    }
}
