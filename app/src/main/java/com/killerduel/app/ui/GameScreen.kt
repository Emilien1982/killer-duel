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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.killerduel.app.data.GameMode
import com.killerduel.app.game.GameSession
import com.killerduel.app.ui.theme.Palette

@Composable
fun GameScreen(
    session: GameSession,
    onBack: () -> Unit,
    onCell: (Int) -> Unit,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onUndo: () -> Unit,
    onToggleNotes: () -> Unit,
    onHint: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onToggleDigitFirst: () -> Unit,
    onReplay: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Palette.Background)) {
        Column(Modifier.fillMaxSize()) {
            GameTopBar(
                title = if (session.mode == GameMode.DUEL) "Défi" else "Entraînement",
                onBack = onBack,
                onPause = onPause,
                // Mettre un duel en pause figerait aussi l'adversaire : seul
                // l'entraînement s'interrompt.
                pauseEnabled = !session.finished && session.mode == GameMode.TRAINING
            )

            InfoRow(session)

            if (session.mode == GameMode.DUEL && session.opponent != null) {
                Spacer(Modifier.height(8.dp))
                DuelBanner(session)
            }

            Spacer(Modifier.weight(0.4f))

            SudokuBoard(
                state = BoardState(
                    puzzle = session.puzzle,
                    entries = session.entries,
                    notes = session.notes,
                    selected = session.selected,
                    wrongCells = session.wrongCells
                ),
                onCellTap = onCell,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .alpha(if (session.paused) 0f else 1f)
            )

            Spacer(Modifier.weight(0.8f))

            ActionRow(
                pencil = session.pencil,
                hintsLeft = session.hintsLeft,
                canUndo = session.history.isNotEmpty(),
                onUndo = onUndo,
                onErase = onErase,
                onToggleNotes = onToggleNotes,
                onHint = onHint
            )

            Spacer(Modifier.height(14.dp))

            NumberPad(
                isExhausted = { session.isDigitExhausted(it) },
                activeDigit = session.activeDigit,
                onDigit = onDigit,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            Spacer(Modifier.height(10.dp))

            DigitFirstSwitch(checked = session.digitFirst, onToggle = onToggleDigitFirst)

            Spacer(Modifier.weight(0.7f))
        }

        if (session.paused && !session.finished) {
            PauseOverlay(onResume = onResume)
        }
        if (session.finished) {
            ResultOverlay(session = session, onReplay = onReplay, onHome = onBack)
        }
    }
}

@Composable
private fun GameTopBar(
    title: String,
    onBack: () -> Unit,
    onPause: () -> Unit,
    pauseEnabled: Boolean
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(AppIcon.Back, onBack)
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = Palette.TextPrimary,
            modifier = Modifier.weight(1f).padding(start = 4.dp)
        )
        if (pauseEnabled) IconButton(AppIcon.Pause, onPause)
    }
}

@Composable
private fun IconButton(icon: AppIcon, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AppIconView(icon, Palette.TextMuted, size = 22.dp)
    }
}

@Composable
private fun InfoRow(session: GameSession) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Erreurs ${session.mistakes}/${GameSession.MAX_MISTAKES}",
            color = if (session.mistakes > 0) Palette.Error else Palette.TextMuted,
            fontSize = 13.sp
        )
        Text(session.puzzle.difficulty.label, color = Palette.TextMuted, fontSize = 13.sp)
        Text(
            formatDuration(session.elapsedMillis),
            color = Palette.TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Face à face du duel : la progression de chacun, sans montrer la grille adverse. */
@Composable
private fun DuelBanner(session: GameSession) {
    val opponent = session.opponent ?: return
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Palette.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Palette.Divider),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            ProgressLine(
                label = "Vous",
                badge = "",
                filled = session.filledCount,
                color = Palette.Accent
            )
            Spacer(Modifier.height(8.dp))
            ProgressLine(
                label = opponent.profile.name,
                badge = "${opponent.profile.flag}  ${opponent.profile.rating}",
                filled = session.opponentFilled,
                color = Palette.Gold
            )
        }
    }
}

@Composable
private fun ProgressLine(label: String, badge: String, filled: Int, color: androidx.compose.ui.graphics.Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Palette.TextPrimary,
                maxLines = 1
            )
            if (badge.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(badge, fontSize = 12.sp, color = Palette.TextMuted)
            }
            Spacer(Modifier.weight(1f))
            Text("$filled/81", fontSize = 12.sp, color = Palette.TextMuted)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { filled / 81f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Palette.Background,
            drawStopIndicator = {}
        )
    }
}

@Composable
private fun ActionRow(
    pencil: Boolean,
    hintsLeft: Int,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onErase: () -> Unit,
    onToggleNotes: () -> Unit,
    onHint: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(AppIcon.Undo, "Annuler", enabled = canUndo, onClick = onUndo)
        ActionButton(AppIcon.Erase, "Gommer", onClick = onErase)
        ActionButton(
            AppIcon.Pencil,
            if (pencil) "Notes ON" else "Notes",
            active = pencil,
            onClick = onToggleNotes
        )
        ActionButton(
            AppIcon.Hint,
            "Indice",
            badge = hintsLeft.takeIf { it > 0 }?.toString(),
            enabled = hintsLeft > 0,
            onClick = onHint
        )
    }
}

@Composable
private fun ActionButton(
    icon: AppIcon,
    label: String,
    enabled: Boolean = true,
    active: Boolean = false,
    badge: String? = null,
    onClick: () -> Unit
) {
    val tint = when {
        !enabled -> Palette.TextMuted.copy(alpha = 0.35f)
        active -> Palette.Accent
        else -> Palette.TextMuted
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                AppIconView(icon, tint, size = 24.dp)
            }
            if (badge != null) {
                Surface(
                    color = Palette.Accent,
                    shape = CircleShape,
                    modifier = Modifier.size(15.dp)
                ) {
                    Text(
                        badge,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 1.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = tint)
    }
}

@Composable
private fun NumberPad(
    isExhausted: (Int) -> Boolean,
    activeDigit: Int,
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (digit in 1..9) {
            val done = isExhausted(digit)
            val armed = digit == activeDigit
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !done) { onDigit(digit) },
                color = when {
                    armed -> Palette.Accent
                    done -> Palette.Background
                    else -> Palette.Surface
                },
                shadowElevation = if (done) 0.dp else 2.dp,
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        digit.toString(),
                        fontSize = 26.sp,
                        fontWeight = if (armed) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            armed -> Color.White
                            done -> Palette.TextMuted.copy(alpha = 0.35f)
                            else -> Palette.Entry
                        }
                    )
                }
            }
        }
    }
}

/**
 * L'interrupteur du mode « chiffre d'abord » : on arme un chiffre au pavé, puis
 * chaque case touchée le reçoit. Le chiffre armé se change au pavé, sans repasser
 * par l'interrupteur.
 */
@Composable
private fun DigitFirstSwitch(checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Chiffre d'abord",
            fontSize = 13.sp,
            color = if (checked) Palette.Accent else Palette.TextMuted
        )
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Palette.Accent,
                checkedBorderColor = Palette.Accent,
                uncheckedThumbColor = Palette.TextMuted,
                uncheckedTrackColor = Palette.Surface,
                uncheckedBorderColor = Palette.Divider
            ),
            modifier = Modifier.scale(0.8f)
        )
    }
}

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
