package com.killerduel.app.opponent

import com.killerduel.app.core.Difficulty
import com.killerduel.app.data.GameRepository
import kotlin.random.Random

/**
 * Choisit l'adversaire d'un duel. Dès qu'une partie d'entraînement est disponible
 * pour le niveau, elle sert de matière : c'est le comportement visé à terme, et
 * l'adversaire simulé ne prend le relais que faute d'historique.
 */
class OpponentPicker(private val repository: GameRepository) {

    suspend fun pick(difficulty: Difficulty, seed: Long): OpponentEngine {
        val rng = Random(seed)
        val sessions = repository.recentSessions(difficulty, limit = 10)
        if (sessions.isNotEmpty() && rng.nextDouble() < REPLAY_SHARE) {
            return ReplayOpponentEngine(sessions[rng.nextInt(sessions.size)])
        }
        return SyntheticOpponentEngine()
    }

    private companion object {
        /** Part des duels confiés à un rejeu quand l'historique le permet. */
        const val REPLAY_SHARE = 0.7
    }
}
