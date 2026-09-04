package com.killerduel.app.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.killerduel.app.data.GameMode
import com.killerduel.app.game.GameSession
import com.killerduel.app.game.Outcome
import com.killerduel.app.ui.theme.Palette

@Composable
private fun Scrim(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC1B1D21))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun DialogCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        color = Palette.Surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { content() }
    }
}

@Composable
fun PauseOverlay(onResume: () -> Unit) {
    Scrim {
        DialogCard {
            AppIconView(AppIcon.Pause, Palette.TextMuted, size = 34.dp)
            Spacer(Modifier.height(12.dp))
            Text("Partie en pause", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "La grille est masquée et le chronomètre arrêté.",
                fontSize = 14.sp,
                color = Palette.TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Reprendre", onResume, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ResultOverlay(
    session: GameSession,
    level: Int,
    onReplay: () -> Unit,
    onHome: () -> Unit
) {
    val won = session.outcome == Outcome.WON
    val opponentName = session.opponent?.profile?.name

    val title = when (session.outcome) {
        Outcome.WON -> if (session.mode == GameMode.DUEL) "Victoire !" else "Grille terminée !"
        Outcome.LOST_ON_TIME -> "Battu au sprint"
        Outcome.LOST_ON_MISTAKES -> "Partie perdue"
        null -> ""
    }
    val message = when (session.outcome) {
        Outcome.WON ->
            if (session.mode == GameMode.DUEL && opponentName != null) {
                "Vous avez coiffé $opponentName sur le fil."
            } else {
                "Résolue en ${formatDuration(session.elapsedMillis)}."
            }
        Outcome.LOST_ON_TIME -> "${opponentName ?: "L'adversaire"} a rempli la grille avant vous."
        Outcome.LOST_ON_MISTAKES -> "${GameSession.MAX_MISTAKES} erreurs : la grille se referme."
        null -> ""
    }

    Scrim {
        DialogCard {
            if (won) {
                StarRow(session.stars)
                Spacer(Modifier.height(10.dp))
            } else {
                AppIconView(AppIcon.Stats, Palette.TextMuted, size = 40.dp)
                Spacer(Modifier.height(14.dp))
            }
            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (won) Palette.Success else Palette.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                fontSize = 14.sp,
                color = Palette.TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultStat("Temps", formatDuration(session.elapsedMillis))
                ResultStat("Score", session.score.toString())
                ResultStat("Niveau", level.toString())
            }

            if (won && session.stars < 3) {
                Spacer(Modifier.height(12.dp))
                Text(
                    missingStarHint(session),
                    fontSize = 12.sp,
                    color = Palette.TextMuted,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(22.dp))
            PrimaryButton("Nouvelle partie", onReplay, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Accueil", onHome, Modifier.fillMaxWidth())
        }
    }
}

/** Les trois étoiles de la partie : gagnées en doré, manquées en creux. */
@Composable
private fun StarRow(stars: Int) {
    Row(horizontalArrangement = Arrangement.Center) {
        for (i in 1..3) {
            AppIconView(
                AppIcon.Star,
                if (i <= stars) Palette.Gold else Palette.Divider,
                size = if (i == 2) 44.dp else 34.dp,
                modifier = Modifier.padding(horizontal = 3.dp)
            )
        }
    }
}

/** Dit ce qui manquait, plutôt que de laisser deviner. */
private fun missingStarHint(session: GameSession): String = when {
    !session.beatTheClock && !session.flawless ->
        "Sous ${formatDuration(session.targetMillis)} et sans erreur pour trois étoiles."
    !session.beatTheClock ->
        "Il fallait finir sous ${formatDuration(session.targetMillis)} pour la dernière étoile."
    else -> "Une grille sans erreur vous aurait donné la dernière étoile."
}

@Composable
private fun ResultStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Palette.TextPrimary)
        Text(label, fontSize = 12.sp, color = Palette.TextMuted)
    }
}
