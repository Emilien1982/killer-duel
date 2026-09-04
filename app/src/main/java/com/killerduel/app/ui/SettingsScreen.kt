package com.killerduel.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.killerduel.app.data.AppTheme
import com.killerduel.app.data.GameSettings
import com.killerduel.app.ui.theme.Palette

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onBack: () -> Unit,
    onChange: (GameSettings) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { AppIconView(AppIcon.Back, Palette.TextMuted, size = 22.dp) }

        Spacer(Modifier.height(10.dp))
        Text("Réglages", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Palette.TextPrimary)
        Spacer(Modifier.height(20.dp))

        GroupTitle("Partie")
        SettingsCard {
            SettingRow(
                "Chronomètre",
                "Afficher le temps pendant la partie",
                settings.showTimer
            ) { onChange(settings.copy(showTimer = it)) }
            RowDivider()
            SettingRow(
                "Limite d'erreurs",
                "Trois erreurs et la grille se referme",
                settings.mistakesLimit
            ) { onChange(settings.copy(mistakesLimit = it)) }
            RowDivider()
            SettingRow(
                "Chiffre d'abord",
                "Armer un chiffre, puis remplir les cases d'un appui",
                settings.digitFirst
            ) { onChange(settings.copy(digitFirst = it)) }
            RowDivider()
            SettingRow(
                "Chiffres restants",
                "Afficher sous chaque touche ce qu'il reste à placer",
                settings.showRemainingCounts
            ) { onChange(settings.copy(showRemainingCounts = it)) }
            RowDivider()
            SettingRow(
                "Score",
                "Afficher le score qui court pendant la partie",
                settings.showScore
            ) { onChange(settings.copy(showScore = it)) }
        }

        Spacer(Modifier.height(18.dp))
        GroupTitle("Affichage")
        SettingsCard {
            SettingRow(
                "Cages colorées",
                "Distinguer les cages par la couleur plutôt que par des pointillés",
                settings.colorfulCages
            ) { onChange(settings.copy(colorfulCages = it)) }
            RowDivider()
            SettingRow(
                "Surligner la région",
                "Ligne, colonne et région de la case choisie",
                settings.highlightRegions
            ) { onChange(settings.copy(highlightRegions = it)) }
            RowDivider()
            SettingRow(
                "Surligner les chiffres identiques",
                "Les cases et les notes portant le même chiffre",
                settings.highlightSameNumbers
            ) { onChange(settings.copy(highlightSameNumbers = it)) }
            RowDivider()
            SettingRow(
                "Effacer les notes",
                "Poser un chiffre retire cette note des cases voisines",
                settings.autoClearNotes
            ) { onChange(settings.copy(autoClearNotes = it)) }
        }

        Spacer(Modifier.height(18.dp))
        GroupTitle("Thème")
        SettingsCard {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTheme.entries.forEach { theme ->
                    ThemeChip(
                        theme = theme,
                        selected = theme == settings.theme,
                        onSelect = { onChange(settings.copy(theme = theme)) }
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun GroupTitle(title: String) {
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.TextMuted)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        color = Palette.Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Palette.Divider)
    ) {
        Column { content() }
    }
}

/** Trait intérieur : la carte porte déjà sa bordure, seules les lignes se séparent. */
@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(Palette.Divider)
    )
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Palette.TextPrimary
            )
            Text(description, fontSize = 11.sp, color = Palette.TextMuted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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

private val ThemeSwatches = mapOf(
    AppTheme.LIGHT to Color(0xFFFFFFFF),
    AppTheme.CREAM to Color(0xFFF6EFDF),
    AppTheme.DARK to Color(0xFF23262B)
)

@Composable
private fun ThemeChip(theme: AppTheme, selected: Boolean, onSelect: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // L'anneau se dessine sur un cercle plus large que la pastille pour ne pas
        // rogner la couleur, qui est la seule chose à comparer d'une pastille à l'autre.
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .clickable(onClick = onSelect)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) Palette.Accent else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ThemeSwatches.getValue(theme))
                    .border(1.dp, Palette.Divider, CircleShape)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            theme.label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Palette.Accent else Palette.TextMuted
        )
    }
}
