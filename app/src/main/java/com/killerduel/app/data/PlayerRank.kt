package com.killerduel.app.data

import kotlinx.serialization.Serializable

/**
 * Niveau du joueur, remis en jeu chaque mois. Un mois qui s'achève ne repart
 * pas de zéro mais de la moitié du niveau atteint : ce qui a été gagné compte
 * encore, sans que le compteur se fige au plafond.
 */
@Serializable
data class PlayerRank(
    val level: Int = 1,
    /** Mois de référence, au format AAAA-MM. */
    val monthKey: String = ""
) {
    /** Applique le changement de mois, puis les étoiles gagnées. */
    fun advance(currentMonth: String, starsEarned: Int): PlayerRank {
        val base = when {
            monthKey.isEmpty() -> level
            monthKey == currentMonth -> level
            else -> (level / 2).coerceAtLeast(MIN_LEVEL)
        }
        return PlayerRank(
            level = (base + starsEarned).coerceIn(MIN_LEVEL, MAX_LEVEL),
            monthKey = currentMonth
        )
    }

    companion object {
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 100
    }
}
