package com.killerduel.app.data

import com.killerduel.app.core.Difficulty
import kotlinx.coroutines.flow.Flow

/**
 * Accès à tout ce qui survit à la fermeture de l'application : statistiques,
 * partie en cours, et historique des parties d'entraînement.
 */
interface GameRepository {

    val stats: Flow<Map<Difficulty, DifficultyStats>>

    val duelStats: Flow<DuelStats>

    /** Préférences de jeu, appliquées à toute nouvelle partie comme à l'affichage. */
    val settings: Flow<GameSettings>

    suspend fun updateSettings(transform: (GameSettings) -> GameSettings)

    /** Enregistre une partie d'entraînement terminée et met à jour les statistiques. */
    suspend fun recordTrainingSession(session: RecordedSession)

    /** Comptabilise une partie commencée (pour distinguer entamées et terminées). */
    suspend fun recordGameStarted(difficulty: Difficulty)

    /** Issue d'un duel. */
    suspend fun recordDuel(won: Boolean)

    /**
     * Parties d'entraînement les plus récentes pour un niveau, les plus récentes
     * d'abord. C'est ce que consomme l'adversaire par rejeu.
     */
    suspend fun recentSessions(difficulty: Difficulty, limit: Int = 20): List<RecordedSession>

    /** Sauvegarde (ou efface si null) la partie en cours. */
    suspend fun saveInProgress(game: SavedGame?)

    suspend fun loadInProgress(): SavedGame?
}
