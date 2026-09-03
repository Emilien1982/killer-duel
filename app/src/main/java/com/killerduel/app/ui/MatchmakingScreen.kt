package com.killerduel.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.killerduel.app.core.Difficulty
import com.killerduel.app.ui.theme.Palette

@Composable
fun MatchmakingScreen(difficulty: Difficulty, progress: Float, onCancel: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scale"
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            Modifier
                .size(110.dp)
                .scale(scale)
                .clip(RoundedCornerShape(30.dp))
                .background(Palette.Gold.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            AppIconView(AppIcon.Trophy, Palette.Gold, size = 52.dp)
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "Recherche d'un adversaire",
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            color = Palette.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Niveau ${difficulty.label.lowercase()} — vous jouerez la même grille, chacun de votre côté.",
            fontSize = 14.sp,
            color = Palette.TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(26.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Palette.Gold,
            trackColor = Palette.Divider,
            drawStopIndicator = {}
        )

        Spacer(Modifier.weight(1f))
        SecondaryButton("Annuler", onCancel, Modifier.fillMaxWidth())
        Spacer(Modifier.height(28.dp))
    }
}
