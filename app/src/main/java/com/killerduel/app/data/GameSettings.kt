package com.killerduel.app.data

import kotlinx.serialization.Serializable

/** Habillage de la grille. */
enum class AppTheme(val label: String) {
    LIGHT("Clair"),
    CREAM("Crème"),
    DARK("Sombre")
}

/**
 * Préférences de jeu. Elles reprennent celles du jeu de référence, moins tout
 * ce qui relève du son, des notifications ou d'un serveur, et plus le choix
 * propre à cette application : des cages colorées plutôt que des pointillés.
 */
@Serializable
data class GameSettings(
    /** Afficher le chronomètre pendant la partie. */
    val showTimer: Boolean = true,
    /** Trois erreurs et la partie est perdue ; sinon les erreurs sont libres. */
    val mistakesLimit: Boolean = true,
    /** On arme un chiffre, puis chaque case touchée le reçoit. */
    val digitFirst: Boolean = false,
    /** Surligner la ligne, la colonne et la région de la case choisie. */
    val highlightRegions: Boolean = true,
    /** Surligner les cases et les notes portant le même chiffre. */
    val highlightSameNumbers: Boolean = true,
    /** Retirer des cases voisines la note d'un chiffre qu'on vient de poser. */
    val autoClearNotes: Boolean = true,
    /** Afficher sous chaque touche le nombre de chiffres restant à placer. */
    val showRemainingCounts: Boolean = true,
    /** Afficher le score qui court pendant la partie. */
    val showScore: Boolean = true,
    /** Colorer les cages plutôt que de les cerner de pointillés. */
    val colorfulCages: Boolean = true,
    val theme: AppTheme = AppTheme.LIGHT
)
