package com.killerduel.app.opponent

import com.killerduel.app.core.Difficulty
import com.killerduel.app.core.PuzzleGenerator
import com.killerduel.app.data.DifficultyStats
import com.killerduel.app.data.DuelStats
import com.killerduel.app.data.GameRepository
import com.killerduel.app.data.RecordedSession
import com.killerduel.app.data.SavedGame
import com.killerduel.app.game.GameSession
import com.killerduel.app.game.Outcome
import com.killerduel.app.game.withDigit
import com.killerduel.app.data.GameMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * La boucle visée par le projet : une partie jouée en entraînement doit pouvoir
 * revenir comme adversaire d'un duel, sur une grille qu'elle n'a jamais vue.
 */
class ReplayLoopTest {

    private class FakeRepository : GameRepository {
        val sessions = mutableListOf<RecordedSession>()
        override val stats: Flow<Map<Difficulty, DifficultyStats>> = MutableStateFlow(emptyMap())
        override val duelStats: Flow<DuelStats> = MutableStateFlow(DuelStats())
        override val settings: Flow<com.killerduel.app.data.GameSettings> =
            MutableStateFlow(com.killerduel.app.data.GameSettings())
        override suspend fun updateSettings(
            transform: (com.killerduel.app.data.GameSettings) -> com.killerduel.app.data.GameSettings
        ) {}
        override suspend fun recordTrainingSession(session: RecordedSession) { sessions += session }
        override suspend fun recordGameStarted(difficulty: Difficulty) {}
        override suspend fun recordDuel(won: Boolean) {}
        override suspend fun recentSessions(difficulty: Difficulty, limit: Int) =
            sessions.filter { it.difficulty == difficulty }.take(limit)
        override suspend fun saveInProgress(game: SavedGame?) {}
        override suspend fun loadInProgress(): SavedGame? = null
    }

    /** Joue la grille de bout en bout comme le ferait le joueur, coup par coup. */
    private fun playThrough(difficulty: Difficulty, seed: Long): GameSession {
        val puzzle = PuzzleGenerator.generate(difficulty, seed)
        var session = GameSession(puzzle = puzzle, mode = GameMode.TRAINING)
        var clock = 0L
        for (cell in puzzle.solveOrder) {
            clock += 4_000L + (cell % 7) * 1_500L
            session = session
                .copy(selected = cell, elapsedMillis = clock)
                .withDigit(puzzle.solution[cell])
        }
        return session
    }

    @Test
    fun `a finished training game becomes the next duel opponent`() = runTest {
        val repository = FakeRepository()
        val played = playThrough(Difficulty.EASY, seed = 2024L)
        assertEquals(Outcome.WON, played.outcome)

        repository.recordTrainingSession(
            RecordedSession(
                id = UUID.randomUUID().toString(),
                difficulty = Difficulty.EASY,
                puzzleSeed = played.puzzle.seed,
                completedAtEpochMillis = 1_700_000_000_000L,
                durationMillis = played.elapsedMillis,
                mistakes = played.mistakes,
                moves = played.moveLog
            )
        )

        // Une grille différente de celle qui a été jouée.
        val freshPuzzle = PuzzleGenerator.generate(Difficulty.EASY, seed = 999L)

        val replays = (0L until 200L).count { seed ->
            OpponentPicker(repository).pick(Difficulty.EASY, seed) is ReplayOpponentEngine
        }
        assertTrue("le rejeu doit dominer dès qu'un historique existe ($replays/200)", replays in 110..170)

        val engine = ReplayOpponentEngine(repository.recentSessions(Difficulty.EASY, 1).first())
        val plan = engine.plan(freshPuzzle, seed = 7L)

        assertEquals(PaceSource.RECORDED, plan.profile.origin)
        assertEquals(freshPuzzle.givens.count { it == 0 }, plan.moves.size)
        plan.moves.forEach { assertEquals(freshPuzzle.solution[it.cell], it.value) }

        val ratio = plan.totalMillis.toDouble() / played.elapsedMillis
        assertTrue("le temps total doit rester celui du joueur ($ratio)", ratio in 0.9..1.1)
    }

    @Test
    fun `without history the opponent is synthetic`() = runTest {
        val repository = FakeRepository()
        repeat(20) { seed ->
            assertTrue(
                OpponentPicker(repository).pick(Difficulty.HARD, seed.toLong())
                    is SyntheticOpponentEngine
            )
        }
    }
}
