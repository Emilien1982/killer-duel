package com.killerduel.app.data

import com.killerduel.app.core.Difficulty
import com.killerduel.app.core.Puzzle
import kotlinx.serialization.Serializable

/** Un coup joué, daté depuis le début de la partie. */
@Serializable
data class RecordedMove(val elapsedMillis: Long, val cell: Int, val value: Int)

/**
 * Une partie d'entraînement terminée, conservée avec son déroulé complet.
 * C'est la matière première du mode Défi : le rythme d'un vrai joueur
 * remplace celui de l'adversaire simulé.
 */
@Serializable
data class RecordedSession(
    val id: String,
    val difficulty: Difficulty,
    val puzzleSeed: Long,
    val completedAtEpochMillis: Long,
    val durationMillis: Long,
    val mistakes: Int,
    val moves: List<RecordedMove>
)

/** Statistiques par niveau, alimentées par les parties terminées. */
@Serializable
data class DifficultyStats(
    val played: Int = 0,
    val completed: Int = 0,
    val bestMillis: Long = 0,
    val totalMillis: Long = 0
) {
    val averageMillis: Long get() = if (completed == 0) 0 else totalMillis / completed
}

/** Bilan des duels, tous niveaux confondus. */
@Serializable
data class DuelStats(val played: Int = 0, val won: Int = 0)

/** Mode de jeu en cours. */
enum class GameMode { TRAINING, DUEL }

/**
 * Partie interrompue, restaurée au retour dans l'application. L'historique
 * d'annulation n'en fait pas partie : le conserver alourdirait chaque
 * sauvegarde d'une copie complète de la grille par coup joué.
 */
@Serializable
data class SavedGame(
    val puzzle: Puzzle,
    val mode: GameMode,
    val entries: List<Int>,
    val notes: List<Int>,
    val mistakes: Int,
    val hintsLeft: Int,
    val elapsedMillis: Long,
    val moveLog: List<RecordedMove>
)
